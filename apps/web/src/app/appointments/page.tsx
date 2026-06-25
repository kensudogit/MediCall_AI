import { api, Appointment } from '@/lib/api';

const statusLabels: Record<string, string> = {
  CONFIRMED: '確定',
  CANCELLED: 'キャンセル',
};

export default async function AppointmentsPage() {
  let appointments: Appointment[] = [];
  try {
    appointments = await api<Appointment[]>('/api/admin/appointments');
  } catch { /* */ }

  const confirmed = appointments.filter(a => a.status === 'CONFIRMED');
  const cancelled = appointments.filter(a => a.status === 'CANCELLED');

  return (
    <div>
      <h2>予約一覧</h2>
      <div className="grid" style={{ marginBottom: '1rem' }}>
        <div className="card"><div className="stat">{confirmed.length}</div><div className="label">確定予約</div></div>
        <div className="card"><div className="stat">{cancelled.length}</div><div className="label">キャンセル</div></div>
      </div>
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>予約日時</th><th>患者名</th><th>電話番号</th><th>診療科</th><th>状態</th><th>備考</th>
            </tr>
          </thead>
          <tbody>
            {appointments.map(a => (
              <tr key={a.id}>
                <td>{new Date(a.scheduledAt).toLocaleString('ja-JP')}</td>
                <td>{a.patientName || '-'}</td>
                <td>{a.patientPhone || '-'}</td>
                <td>{a.department}</td>
                <td>
                  <span className={`badge ${a.status === 'CANCELLED' ? 'cancelled' : 'confirmed'}`}>
                    {statusLabels[a.status] || a.status}
                  </span>
                </td>
                <td>{a.notes || '-'}</td>
              </tr>
            ))}
            {appointments.length === 0 && <tr><td colSpan={6}>予約がありません</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
