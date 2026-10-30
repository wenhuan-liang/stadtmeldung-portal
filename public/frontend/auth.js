async function updateNavbar() {
  const authDiv = document.getElementById('navbar-auth');
  let session = null;

  if (authDiv) {
    const response = await fetch('/api/session');
    if (response.ok) {
      const body = await response.json();
      session = body.user;
    } else if (response.status !== 401) {
      throw new Error('Session status could not be checked.');
    }

    authDiv.replaceChildren();
    if (session) {
      const name = document.createElement('span');
      name.className = 'nav-user';
      name.textContent = session.displayName;
      const logout = document.createElement('button');
      logout.type = 'button';
      logout.className = 'nav-auth-link';
      logout.textContent = 'Abmelden';
      logout.addEventListener('click', async () => {
        const result = await apiFetch('/api/logout', { method: 'POST' });
        if (!result.ok) throw new Error('Abmelden fehlgeschlagen.');
        window.location.href = 'login.html';
      });
      authDiv.append(name, logout);
    } else {
      const login = document.createElement('a');
      login.href = 'login.html#user';
      login.className = 'nav-auth-link';
      login.textContent = 'Anmelden';
      authDiv.append(login);
    }
  }

  const meineMeldungenTab = document.querySelector('a.tab[href="meine-meldungen.html"]');
  const isCityStaff = ['staff', 'admin'].includes(session?.role);
  if (meineMeldungenTab && (!session || isCityStaff)) {
    meineMeldungenTab.style.display = 'none';
  }
  const employeeManagementTab = document.querySelector('a.tab[href="mitarbeiter.html"]');
  if (employeeManagementTab) {
    employeeManagementTab.style.display =
      session?.role === 'admin' ? 'flex' : 'none';
  }
  const newReportButton = document.getElementById('btn-neue-meldung');
  if (newReportButton && isCityStaff) {
    newReportButton.style.display = 'none';
  }
  window.stadtmeldungSession = session;
  const page = document.body.dataset.page;
  if (page === 'meine-meldungen' && (!session || isCityStaff)) {
    window.location.href = 'login.html';
  }
  if (page === 'mitarbeiter' && session?.role !== 'admin') {
    window.location.href = session ? 'index.html' : 'login.html';
  }
  if (page === 'neue-meldung' && isCityStaff) {
    window.location.href = 'index.html';
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.stadtmeldungSessionReady = updateNavbar().catch(error => {
    console.error('Session konnte nicht geladen werden:', error);
    const authDiv = document.getElementById('navbar-auth');
    if (authDiv) authDiv.textContent = 'Verbindung zum Backend fehlgeschlagen.';
  });
});
