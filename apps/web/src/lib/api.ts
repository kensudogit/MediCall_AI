import { getClientTenant } from './tenant';

const API_BASE =
  typeof window !== 'undefined'
    ? ''
    : process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function api<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Slug': getClientTenant(),
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

export interface Tenant {
  id: number;
  slug: string;
  name: string;
  status: string;
  plan: string;
  createdAt: string;
}

export interface CreateTenantRequest {
  slug: string;
  name: string;
  plan?: string;
  hoursText?: string;
  holidaysText?: string;
  accessText?: string;
  belongingsText?: string;
}

export interface CallSession {
  id: string;
  callerPhone?: string;
  status: string;
  intent?: string;
  verified: boolean;
  emergencyFlag: boolean;
  transferred: boolean;
  transferReason?: string;
  summary?: string;
  startedAt: string;
  endedAt?: string;
}

export interface CallTurn {
  id: number;
  role: string;
  content: string;
  intent?: string;
  action?: string;
  createdAt: string;
}

export interface FaqItem {
  id?: number;
  category: string;
  question: string;
  answer: string;
  active: boolean;
  sortOrder: number;
}

export interface ClinicSettings {
  id?: number;
  clinicName: string;
  hoursText: string;
  holidaysText: string;
  accessText: string;
  belongingsText: string;
}

export interface Appointment {
  id: number;
  patientId?: number;
  patientName?: string;
  patientPhone?: string;
  scheduledAt: string;
  department: string;
  status: string;
  notes?: string;
}

export interface AdminStats {
  patientCount: number;
  appointmentCount: number;
  confirmedAppointments: number;
  callCount: number;
  activeCalls: number;
  transferredCalls: number;
  emergencyCalls: number;
  intentCounts?: { intent: string; count: number }[];
  callsLast7Days?: { date: string; count: number }[];
}

export interface Patient {
  id: number;
  fullName: string;
  nameKana?: string;
  dateOfBirth: string;
  phoneNumber: string;
  verified: boolean;
}

export interface PatientDetail {
  patient: Patient;
  recentCalls: CallSession[];
  appointments: Appointment[];
}

export interface HealthStatus {
  status: string;
  openai: boolean;
}

export interface CallStartResponse {
  sessionId: string;
  status: string;
  greeting: string;
  tenantSlug?: string;
}

export interface CallResponse {
  text: string;
  action: string;
  intent: string;
  transfer: boolean;
  transferReason?: string;
  audioUrl?: string;
}

export interface CallEndResponse {
  summary: string;
}
