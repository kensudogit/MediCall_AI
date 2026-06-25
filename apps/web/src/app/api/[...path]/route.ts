import { NextRequest } from 'next/server';
import { getBackendUrl } from '@/lib/backend';

type RouteContext = { params: { path: string[] } };

async function proxy(req: NextRequest, { params }: RouteContext) {
  const path = `/api/${params.path.join('/')}`;
  const target = `${getBackendUrl(path)}${req.nextUrl.search}`;

  const headers = new Headers();
  const contentType = req.headers.get('content-type');
  if (contentType) headers.set('content-type', contentType);
  const accept = req.headers.get('accept');
  if (accept) headers.set('accept', accept);

  const init: RequestInit = {
    method: req.method,
    headers,
    cache: 'no-store',
  };

  if (req.method !== 'GET' && req.method !== 'HEAD') {
    init.body = await req.arrayBuffer();
  }

  try {
    const res = await fetch(target, init);
    const body = await res.arrayBuffer();
    const responseHeaders = new Headers();
    const resType = res.headers.get('content-type');
    if (resType) responseHeaders.set('content-type', resType);
    return new Response(body, { status: res.status, headers: responseHeaders });
  } catch (err) {
    const message = err instanceof Error ? err.message : 'backend unreachable';
    return Response.json(
      { error: 'backend_unreachable', message, path },
      { status: 503 },
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const OPTIONS = proxy;
