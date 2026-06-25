import { api, CallSession } from '@/lib/api';

export default async function DashboardPage() {
  let calls: CallSession[] = [];
  let health = { status: 'unknown', openai: false };
  try {
    calls = await api<CallSession[]>('/api/admin/calls');
    health = await api('/api/health');
  } catch {
    /* API offline */
  }

  const active = calls.filter(c => c.status === 'ACTIVE').length;
  const transferred = calls.filter(c => c.transferred).length;
  const emergency = calls.filter(c => c.emergencyFlag).length;

  return (
    <div>
      <h2>ダッシュボード</h2>
      <div className="grid" style={{ marginBottom: '1.5rem' }}>
        <div className="card"><div className="stat">{active}</div><div className="label">進行中通話</div></div>
        <div className="card"><div className="stat">{calls.length}</div><div className="label">総通話数</div></div>
        <div className="card"><div className="stat">{transferred}</div><div className="label">職員転送</div></div>
        <div className="card"><div className="stat">{emergency}</div><div className="label">緊急対応</div></div>
      </div>
      <div className="card">
        <p>API: <strong>{health.status}</strong> / OpenAI: <strong>{health.openai ? '接続済' : '未設定'}</strong></p>
      </div>
      <h3 style={{ marginTop: '1.5rem', marginBottom: '0.5rem' }}>直近の通話</h3>
      <div className="card">
        <table>
          <thead><tr><th>開始</th><th>電話番号</th><th>意図</th><th>状態</th><th>要約</th></tr></thead>
          <tbody>
            {calls.slice(0, 10).map(c => (
              <tr key={c.id}>
                <td>{new Date(c.startedAt).toLocaleString('ja-JP')}</td>
                <td>{c.callerPhone || '-'}</td>
                <td>{c.intent || '-'}</td>
                <td>
                  {c.emergencyFlag && <span className="badge emergency">緊急</span>}
                  {c.transferred && <span className="badge transfer">転送</span>}
                  {c.status === 'ACTIVE' && <span className="badge active">進行中</span>}
                </td>
                <td>{c.summary ? c.summary.slice(0, 60) + '...' : '-'}</td>
              </tr>
            ))}
            {calls.length === 0 && <tr><td colSpan={5}>通話データがありません</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
