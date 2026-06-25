import { PatientSearch } from '@/components/PatientSearch';

export default function PatientsPage() {
  return (
    <div>
      <h2>患者検索</h2>
      <p className="page-meta" style={{ marginBottom: '1rem' }}>
        電話番号または氏名で患者を検索し、通話履歴・予約を確認できます。
      </p>
      <PatientSearch />
    </div>
  );
}
