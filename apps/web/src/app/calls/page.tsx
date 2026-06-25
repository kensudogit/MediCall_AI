import { api, CallSession } from '@/lib/api';
import Link from 'next/link';

export default async function CallsPage() {
  let calls: CallSession[] = [];
  try {
    calls = await api<CallSession[]>('/api/admin/calls');
  } catch { /* */ }

  return (
    <div>
      <h2>通話履歴</h2>
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>開始日時</th><th>電話番号</th><th>意図</th><th>本人確認</th>
              <th>転送</th><th>要約</th><th></th>
            </tr>
          </thead>
          <tbody>
            {calls.map(c => (
              <tr key={c.id}>
                <td>{new Date(c.startedAt).toLocaleString('ja-JP')}</td>
                <td>{c.callerPhone || '-'}</td>
                <td>{c.intent || '-'}</td>
                <td>{c.verified ? '済' : '未'}</td>
                <td>{c.transferred ? c.transferReason || 'あり' : '-'}</td>
                <td>{c.summary ? c.summary.slice(0, 80) : '-'}</td>
                <td><Link href={`/calls/${c.id}`}>詳細</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
