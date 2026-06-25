'use client';

import { useEffect, useState } from 'react';
import { api, FaqItem } from '@/lib/api';

export default function FaqPage() {
  const [items, setItems] = useState<FaqItem[]>([]);
  const [editing, setEditing] = useState<FaqItem | null>(null);
  const [form, setForm] = useState<FaqItem>({
    category: '一般', question: '', answer: '', active: true, sortOrder: 0,
  });

  const load = () => api<FaqItem[]>('/api/admin/faq').then(setItems).catch(() => {});
  useEffect(() => { load(); }, []);

  const save = async () => {
    if (editing?.id) {
      await api(`/api/admin/faq/${editing.id}`, { method: 'PUT', body: JSON.stringify(form) });
    } else {
      await api('/api/admin/faq', { method: 'POST', body: JSON.stringify(form) });
    }
    setEditing(null);
    setForm({ category: '一般', question: '', answer: '', active: true, sortOrder: 0 });
    load();
  };

  const remove = async (id: number) => {
    if (!confirm('削除しますか？')) return;
    await api(`/api/admin/faq/${id}`, { method: 'DELETE' });
    load();
  };

  return (
    <div>
      <h2>FAQ管理</h2>
      <div className="card">
        <h3>{editing ? 'FAQ編集' : '新規FAQ'}</h3>
        <div className="form-group">
          <label>カテゴリ</label>
          <input value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} />
        </div>
        <div className="form-group">
          <label>質問</label>
          <input value={form.question} onChange={e => setForm({ ...form, question: e.target.value })} />
        </div>
        <div className="form-group">
          <label>回答</label>
          <textarea rows={4} value={form.answer} onChange={e => setForm({ ...form, answer: e.target.value })} />
        </div>
        <button onClick={save}>{editing ? '更新' : '追加'}</button>
        {editing && <button style={{ marginLeft: 8 }} onClick={() => setEditing(null)}>キャンセル</button>}
      </div>
      <div className="card">
        <table>
          <thead><tr><th>カテゴリ</th><th>質問</th><th>回答</th><th>有効</th><th></th></tr></thead>
          <tbody>
            {items.map(item => (
              <tr key={item.id}>
                <td>{item.category}</td>
                <td>{item.question}</td>
                <td>{item.answer.slice(0, 60)}...</td>
                <td>{item.active ? '○' : '×'}</td>
                <td>
                  <button onClick={() => { setEditing(item); setForm(item); }}>編集</button>{' '}
                  <button className="danger" onClick={() => item.id && remove(item.id)}>削除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
