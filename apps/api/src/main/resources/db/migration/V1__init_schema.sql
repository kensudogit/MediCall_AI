-- MediCall AI schema

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE clinic_settings (
    id              BIGSERIAL PRIMARY KEY,
    clinic_name     VARCHAR(200) NOT NULL,
    hours_text      TEXT NOT NULL,
    holidays_text   TEXT NOT NULL,
    access_text     TEXT NOT NULL,
    belongings_text TEXT NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE faq_items (
    id          BIGSERIAL PRIMARY KEY,
    category    VARCHAR(50) NOT NULL,
    question    TEXT NOT NULL,
    answer      TEXT NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE knowledge_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type VARCHAR(30) NOT NULL,
    source_id   VARCHAR(100),
    content     TEXT NOT NULL,
    embedding   vector(1536),
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_knowledge_embedding ON knowledge_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE patients (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    name_kana       VARCHAR(100),
    date_of_birth   DATE NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (phone_number)
);

CREATE TABLE appointments (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT REFERENCES patients(id),
    scheduled_at    TIMESTAMPTZ NOT NULL,
    department      VARCHAR(50) NOT NULL DEFAULT '内科',
    status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE call_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connect_contact_id  VARCHAR(100),
    caller_phone        VARCHAR(20),
    patient_id          BIGINT REFERENCES patients(id),
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    intent              VARCHAR(30),
    verified            BOOLEAN NOT NULL DEFAULT FALSE,
    emergency_flag      BOOLEAN NOT NULL DEFAULT FALSE,
    transferred         BOOLEAN NOT NULL DEFAULT FALSE,
    transfer_reason     VARCHAR(100),
    summary             TEXT,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at            TIMESTAMPTZ
);

CREATE TABLE call_turns (
    id              BIGSERIAL PRIMARY KEY,
    session_id      UUID NOT NULL REFERENCES call_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    intent          VARCHAR(30),
    action          VARCHAR(30),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_call_sessions_started ON call_sessions(started_at DESC);
CREATE INDEX idx_call_turns_session ON call_turns(session_id, created_at);
CREATE INDEX idx_appointments_scheduled ON appointments(scheduled_at);

-- Seed clinic settings
INSERT INTO clinic_settings (clinic_name, hours_text, holidays_text, access_text, belongings_text)
VALUES (
    'メディコール内科クリニック',
    '平日 9:00〜12:00 / 14:00〜18:00、土曜 9:00〜12:00',
    '日曜・祝日は休診です',
    'JR〇〇駅東口より徒歩5分。駐車場は建物北側に10台あります',
    '保険証、お薬手帳、各種受診券をご持参ください'
);

INSERT INTO faq_items (category, question, answer, sort_order) VALUES
('診療時間', '診療時間を教えてください', '平日は9時から12時、14時から18時です。土曜は9時から12時まで診療しています。', 1),
('休診日', '休診日はいつですか', '日曜日と祝日は休診です。', 2),
('アクセス', 'クリニックへの行き方', 'JR〇〇駅東口を出て直進、5分ほど歩くと右手に当院があります。', 3),
('持ち物', '初診の持ち物', '保険証、お薬手帳、紹介状（お持ちの場合）をご持参ください。', 4),
('予約', '予約の変更方法', 'お電話または管理画面から変更できます。前日までにお願いします。', 5);
