package com.medicall.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
public class CallSession {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "connect_contact_id")
    private String connectContactId;

    @Column(name = "caller_phone")
    private String callerPhone;

    @Column(name = "patient_id")
    private Long patientId;

    private String status = "ACTIVE";
    private String intent;
    private boolean verified;
    @Column(name = "emergency_flag")
    private boolean emergencyFlag;
    private boolean transferred;
    @Column(name = "transfer_reason")
    private String transferReason;
    private String summary;

    @Column(name = "started_at")
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getConnectContactId() { return connectContactId; }
    public void setConnectContactId(String connectContactId) { this.connectContactId = connectContactId; }
    public String getCallerPhone() { return callerPhone; }
    public void setCallerPhone(String callerPhone) { this.callerPhone = callerPhone; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public boolean isEmergencyFlag() { return emergencyFlag; }
    public void setEmergencyFlag(boolean emergencyFlag) { this.emergencyFlag = emergencyFlag; }
    public boolean isTransferred() { return transferred; }
    public void setTransferred(boolean transferred) { this.transferred = transferred; }
    public String getTransferReason() { return transferReason; }
    public void setTransferReason(String transferReason) { this.transferReason = transferReason; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}
