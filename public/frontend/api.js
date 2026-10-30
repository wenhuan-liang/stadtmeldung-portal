async function apiFetch(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers || {});

  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    const csrfResponse = await fetch('/api/auth/csrf', {
      credentials: 'same-origin'
    });
    const csrf = await csrfResponse.json();
    if (!csrfResponse.ok) {
      throw new Error(csrf.message || 'Sicherheitsprüfung fehlgeschlagen.');
    }
    headers.set(csrf.headerName, csrf.token);
  }

  return fetch(url, {
    ...options,
    method,
    headers,
    credentials: 'same-origin'
  });
}
