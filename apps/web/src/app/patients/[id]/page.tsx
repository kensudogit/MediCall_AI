import { api, PatientDetail } from '@/lib/api-server';
import { intentLabel } from '@/lib/intents';
import Link from 'next/link';

export default async function PatientDetailPage({ params }: { params: { id: string } }) {
  let detail: PatientDetail | null = null;
  try {
    detail = await api<PatientDetail>(`/api/admin/patients/${params.id}`);
  } catch {
    /* not found */
  }

  if (!detail) {
    return (
      <div>
        <h2>患者が見つかりません</h2>
        <Link href="/patients">← 患者検索に戻る</Link>
      </div>
    );
  }

  const { patient, recentCalls, appointments } = detail;

  return (
    <div>
      <p><Link href="/patients">← 患者検索</Link></p>
      <h2>{patient.fullName}</h2>
      <div className="grid" style={{ marginBottom: '1.5rem' }}>
        <div className="card">
          <div className="label">フリガナ</div>
          <div>{patient.nameKana || '-'}</div>
        </div>
        <div className="card">
          <div className="label">生年月日</div>
          <div>{patient.dateOfBirth}</div>
        </div>
        <div className="card">
          <div className="label">電話番号</div>
          <div>{patient.phoneNumber}</div>
        </div>
        <div className="card">
          <div className="label">本人確認</div>
          <div>{patient.verified ? '済' : '未'}</div>
        </div>
      </div>

      <h3 style={{ marginBottom: '0.5rem' }}>通話履歴</h3>
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <table>
          <thead>
            <tr><th>日時</th><th>意図</th><th>状態</th><th>要約</th><th></th></tr>
          </thead>
          <tbody>
            {recentCalls.map(c => (
              <tr key={c.id}>
                <td>{new Date(c.startedAt).toLocaleString('ja-JP')}</td>
                <td>{intentLabel(c.intent)}</td>
                <td>
                  {c.emergencyFlag && <span className="badge emergency">緊急</span>}{' '}
                  {c.transferred && <span className="badge transfer">転送</span>}{' '}
                  {c.status === 'ACTIVE' && <span className="badge active">進行中</span>}
                  {c.status === 'ENDED' && !c.emergencyFlag && !c.transferred && (
                    <span className="badge ended">完了</span>
                  )}
                </td>
                <td>{c.summary ? c.summary.slice(0, 40) + '…' : '-'}</td>
                <td><Link href={`/calls/${c.id}`}>詳細</Link></td>
              </tr>
            ))}
            {recentCalls.length === 0 && (
              <tr><td colSpan={5}>通話履歴がありません</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <h3 style={{ marginBottom: '0.5rem' }}>予約</h3>
      <div className="card">
        <table>
          <thead>
            <tr><th>日時</th><th>診療科</th><th>状態</th><th>備考</th></tr>
          </thead>
          <tbody>
            {appointments.map(a => (
              <tr key={a.id}>
                <td>{new Date(a.scheduledAt).toLocaleString('ja-JP')}</td>
                <td>{a.department}</td>
                <td>
                  <span className={`badge ${a.status === 'CONFIRMED' ? 'confirmed' : 'cancelled'}`}>
                    {a.status === 'CONFIRMED' ? '確定' : a.status}
                  </span>
                </td>
                <td>{a.notes || '-'}</td>
              </tr>
            ))}
            {appointments.length === 0 && (
              <tr><td colSpan={4}>予約がありません</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
