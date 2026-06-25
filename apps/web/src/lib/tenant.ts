export const TENANT_COOKIE = 'medicall-tenant';
export const TENANT_STORAGE_KEY = 'medicall-tenant';
export const DEFAULT_TENANT = 'demo';

export function setStoredTenant(slug: string) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(TENANT_STORAGE_KEY, slug);
  document.cookie = `${TENANT_COOKIE}=${encodeURIComponent(slug)}; path=/; max-age=31536000; SameSite=Lax`;
}

export function getClientTenant(): string {
  if (typeof window === 'undefined') return DEFAULT_TENANT;
  return localStorage.getItem(TENANT_STORAGE_KEY) || DEFAULT_TENANT;
}
