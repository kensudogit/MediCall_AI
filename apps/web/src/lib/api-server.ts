import { DEFAULT_TENANT, TENANT_COOKIE } from './tenant';

const API_BASE = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function api<T>(path: string, options?: RequestInit): Promise<T> {
  const { cookies } = await import('next/headers');
  const tenant = cookies().get(TENANT_COOKIE)?.value || DEFAULT_TENANT;

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Slug': tenant,
      ...(options?.headers || {}),
    },
    cache: 'no-store',
  });
  if (!res.ok) {
    let detail = `HTTP ${res.status}`;
    try {
      const body = await res.json();
      if (body?.message) detail = body.message;
      else if (body?.error) detail = body.error;
    } catch {
      /* not json */
    }
    throw new Error(detail);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export type {
  Tenant,
  CreateTenantRequest,
  CallSession,
  CallTurn,
  FaqItem,
  ClinicSettings,
  Appointment,
  AdminStats,
  Patient,
  PatientDetail,
  HealthStatus,
  CallStartResponse,
  CallResponse,
  CallEndResponse,
} from './api';
