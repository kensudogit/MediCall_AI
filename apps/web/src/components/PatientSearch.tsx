'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api, Patient } from '@/lib/api';

export function PatientSearch() {
  const [phone, setPhone] = useState('');
  const [name, setName] = useState('');
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const search = async () => {
    setLoading(true);
    setSearched(true);
    try {
      const params = new URLSearchParams();
      if (phone.trim()) params.set('phone', phone.trim());
      if (name.trim()) params.set('name', name.trim());
      const q = params.toString();
      const data = await api<Patient[]>(`/api/admin/patients${q ? `?${q}` : ''}`);
      setPatients(data);
    } catch {
      setPatients([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    search();
  }, []);

  return (
    <div>
      <div className="card search-bar">
        <div className="search-fields">
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>電話番号</label>
            <input
              value={phone}
              onChange={e => setPhone(e.target.value)}
              placeholder="090..."
              onKeyDown={e => e.key === 'Enter' && search()}
            />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label>氏名</label>
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="山田"
              onKeyDown={e => e.key === 'Enter' && search()}
            />
          </div>
          <button type="button" onClick={search} disabled={loading}>
            {loading ? '検索中…' : '検索'}
          </button>
        </div>
      </div>

      <div className="card">
        <table>
          <thead>
            <tr>
              <th>氏名</th><th>フリガナ</th><th>生年月日</th><th>電話番号</th><th>本人確認</th><th></th>
            </tr>
          </thead>
          <tbody>
            {patients.map(p => (
              <tr key={p.id}>
                <td>{p.fullName}</td>
                <td>{p.nameKana || '-'}</td>
                <td>{p.dateOfBirth}</td>
                <td>{p.phoneNumber}</td>
                <td>{p.verified ? '済' : '未'}</td>
                <td><Link href={`/patients/${p.id}`}>詳細</Link></td>
              </tr>
            ))}
            {searched && patients.length === 0 && (
              <tr><td colSpan={6}>該当する患者がいません</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
