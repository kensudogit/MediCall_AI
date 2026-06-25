package com.medicall.service;

import com.medicall.domain.*;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.CallTurnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class CallOrchestrationService {

    private final CallSessionRepository sessionRepository;
    private final CallTurnRepository turnRepository;
    private final EmergencyDetectionService emergencyDetection;
    private final IntentClassificationService intentClassification;
    private final ProhibitedResponseGuard prohibitedGuard;
    private final IdentityVerificationService identityService;
    private final AppointmentService appointmentService;
    private final FaqService faqService;
    private final RagService ragService;
    private final OpenAiService openAiService;
    private final PollyService pollyService;

    public CallOrchestrationService(CallSessionRepository sessionRepository,
                                    CallTurnRepository turnRepository,
                                    EmergencyDetectionService emergencyDetection,
                                    IntentClassificationService intentClassification,
                                    ProhibitedResponseGuard prohibitedGuard,
                                    IdentityVerificationService identityService,
                                    AppointmentService appointmentService,
                                    FaqService faqService,
                                    RagService ragService,
                                    OpenAiService openAiService,
                                    PollyService pollyService) {
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.emergencyDetection = emergencyDetection;
        this.intentClassification = intentClassification;
        this.prohibitedGuard = prohibitedGuard;
        this.identityService = identityService;
        this.appointmentService = appointmentService;
        this.faqService = faqService;
        this.ragService = ragService;
        this.openAiService = openAiService;
        this.pollyService = pollyService;
    }

    public record CallRequest(UUID sessionId, String utterance, String callerPhone,
                              String fullName, String dateOfBirth) {}

    public record CallResponse(String text, CallAction action, CallIntent intent,
                               boolean transfer, String transferReason, String audioUrl) {}

    @Transactional
    public CallSession startSession(String connectContactId, String callerPhone) {
        CallSession session = new CallSession();
        session.setConnectContactId(connectContactId);
        session.setCallerPhone(callerPhone);
        session.setStatus("ACTIVE");
        session = sessionRepository.save(session);

        String greeting = faqService.getClinicSettings().getClinicName()
                + "です。自動応答がご案内いたします。ご用件をお話しください。";
        saveTurn(session.getId(), "assistant", greeting, null, CallAction.RESPOND.name());
        return session;
    }

    @Transactional
    public CallResponse processUtterance(CallRequest req) {
        CallSession session = sessionRepository.findById(req.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        saveTurn(session.getId(), "caller", req.utterance(), null, null);

        if (emergencyDetection.isEmergency(req.utterance())) {
            return transfer(session, CallIntent.EMERGENCY, "緊急症状の可能性があるため、直ちに職員へおつなぎします。");
        }

        CallIntent intent = intentClassification.classify(req.utterance());

        if (intent == CallIntent.COMPLAINT || intent == CallIntent.MEDICAL_QUESTION) {
            return transfer(session, intent, "担当職員におつなぎします。");
        }

        if (requiresIdentity(intent) && !session.isVerified()) {
            if (hasIdentityFields(req)) {
                var result = identityService.verify(new IdentityVerificationService.IdentityInput(
                        req.fullName(), req.dateOfBirth(), req.callerPhone()));
                if (!result.success()) {
                    if (result.message().contains("職員")) {
                        return transfer(session, CallIntent.HUMAN_TRANSFER, result.message());
                    }
                    String msg = result.message();
                    return respond(session, intent, msg, CallAction.ASK_IDENTITY);
                }
                session.setVerified(true);
                session.setPatientId(result.patientId());
                sessionRepository.save(session);
            } else {
                return respond(session, intent,
                        "予約のお手続きには本人確認が必要です。氏名・生年月日・お電話番号をお伝えください。",
                        CallAction.ASK_IDENTITY);
            }
        }

        String responseText = buildResponse(session, intent, req.utterance());
        if (prohibitedGuard.isProhibited(responseText)) {
            return transfer(session, CallIntent.HUMAN_TRANSFER, prohibitedGuard.safeFallback());
        }

        if (intent == CallIntent.UNKNOWN) {
            return transfer(session, CallIntent.HUMAN_TRANSFER, "ご用件を確認できませんでした。職員におつなぎします。");
        }

        session.setIntent(intent.name());
        sessionRepository.save(session);
        return respond(session, intent, responseText, CallAction.RESPOND);
    }

    private String buildResponse(CallSession session, CallIntent intent, String utterance) {
        ClinicSettings clinic = faqService.getClinicSettings();

        return switch (intent) {
            case HOURS -> clinic.getHoursText();
            case HOLIDAY -> clinic.getHolidaysText();
            case ACCESS -> clinic.getAccessText();
            case BELONGINGS -> clinic.getBelongingsText();
            case APPOINTMENT_NEW -> {
                if (session.getPatientId() != null) {
                    var appt = appointmentService.create(session.getPatientId(), null, "内科", utterance);
                    yield appointmentService.formatConfirmation(appt);
                }
                yield "予約を承ります。ご希望の日時をお伝えください。";
            }
            case APPOINTMENT_CHANGE -> "予約変更を承ります。変更前の予約日と新しいご希望日をお伝えください。";
            case APPOINTMENT_CANCEL -> "予約キャンセルを承ります。キャンセルする予約日をお伝えください。";
            case LAB -> "検査に関するご案内です。採血は予約制となっております。詳細は職員がご案内いたします。";
            case BILLING -> "会計・料金に関するお問い合わせです。窓口または会計担当へおつなぎすることもできます。";
            case PHARMACY -> "お薬に関するご質問です。処方内容の確認は医師・薬剤師が対応いたします。";
            case REFERRAL -> "紹介状の発行についてご案内します。担当医の診察後、窓口でお申し出ください。";
            case FAQ -> {
                String faq = faqService.findBestAnswer(utterance);
                if (faq != null) yield faq;
                String rag = ragService.searchContext(utterance, 3);
                if (!rag.isBlank()) {
                    yield openAiService.chat(
                            "FAQに基づき簡潔に回答。診断・処方・重症度判断は禁止。",
                            "質問: " + utterance + "\n参考:\n" + rag);
                }
                yield "該当するFAQが見つかりませんでした。";
            }
            default -> openAiService.chat(
                    "医療コールセンターAI。診断・処方・重症度判断は禁止。案内のみ。",
                    utterance);
        };
    }

    private boolean requiresIdentity(CallIntent intent) {
        return intent == CallIntent.APPOINTMENT_NEW
                || intent == CallIntent.APPOINTMENT_CHANGE
                || intent == CallIntent.APPOINTMENT_CANCEL;
    }

    private boolean hasIdentityFields(CallRequest req) {
        return req.fullName() != null && !req.fullName().isBlank()
                && req.dateOfBirth() != null && !req.dateOfBirth().isBlank();
    }

    private CallResponse transfer(CallSession session, CallIntent intent, String message) {
        session.setTransferred(true);
        session.setTransferReason(intent.name());
        session.setIntent(intent.name());
        if (intent == CallIntent.EMERGENCY) session.setEmergencyFlag(true);
        sessionRepository.save(session);
        saveTurn(session.getId(), "assistant", message, intent.name(), CallAction.TRANSFER_HUMAN.name());
        return new CallResponse(message, CallAction.TRANSFER_HUMAN, intent, true, intent.name(),
                pollyService.synthesize(message));
    }

    private CallResponse respond(CallSession session, CallIntent intent, String message, CallAction action) {
        saveTurn(session.getId(), "assistant", message, intent.name(), action.name());
        return new CallResponse(message, action, intent, false, null, pollyService.synthesize(message));
    }

    private void saveTurn(UUID sessionId, String role, String content, String intent, String action) {
        CallTurn turn = new CallTurn();
        turn.setSessionId(sessionId);
        turn.setRole(role);
        turn.setContent(content);
        turn.setIntent(intent);
        turn.setAction(action);
        turnRepository.save(turn);
    }
}
