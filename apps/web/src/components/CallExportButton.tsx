'use client';

import { getClientTenant } from '@/lib/tenant';

export function CallExportButton() {
  const download = async () => {
    const to = new Date().toISOString();
    const from = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString();
    const url = `/api/admin/calls/export?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;
    const res = await fetch(url, {
      headers: { 'X-Tenant-Slug': getClientTenant() },
    });
    if (!res.ok) return;
    const blob = await res.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `calls-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(a.href);
  };

  return (
    <button type="button" className="secondary" onClick={download}>
      CSVエクスポート（直近30日）
    </button>
  );
}
