import { api, CallSession } from '@/lib/api';
import Link from 'next/link';

const intentLabels: Record<string, string> = {
  HOURS: '診療時間',
  HOLIDAY: '休診日',
  ACCESS: 'アクセス',
  BELONGINGS: '持ち物',
  APPOINTMENT_NEW: '新規予約',
  APPOINTMENT_CHANGE: '予約変更',
  APPOINTMENT_CANCEL: 'キャンセル',
  LAB: '検査',
  BILLING: '会計',
  PHARMACY: '薬',
  REFERRAL: '紹介状',
  EMERGENCY: '緊急',
  COMPLAINT: '苦情',
  HUMAN_TRANSFER: '職員転送',
};

export default async function CallsPage() {
  let calls: CallSession[] = [];
  try {
    calls = await api<CallSession[]>('/api/admin/calls');
  } catch { /* */ }

  return (
    <div>
      <h2>通話履歴</h2>
      <p className="page-meta">{calls.length} 件の通話記録</p>
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>開始日時</th><th>電話番号</th><th>意図</th><th>本人確認</th>
              <th>状態</th><th>要約</th><th></th>
            </tr>
          </thead>
          <tbody>
            {calls.map(c => (
              <tr key={c.id}>
                <td>{new Date(c.startedAt).toLocaleString('ja-JP')}</td>
                <td>{c.callerPhone || '-'}</td>
                <td>{intentLabels[c.intent || ''] || c.intent || '-'}</td>
                <td>{c.verified ? '済' : '未'}</td>
                <td>
                  {c.emergencyFlag && <span className="badge emergency">緊急</span>}{' '}
                  {c.transferred && <span className="badge transfer">転送</span>}{' '}
                  {c.status === 'ACTIVE' && <span className="badge active">進行中</span>}{' '}
                  {c.status === 'ENDED' && !c.emergencyFlag && !c.transferred && (
                    <span className="badge ended">完了</span>
                  )}
                </td>
                <td>{c.summary ? c.summary.slice(0, 60) + '…' : '-'}</td>
                <td><Link href={`/calls/${c.id}`}>詳細</Link></td>
              </tr>
            ))}
            {calls.length === 0 && <tr><td colSpan={7}>通話データがありません</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
