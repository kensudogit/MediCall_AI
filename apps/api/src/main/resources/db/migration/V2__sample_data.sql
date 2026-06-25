-- Sample data for demo / development

-- Patients
INSERT INTO patients (full_name, name_kana, date_of_birth, phone_number, verified) VALUES
    ('山田 太郎', 'ヤマダタロウ', '1985-04-12', '09012345678', true),
    ('佐藤 花子', 'サトウハナコ', '1990-08-23', '08098765432', true),
    ('鈴木 一郎', 'スズキイチロウ', '1978-01-05', '07011112222', true),
    ('田中美咲', 'タナカミサキ', '1995-11-30', '09055556666', false);

-- Appointments
INSERT INTO appointments (patient_id, scheduled_at, department, status, notes)
SELECT p.id, NOW() + INTERVAL '2 days 10 hours', '内科', 'CONFIRMED', '初診・頭痛の相談'
FROM patients p WHERE p.phone_number = '09012345678';

INSERT INTO appointments (patient_id, scheduled_at, department, status, notes)
SELECT p.id, NOW() + INTERVAL '5 days 14 hours 30 minutes', '循環器内科', 'CONFIRMED', '血圧検査・定期受診'
FROM patients p WHERE p.phone_number = '08098765432';

INSERT INTO appointments (patient_id, scheduled_at, department, status, notes)
SELECT p.id, NOW() + INTERVAL '1 day 9 hours', '内科', 'CONFIRMED', '風邪症状'
FROM patients p WHERE p.phone_number = '07011112222';

INSERT INTO appointments (patient_id, scheduled_at, department, status, notes)
SELECT p.id, NOW() - INTERVAL '3 days 11 hours', '内科', 'CANCELLED', '患者都合でキャンセル'
FROM patients p WHERE p.phone_number = '09055556666';

-- Call sessions (fixed UUIDs for turn references)
INSERT INTO call_sessions (
    id, connect_contact_id, caller_phone, patient_id, status, intent,
    verified, emergency_flag, transferred, transfer_reason, summary,
    started_at, ended_at
) VALUES
(
    '11111111-1111-1111-1111-111111111101',
    'connect-sample-001',
    '09012345678',
    (SELECT id FROM patients WHERE phone_number = '09012345678'),
    'ENDED', 'HOURS', true, false, false, NULL,
    '山田様より診療時間の問い合わせ。自動応答で平日・土曜の診療時間を案内し、了承のうえ通話終了。',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour 55 minutes'
),
(
    '11111111-1111-1111-1111-111111111102',
    'connect-sample-002',
    '08098765432',
    (SELECT id FROM patients WHERE phone_number = '08098765432'),
    'ENDED', 'APPOINTMENT_NEW', true, false, false, NULL,
    '佐藤様より新規予約の依頼。本人確認後、6/28 14:30 循環器内科の予約を承りました。',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day' + INTERVAL '8 minutes'
),
(
    '11111111-1111-1111-1111-111111111103',
    'connect-sample-003',
    '09099998888',
    NULL,
    'ENDED', 'EMERGENCY', false, true, true, 'EMERGENCY',
    '非登録番号より緊急連絡。「胸が痛い」との訴えのため AI 応答せず直ちに職員へ転送。',
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '2 hours 50 minutes'
),
(
    '11111111-1111-1111-1111-111111111104',
    'connect-sample-004',
    '07011112222',
    (SELECT id FROM patients WHERE phone_number = '07011112222'),
    'ACTIVE', 'LAB', true, false, false, NULL,
    NULL,
    NOW() - INTERVAL '5 minutes',
    NULL
),
(
    '11111111-1111-1111-1111-111111111105',
    'connect-sample-005',
    '08011112233',
    NULL,
    'ENDED', 'COMPLAINT', false, false, true, 'COMPLAINT',
    '待ち時間に関する苦情。AI 判断不能のため会計担当へ転送。折り返し連絡を約束して終了。',
    NOW() - INTERVAL '6 hours',
    NOW() - INTERVAL '5 hours 52 minutes'
);

-- Call turns: session 101 (診療時間)
INSERT INTO call_turns (session_id, role, content, intent, action, created_at) VALUES
('11111111-1111-1111-1111-111111111101', 'assistant',
 'メディコール内科クリニックです。自動応答がご案内いたします。ご用件をお話しください。',
 NULL, 'RESPOND', NOW() - INTERVAL '2 hours'),
('11111111-1111-1111-1111-111111111101', 'caller',
 '診療時間を教えてください', NULL, NULL, NOW() - INTERVAL '1 hour 59 minutes'),
