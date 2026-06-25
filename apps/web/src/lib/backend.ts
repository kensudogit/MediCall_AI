export function getBackendBaseUrl(): string {
  return (
    process.env.API_URL ||
    `http://127.0.0.1:${process.env.BACKEND_PORT || '8081'}`
  );
}

export function getBackendUrl(path: string): string {
  const base = getBackendBaseUrl();
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`;
}
