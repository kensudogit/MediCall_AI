'use client';

import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, Tenant } from '@/lib/api';
import { DEFAULT_TENANT, getClientTenant, setStoredTenant } from '@/lib/tenant';

type TenantContextValue = {
  slug: string;
  tenant: Tenant | null;
  tenants: Tenant[];
  setSlug: (slug: string) => void;
  refresh: () => Promise<void>;
};

const TenantContext = createContext<TenantContextValue>({
  slug: DEFAULT_TENANT,
  tenant: null,
  tenants: [],
  setSlug: () => {},
  refresh: async () => {},
});

export function TenantProvider({ children }: { children: React.ReactNode }) {
  const [slug, setSlugState] = useState(DEFAULT_TENANT);
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [tenants, setTenants] = useState<Tenant[]>([]);

  const refresh = useCallback(async () => {
    try {
      const [current, list] = await Promise.all([
        api<Tenant>('/api/admin/tenant'),
        api<Tenant[]>('/api/admin/tenants'),
      ]);
      setTenant(current);
      setTenants(list);
      setSlugState(current.slug);
      setStoredTenant(current.slug);
    } catch {
      /* API offline or tenant missing */
    }
  }, []);

  useEffect(() => {
    const stored = getClientTenant();
    setSlugState(stored);
    setStoredTenant(stored);
    refresh();
  }, [refresh]);

  const setSlug = useCallback((next: string) => {
    setStoredTenant(next);
    setSlugState(next);
    window.location.reload();
  }, []);

  return (
    <TenantContext.Provider value={{ slug, tenant, tenants, setSlug, refresh }}>
      {children}
    </TenantContext.Provider>
  );
}

export function useTenant() {
  return useContext(TenantContext);
}
