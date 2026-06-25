'use client';

import { useEffect, useState } from 'react';
import { api, ClinicSettings } from '@/lib/api';

export default function ClinicPage() {
  const [settings, setSettings] = useState<ClinicSettings | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    api<ClinicSettings>('/api/admin/clinic').then(setSettings).catch(() => {});
  }, []);

  const save = async () => {
    if (!settings) return;
    await api('/api/admin/clinic', { method: 'PUT', body: JSON.stringify(settings) });
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  if (!settings) return <div>読み込み中...</div>;

  const fields: { key: keyof ClinicSettings; label: string; rows?: number }[] = [
    { key: 'clinicName', label: '医院名' },
    { key: 'hoursText', label: '診療時間', rows: 3 },
    { key: 'holidaysText', label: '休診日', rows: 2 },
    { key: 'accessText', label: 'アクセス', rows: 3 },
    { key: 'belongingsText', label: '持ち物案内', rows: 3 },
  ];

  return (
    <div>
      <h2>医院設定</h2>
      <div className="card">
        {fields.map(f => (
          <div className="form-group" key={f.key}>
            <label>{f.label}</label>
            {f.rows ? (
              <textarea
                rows={f.rows}
                value={settings[f.key] as string}
                onChange={e => setSettings({ ...settings, [f.key]: e.target.value })}
              />
            ) : (
              <input
                value={settings[f.key] as string}
                onChange={e => setSettings({ ...settings, [f.key]: e.target.value })}
              />
            )}
          </div>
        ))}
        <button onClick={save}>保存</button>
        {saved && <span style={{ marginLeft: 12, color: '#27ae60' }}>保存しました</span>}
      </div>
    </div>
  );
}
