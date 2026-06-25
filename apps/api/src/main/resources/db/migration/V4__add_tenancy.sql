-- SaaS multi-tenancy: tenants table + tenant_id on all tenant-scoped data

CREATE TABLE tenants (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(63) NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    plan        VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO tenants (slug, name, plan) VALUES
    ('demo', 'メディコール内科クリニック', 'STANDARD'),
    ('clinic-b', 'サンプル歯科医院', 'TRIAL');

-- clinic_settings
ALTER TABLE clinic_settings ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE clinic_settings SET tenant_id = (SELECT id FROM tenants WHERE slug = 'demo');
ALTER TABLE clinic_settings ALTER COLUMN tenant_id SET NOT NULL;
CREATE UNIQUE INDEX idx_clinic_settings_tenant ON clinic_settings(tenant_id);

-- faq_items
ALTER TABLE faq_items ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE faq_items SET tenant_id = (SELECT id FROM tenants WHERE slug = 'demo');
ALTER TABLE faq_items ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_faq_items_tenant ON faq_items(tenant_id, sort_order);

-- knowledge_chunks
ALTER TABLE knowledge_chunks ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE knowledge_chunks SET tenant_id = (SELECT id FROM tenants WHERE slug = 'demo');
ALTER TABLE knowledge_chunks ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_knowledge_tenant ON knowledge_chunks(tenant_id);

-- patients: per-tenant phone uniqueness
ALTER TABLE patients ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE patients SET tenant_id = (SELECT id FROM tenants WHERE slug = 'demo');
ALTER TABLE patients ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE patients DROP CONSTRAINT IF EXISTS patients_phone_number_key;
CREATE UNIQUE INDEX idx_patients_tenant_phone ON patients(tenant_id, phone_number);
CREATE INDEX idx_patients_tenant ON patients(tenant_id);

-- appointments
ALTER TABLE appointments ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE appointments SET tenant_id = (SELECT id FROM tenants WHERE slug = 'demo');
ALTER TABLE appointments ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_appointments_tenant ON appointments(tenant_id, scheduled_at);

-- call_sessions
ALTER TABLE call_sessions ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
UPDATE call_sessions SET tenant_id = (SELECT id FROM tenants WHERE slug = 'demo');
ALTER TABLE call_sessions ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_call_sessions_tenant ON call_sessions(tenant_id, started_at DESC);

-- Seed second tenant (clinic-b) with default settings
INSERT INTO clinic_settings (tenant_id, clinic_name, hours_text, holidays_text, access_text, belongings_text)
SELECT t.id,
    'サンプル歯科医院',
    '平日 10:00〜13:00 / 15:00〜19:00',
    '水曜・日曜・祝日は休診',
    '地下鉄△△駅 3番出口より徒歩2分',
    '保険証、お薬手帳をご持参ください'
FROM tenants t WHERE t.slug = 'clinic-b';

INSERT INTO faq_items (tenant_id, category, question, answer, sort_order) 
SELECT t.id, v.category, v.question, v.answer, v.sort_order
FROM tenants t,
(VALUES
    ('診療時間', '診療時間を教えてください', '平日は10時から13時、15時から19時です。', 1),
    ('休診日', '休診日はいつですか', '水曜・日曜・祝日は休診です。', 2),
    ('予約', '予約方法', 'お電話またはWebからご予約いただけます。', 3)
) AS v(category, question, answer, sort_order)
WHERE t.slug = 'clinic-b';
