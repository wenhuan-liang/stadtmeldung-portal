async function requestAccount(endpoint, username, password) {
  const response = await apiFetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.message || 'Anmeldung fehlgeschlagen.');
  return result.user;
}

// ---- Hilfsfunktionen ----

function showView(id) {
  document.querySelectorAll('.auth-view').forEach(v => v.style.display = 'none');
  document.getElementById(id).style.display = 'flex';
}

function showError(elId, msg) {
  const el = document.getElementById(elId);
  el.textContent = msg;
  el.style.display = 'block';
}

function hideError(elId) {
  document.getElementById(elId).style.display = 'none';
}

async function login(username, password, allowedRoles, errorId) {
  try {
    const user = await requestAccount('/api/login', username, password);
    if (!allowedRoles.includes(user.role)) {
      await apiFetch('/api/logout', { method: 'POST' });
      throw new Error('Dieser Account kann sich in diesem Bereich nicht anmelden.');
    }
    window.location.href = 'index.html';
  } catch (error) {
    showError(errorId, error.message);
  }
}

async function register(username, password) {
  await requestAccount('/api/register', username, password);
  window.location.href = 'index.html';
}

function onEnter(inputIds, btnId) {
  inputIds.forEach(id => {
    document.getElementById(id).addEventListener('keydown', e => {
      if (e.key === 'Enter') document.getElementById(btnId).click();
    });
  });
}

// ---- Event-Handler ----

document.addEventListener('DOMContentLoaded', () => {

  // Rolle wählen
  if (window.location.hash === '#user') {
    showView('view-user-login');
  }
  document.getElementById('btn-role-user').addEventListener('click', () => showView('view-user-login'));
  document.getElementById('btn-role-staff').addEventListener('click', () => showView('view-staff-login'));
  document.getElementById('link-to-register').addEventListener('click', e => { e.preventDefault(); showView('view-user-register'); });
  document.getElementById('link-register-from-user').addEventListener('click', e => { e.preventDefault(); showView('view-user-register'); });

  // User Login
  document.getElementById('btn-user-login').addEventListener('click', async () => {
    hideError('user-login-error');
    const username = document.getElementById('user-login-username').value.trim();
    const password = document.getElementById('user-login-password').value;
    await login(username, password, ['user'], 'user-login-error');
  });
  onEnter(['user-login-username', 'user-login-password'], 'btn-user-login');

  // Mitarbeiter Login
  document.getElementById('btn-staff-login').addEventListener('click', async () => {
    hideError('staff-login-error');
    const username = document.getElementById('staff-login-username').value.trim();
    const password = document.getElementById('staff-login-password').value;
    await login(username, password, ['staff', 'admin'], 'staff-login-error');
  });
  onEnter(['staff-login-username', 'staff-login-password'], 'btn-staff-login');

  // Register: Rolle wählen
  document.getElementById('btn-role-register-user').addEventListener('click', () => showView('view-user-register'));
  document.getElementById('link-to-login-from-register').addEventListener('click', e => { e.preventDefault(); showView('view-role'); });

  // Register: User
  document.getElementById('link-to-login-from-reg-user').addEventListener('click', e => { e.preventDefault(); showView('view-role'); });
  document.getElementById('btn-user-register').addEventListener('click', async () => {
    hideError('user-register-error');
    const username = document.getElementById('user-register-username').value.trim();
    const password = document.getElementById('user-register-password').value;
    if (!username || !password) { showError('user-register-error', 'Bitte alle Felder ausfüllen.'); return; }
    try { await register(username, password); }
    catch (err) { showError('user-register-error', err.message); }
  });
  onEnter(['user-register-username', 'user-register-password'], 'btn-user-register');

});
