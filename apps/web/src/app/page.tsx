import { api, CallSession, AdminStats, HealthStatus } from '@/lib/api-server';
import { intentLabel } from '@/lib/intents';
import Link from 'next/link';

export default async function DashboardPage() {
  let calls: CallSession[] = [];
  let stats: AdminStats | null = null;
  let health: HealthStatus = { status: 'unknown', openai: false };

  try {
    [calls, stats, health] = await Promise.all([
      api<CallSession[]>('/api/admin/calls'),
      api<AdminStats>('/api/admin/stats'),
      api<HealthStatus>('/api/health'),
    ]);
  } catch {
    /* API offline */
  }

  const active = stats?.activeCalls ?? calls.filter(c => c.status === 'ACTIVE').length;
  const transferred = stats?.transferredCalls ?? calls.filter(c => c.transferred).length;
  const emergency = stats?.emergencyCalls ?? calls.filter(c => c.emergencyFlag).length;
  const totalCalls = stats?.callCount ?? calls.length;
  const maxDaily = Math.max(1, ...(stats?.callsLast7Days?.map(d => d.count) ?? [1]));
  const topIntents = stats?.intentCounts?.slice(0, 6) ?? [];
  const maxIntent = Math.max(1, ...topIntents.map(i => i.count));

  return (
    <div>
      <h2>ダッシュボード</h2>
      <div className="grid" style={{ marginBottom: '1.5rem' }}>
        <div className="card"><div className="stat">{active}</div><div className="label">進行中通話</div></div>
        <div className="card"><div className="stat">{totalCalls}</div><div className="label">総通話数</div></div>
        <div className="card"><div className="stat">{transferred}</div><div className="label">職員転送</div></div>
        <div className="card"><div className="stat">{emergency}</div><div className="label">緊急対応</div></div>
      </div>
      {stats && (
        <div className="grid" style={{ marginBottom: '1.5rem' }}>
          <div className="card"><div className="stat">{stats.patientCount}</div><div className="label">登録患者</div></div>
          <div className="card"><div className="stat">{stats.confirmedAppointments}</div><div className="label">確定予約</div></div>
          <div className="card"><div className="stat">{stats.appointmentCount}</div><div className="label">予約総数</div></div>
        </div>
      )}

      {stats?.callsLast7Days && stats.callsLast7Days.length > 0 && (
        <div className="card chart-card">
          <h3>直近7日間の通話数</h3>
          <div className="bar-chart">
            {stats.callsLast7Days.map(d => (
              <div key={d.date} className="bar-chart-col">
                <div
                  className="bar-chart-bar"
                  style={{ height: `${(d.count / maxDaily) * 100}%` }}
                  title={`${d.count}件`}
                />
                <span className="bar-chart-label">{d.date.slice(5)}</span>
                <span className="bar-chart-value">{d.count}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {topIntents.length > 0 && (
        <div className="card chart-card">
          <h3>問い合わせ意図（上位）</h3>
          <div className="intent-chart">
            {topIntents.map(i => (
              <div key={i.intent} className="intent-row">
                <span className="intent-label">{intentLabel(i.intent)}</span>
                <div className="intent-bar-track">
                  <div
                    className="intent-bar-fill"
                    style={{ width: `${(i.count / maxIntent) * 100}%` }}
                  />
                </div>
                <span className="intent-count">{i.count}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <p>API: <strong>{health.status}</strong> / OpenAI: <strong>{health.openai ? '接続済' : '未設定'}</strong></p>
        {active > 0 && (
          <p style={{ marginTop: '0.5rem' }}>
            <Link href="/queue">ライブキューで {active} 件の進行中通話を監視 →</Link>
          </p>
        )}
      </div>

      <h3 style={{ marginTop: '1.5rem', marginBottom: '0.5rem' }}>直近の通話</h3>
      <div className="card">
        <table>
          <thead><tr><th>開始</th><th>電話番号</th><th>意図</th><th>状態</th><th>要約</th><th></th></tr></thead>
          <tbody>
            {calls.slice(0, 10).map(c => (
              <tr key={c.id}>
                <td>{new Date(c.startedAt).toLocaleString('ja-JP')}</td>
                <td>{c.callerPhone || '-'}</td>
                <td>{intentLabel(c.intent)}</td>
                <td>
                  {c.emergencyFlag && <span className="badge emergency">緊急</span>}{' '}
                  {c.transferred && <span className="badge transfer">転送</span>}{' '}
                  {c.status === 'ACTIVE' && <span className="badge active">進行中</span>}{' '}
                  {c.status === 'ENDED' && !c.transferred && !c.emergencyFlag && (
                    <span className="badge ended">完了</span>
                  )}
                </td>
                <td>{c.summary ? c.summary.slice(0, 50) + '…' : '-'}</td>
                <td><Link href={`/calls/${c.id}`}>詳細</Link></td>
              </tr>
            ))}
            {calls.length === 0 && <tr><td colSpan={6}>通話データがありません</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
