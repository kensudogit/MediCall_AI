import { api, CallSession } from '@/lib/api-server';
import { intentLabel } from '@/lib/intents';
import Link from 'next/link';
import { CallExportButton } from '@/components/CallExportButton';

export default async function CallsPage() {
  let calls: CallSession[] = [];
  try {
    calls = await api<CallSession[]>('/api/admin/calls');
  } catch { /* */ }

  return (
    <div>
      <div className="page-header-row">
        <div>
          <h2>通話履歴</h2>
          <p className="page-meta">{calls.length} 件の通話記録</p>
        </div>
        <CallExportButton />
      </div>
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
                <td>{intentLabel(c.intent)}</td>
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
