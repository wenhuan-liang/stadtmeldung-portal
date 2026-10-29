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
        const result = await fetch('/api/logout', { method: 'POST' });
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
  if (meineMeldungenTab && (!session || session.role === 'staff')) {
    meineMeldungenTab.style.display = 'none';
  }
  const newReportButton = document.getElementById('btn-neue-meldung');
  if (newReportButton && session?.role === 'staff') {
    newReportButton.style.display = 'none';
  }
  window.stadtmeldungSession = session;
  const page = document.body.dataset.page;
  if (page === 'meine-meldungen' && (!session || session.role === 'staff')) {
    window.location.href = 'login.html';
  }
  if (page === 'hauptseite' && session?.role === 'admin') {
    window.location.href = 'mitarbeiter.html';
  }
  if (page === 'mitarbeiter' && session?.role !== 'admin') {
    window.location.href = session ? 'index.html' : 'login.html';
  }
  if (page === 'neue-meldung' && session?.role === 'staff') {
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
