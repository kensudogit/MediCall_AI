import { getBackendUrl } from '@/lib/backend';

const BACKEND_HEALTH = getBackendUrl('/api/health');

export async function GET() {
  try {
    const res = await fetch(BACKEND_HEALTH, { cache: 'no-store' });
    const body = await res.json();
    return Response.json(body, { status: res.status });
  } catch {
    return Response.json({ status: 'unavailable', service: 'medicall-ai' }, { status: 503 });
  }
}