('11111111-1111-1111-1111-111111111101', 'assistant',
 '平日 9:00〜12:00 / 14:00〜18:00、土曜 9:00〜12:00', 'HOURS', 'RESPOND',
 NOW() - INTERVAL '1 hour 58 minutes'),
('11111111-1111-1111-1111-111111111101', 'caller',
 'ありがとうございました', NULL, NULL, NOW() - INTERVAL '1 hour 56 minutes');

-- Call turns: session 102 (予約)
INSERT INTO call_turns (session_id, role, content, intent, action, created_at) VALUES
('11111111-1111-1111-1111-111111111102', 'assistant',
 'メディコール内科クリニックです。自動応答がご案内いたします。ご用件をお話しください。',
 NULL, 'RESPOND', NOW() - INTERVAL '1 day'),
('11111111-1111-1111-1111-111111111102', 'caller',
 '予約を取りたいのですが', NULL, NULL, NOW() - INTERVAL '1 day' + INTERVAL '30 seconds'),
('11111111-1111-1111-1111-111111111102', 'assistant',
 '予約のお手続きには本人確認が必要です。氏名・生年月日・お電話番号をお伝えください。',
 'APPOINTMENT_NEW', 'ASK_IDENTITY', NOW() - INTERVAL '1 day' + INTERVAL '1 minute'),
('11111111-1111-1111-1111-111111111102', 'caller',
 '佐藤花子、1990-08-23、08098765432です', NULL, NULL, NOW() - INTERVAL '1 day' + INTERVAL '2 minutes'),
('11111111-1111-1111-1111-111111111102', 'assistant',
 '予約を承りました。循環器内科 14:30 のご予約です。', 'APPOINTMENT_NEW', 'RESPOND',
 NOW() - INTERVAL '1 day' + INTERVAL '3 minutes');

-- Call turns: session 103 (緊急)
INSERT INTO call_turns (session_id, role, content, intent, action, created_at) VALUES
('11111111-1111-1111-1111-111111111103', 'assistant',
 'メディコール内科クリニックです。自動応答がご案内いたします。ご用件をお話しください。',
 NULL, 'RESPOND', NOW() - INTERVAL '3 hours'),
('11111111-1111-1111-1111-111111111103', 'caller',
 '胸が痛くて息も苦しいです', NULL, NULL, NOW() - INTERVAL '3 hours' + INTERVAL '20 seconds'),
('11111111-1111-1111-1111-111111111103', 'assistant',
 '緊急症状の可能性があるため、直ちに職員へおつなぎします。',
 'EMERGENCY', 'TRANSFER_HUMAN', NOW() - INTERVAL '3 hours' + INTERVAL '25 seconds');

-- Call turns: session 104 (進行中・検査)
INSERT INTO call_turns (session_id, role, content, intent, action, created_at) VALUES
('11111111-1111-1111-1111-111111111104', 'assistant',
 'メディコール内科クリニックです。自動応答がご案内いたします。ご用件をお話しください。',
 NULL, 'RESPOND', NOW() - INTERVAL '5 minutes'),
('11111111-1111-1111-1111-111111111104', 'caller',
 '血液検査の結果はいつわかりますか', NULL, NULL, NOW() - INTERVAL '4 minutes'),
('11111111-1111-1111-1111-111111111104', 'assistant',
 '検査に関するご案内です。採血は予約制となっております。詳細は職員がご案内いたします。',
 'LAB', 'RESPOND', NOW() - INTERVAL '3 minutes');

-- Call turns: session 105 (苦情転送)
INSERT INTO call_turns (session_id, role, content, intent, action, created_at) VALUES
('11111111-1111-1111-1111-111111111105', 'assistant',
 'メディコール内科クリニックです。自動応答がご案内いたします。ご用件をお話しください。',
 NULL, 'RESPOND', NOW() - INTERVAL '6 hours'),
('11111111-1111-1111-1111-111111111105', 'caller',
 '待ち時間が長すぎて怒っています', NULL, NULL, NOW() - INTERVAL '6 hours' + INTERVAL '40 seconds'),
('11111111-1111-1111-1111-111111111105', 'assistant',
 '担当職員におつなぎします。', 'COMPLAINT', 'TRANSFER_HUMAN',
 NOW() - INTERVAL '6 hours' + INTERVAL '45 seconds');

-- Additional FAQ samples
INSERT INTO faq_items (category, question, answer, sort_order) VALUES
('検査', '血液検査の結果はいつ届きますか', '通常3〜5営業日でご連絡します。急ぎの場合は受付までお申し出ください。', 6),
('会計', '保険証を忘れた場合', '一旦自費払いとなりますが、後日保険証をお持ちいただければ差額を返金いたします。', 7),
('薬', '処方薬の再発行', '医師の確認が必要です。お電話いただくか、窓口でお申し出ください。', 8);
