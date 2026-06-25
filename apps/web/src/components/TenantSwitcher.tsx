'use client';

import { useTenant } from '@/components/TenantProvider';

export function TenantSwitcher() {
  const { slug, tenant, tenants, setSlug } = useTenant();

  if (tenants.length === 0) {
    return (
      <div className="tenant-switcher">
        <span className="tenant-label">テナント</span>
        <span className="tenant-current">{slug}</span>
      </div>
    );
  }

  return (
    <div className="tenant-switcher">
      <label className="tenant-label" htmlFor="tenant-select">クリニック</label>
      <select
        id="tenant-select"
        className="tenant-select"
        value={slug}
        onChange={e => setSlug(e.target.value)}
      >
        {tenants.map(t => (
          <option key={t.id} value={t.slug}>
            {t.name} ({t.slug})
          </option>
        ))}
      </select>
      {tenant && <span className="tenant-plan">{tenant.plan}</span>}
    </div>
  );
}
