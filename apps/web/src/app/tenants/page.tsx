'use client';

import { useEffect, useState } from 'react';
import { api, CreateTenantRequest, Tenant } from '@/lib/api';
import { useTenant } from '@/components/TenantProvider';

export default function TenantsPage() {
  const { tenants, refresh, setSlug } = useTenant();
  const [form, setForm] = useState<CreateTenantRequest>({
    slug: '',
    name: '',
    plan: 'TRIAL',
  });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const create = async () => {
    setLoading(true);
    setMessage('');
    try {
      const created = await api<Tenant>('/api/admin/tenants', {
        method: 'POST',
        body: JSON.stringify(form),
      });
      setMessage(`テナント「${created.name}」を作成しました`);
      setForm({ slug: '', name: '', plan: 'TRIAL' });
      await refresh();
    } catch (e) {
      setMessage(e instanceof Error ? e.message : '作成に失敗しました');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>テナント管理（SaaS）</h2>
      <p className="page-meta" style={{ marginBottom: '1rem' }}>
        複数クリニックを1つのプラットフォームで運用します。テナントごとにデータは完全に分離されます。
      </p>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h3>登録テナント ({tenants.length})</h3>
        <table>
          <thead>
            <tr><th>スラッグ</th><th>名称</th><th>プラン</th><th>状態</th><th></th></tr>
          </thead>
          <tbody>
            {tenants.map(t => (
              <tr key={t.id}>
                <td><code>{t.slug}</code></td>
                <td>{t.name}</td>
                <td>{t.plan}</td>
                <td>{t.status}</td>
                <td>
                  <button type="button" className="secondary" onClick={() => setSlug(t.slug)}>
                    切替
                  </button>
                </td>
              </tr>
            ))}
            {tenants.length === 0 && (
              <tr><td colSpan={5}>テナントがありません</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="card">
        <h3>新規テナント作成</h3>
        <div className="form-group">
          <label>スラッグ（URL識別子）</label>
          <input
            value={form.slug}
            onChange={e => setForm({ ...form, slug: e.target.value })}
            placeholder="my-clinic"
          />
          <p className="form-hint">英小文字・数字・ハイフン（例: shibuya-clinic）</p>
        </div>
        <div className="form-group">
          <label>クリニック名</label>
          <input
            value={form.name}
            onChange={e => setForm({ ...form, name: e.target.value })}
            placeholder="〇〇内科クリニック"
          />
        </div>
        <div className="form-group">
          <label>プラン</label>
          <select value={form.plan} onChange={e => setForm({ ...form, plan: e.target.value })}>
            <option value="TRIAL">TRIAL</option>
            <option value="STANDARD">STANDARD</option>
            <option value="ENTERPRISE">ENTERPRISE</option>
          </select>
        </div>
        <button type="button" onClick={create} disabled={loading || !form.slug || !form.name}>
          {loading ? '作成中…' : 'テナントを作成'}
        </button>
        {message && <p className="page-meta" style={{ marginTop: '0.75rem' }}>{message}</p>}
      </div>

      <div className="card" style={{ marginTop: '1rem' }}>
        <h3>SaaS アーキテクチャ</h3>
        <ul className="saas-notes">
          <li>全APIリクエストに <code>X-Tenant-Slug</code> ヘッダーでテナントを識別</li>
          <li>患者・通話・FAQ・RAG知識はテナント単位で分離</li>
          <li>通話デモは選択中のテナントのクリニック設定で応答</li>
          <li>本番ではサブドメイン（<code>{'{slug}'}.medicall.ai</code>）やJWTクレーム連携を推奨</li>
        </ul>
      </div>
    </div>
  );
}
