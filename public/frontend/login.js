const MOCK_USERS = [
  { username: 'buerger',     password: 'buerger123',     role: 'user',  displayName: 'Bürger' },
  { username: 'mitarbeiter', password: 'mitarbeiter123', role: 'staff', displayName: 'Mitarbeiter' },
  { username: 'admin',       password: 'admin123',       role: 'admin', displayName: 'Administrator' }
];

// ---- Mock-Datenzugriff ----
// Diese zwei Funktionen werden beim Backend-Merge durch echte API-Calls ersetzt.

function getAllUsers() {
  try {
    const stored = JSON.parse(localStorage.getItem('stadtmeldung_users') || '[]');
    return [...MOCK_USERS, ...stored];
  } catch (_) { return MOCK_USERS; }
}

// TODO: ersetze Inhalt mit:
//   const res = await fetch('/api/login', { method:'POST',
//     headers:{'Content-Type':'application/json'},
//     body: JSON.stringify({ username, password }) });
//   if (!res.ok) return null;
//   return await res.json(); // { username, role, displayName }
function mockLogin(username, password, allowedRoles) {
  return getAllUsers().find(
    u => u.username === username && u.password === password && allowedRoles.includes(u.role)
  ) || null;
}

// TODO: ersetze Inhalt mit:
//   const res = await fetch('/api/register', { method:'POST',
//     headers:{'Content-Type':'application/json'},
//     body: JSON.stringify({ username, password }) });
//   if (!res.ok) throw new Error(await res.text());
//   return await res.json(); // { username, role, displayName }
function mockRegister(username, password) {
  if (getAllUsers().find(u => u.username === username)) {
    throw new Error('Benutzername bereits vergeben.');
  }
  const stored = JSON.parse(localStorage.getItem('stadtmeldung_users') || '[]');
  stored.push({ username, password, role: 'user', displayName: username });
  localStorage.setItem('stadtmeldung_users', JSON.stringify(stored));
  return { username, role: 'user', displayName: username };
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

function saveSession(role, username, displayName) {
  sessionStorage.setItem('stadtmeldung_user', JSON.stringify({ role, username, displayName }));
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
  document.getElementById('btn-role-user').addEventListener('click', () => showView('view-user-login'));
  document.getElementById('btn-role-staff').addEventListener('click', () => showView('view-staff-login'));
  document.getElementById('link-to-register').addEventListener('click', e => { e.preventDefault(); showView('view-register'); });
  document.getElementById('link-anon').addEventListener('click', e => {
    e.preventDefault();
    saveSession('anonymous', 'anonym', 'Anonym');
    window.location.href = 'index.html';
  });

  // User Login
  document.getElementById('btn-back-from-user').addEventListener('click', () => showView('view-role'));
  document.getElementById('link-register-from-user').addEventListener('click', e => { e.preventDefault(); showView('view-register'); });
  document.getElementById('btn-user-login').addEventListener('click', () => {
    hideError('user-login-error');
    const username = document.getElementById('user-login-username').value.trim();
    const password = document.getElementById('user-login-password').value;
    const user = mockLogin(username, password, ['user']);
    if (!user) { showError('user-login-error', 'Benutzername oder Passwort falsch.'); return; }
    saveSession(user.role, user.username, user.displayName);
    window.location.href = 'index.html';
  });
  onEnter(['user-login-username', 'user-login-password'], 'btn-user-login');

  // Mitarbeiter Login
  document.getElementById('btn-back-from-staff').addEventListener('click', () => showView('view-role'));
  document.getElementById('btn-staff-login').addEventListener('click', () => {
    hideError('staff-login-error');
    const username = document.getElementById('staff-login-username').value.trim();
    const password = document.getElementById('staff-login-password').value;
    const user = mockLogin(username, password, ['staff', 'admin']);
    if (!user) { showError('staff-login-error', 'Benutzername oder Passwort falsch.'); return; }
    saveSession(user.role, user.username, user.displayName);
    window.location.href = 'admin.html';
  });
  onEnter(['staff-login-username', 'staff-login-password'], 'btn-staff-login');

  // Register: Rolle wählen
  document.getElementById('btn-role-register-user').addEventListener('click', () => showView('view-user-register'));
  document.getElementById('link-to-login-from-register').addEventListener('click', e => { e.preventDefault(); showView('view-role'); });

  // Register: User
  document.getElementById('link-to-login-from-reg-user').addEventListener('click', e => { e.preventDefault(); showView('view-role'); });
  document.getElementById('btn-user-register').addEventListener('click', () => {
    hideError('user-register-error');
    const username = document.getElementById('user-register-username').value.trim();
    const password = document.getElementById('user-register-password').value;
    if (!username || !password) { showError('user-register-error', 'Bitte alle Felder ausfüllen.'); return; }
    try {
      const user = mockRegister(username, password);
      saveSession(user.role, user.username, user.displayName);
      window.location.href = 'index.html';
    } catch (err) {
      showError('user-register-error', err.message);
    }
  });
  onEnter(['user-register-username', 'user-register-password'], 'btn-user-register');

});
