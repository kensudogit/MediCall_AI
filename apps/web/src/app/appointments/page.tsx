import { api, Appointment } from '@/lib/api';

export default async function AppointmentsPage() {
  let appointments: Appointment[] = [];
  try {
    appointments = await api<Appointment[]>('/api/admin/appointments');
  } catch { /* */ }

  return (
    <div>
      <h2>予約一覧</h2>
      <div className="card">
        <table>
          <thead>
            <tr><th>予約日時</th><th>患者ID</th><th>診療科</th><th>状態</th><th>備考</th></tr>
          </thead>
          <tbody>
            {appointments.map(a => (
              <tr key={a.id}>
                <td>{new Date(a.scheduledAt).toLocaleString('ja-JP')}</td>
                <td>{a.patientId || '-'}</td>
                <td>{a.department}</td>
                <td>{a.status}</td>
                <td>{a.notes || '-'}</td>
              </tr>
            ))}
            {appointments.length === 0 && <tr><td colSpan={5}>予約がありません</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
