import { api } from '@/lib/api-server';
import { CallSession, CallTurn } from '@/lib/api';

export default async function CallDetailPage({ params }: { params: { id: string } }) {
  let session: CallSession | null = null;
  let turns: CallTurn[] = [];
  try {
    session = await api<CallSession>(`/api/admin/calls/${params.id}`);
    turns = await api<CallTurn[]>(`/api/admin/calls/${params.id}/turns`);
  } catch { /* */ }

  if (!session) return <div><h2>通話が見つかりません</h2></div>;

  return (
    <div>
      <h2>通話詳細</h2>
      <div className="card">
        <p><strong>ID:</strong> {session.id}</p>
        <p><strong>電話番号:</strong> {session.callerPhone || '-'}</p>
        <p><strong>状態:</strong> {session.status}</p>
        <p><strong>意図:</strong> {session.intent || '-'}</p>
        {session.emergencyFlag && <p className="badge emergency">緊急対応</p>}
        {session.transferred && <p>転送理由: {session.transferReason}</p>}
      </div>
      {session.summary && (
        <div className="card">
          <h3>通話要約</h3>
          <p>{session.summary}</p>
        </div>
      )}
      <div className="card">
        <h3>会話ログ</h3>
        {turns.map(t => (
          <div key={t.id} style={{ marginBottom: '0.75rem' }}>
            <span className={t.role === 'caller' ? 'turn-caller' : 'turn-assistant'}>
              [{t.role === 'caller' ? '患者' : 'AI'}]
            </span>{' '}
            {t.content}
            {t.intent && <small style={{ color: '#888' }}> ({t.intent})</small>}
          </div>
        ))}
      </div>
    </div>
  );
}
