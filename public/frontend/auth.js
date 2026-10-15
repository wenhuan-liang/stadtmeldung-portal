// Liest die aktuelle Session aus sessionStorage
function getSession() {
  try {
    return JSON.parse(sessionStorage.getItem('stadtmeldung_user') || 'null');
  } catch (_) { return null; }
}

function clearSession() {
  sessionStorage.removeItem('stadtmeldung_user');
}

// Passt die Navbar basierend auf der Session an
function updateNavbar() {
  const session = getSession();
  const authDiv = document.getElementById('navbar-auth');

  if (authDiv) {
    if (!session || session.role === 'anonymous') {
      authDiv.innerHTML = `<a href="login.html" class="nav-auth-link">Anmelden</a>`;
    } else {
      authDiv.innerHTML = `
        <span class="nav-user">${session.displayName}</span>
        <a href="#" class="nav-auth-link" id="logout-btn">Abmelden</a>
      `;
      document.getElementById('logout-btn').addEventListener('click', e => {
        e.preventDefault();
        clearSession();
        window.location.href = 'login.html';
      });
    }
  }

  // "Meine Meldungen"-Tab nur für eingeloggte User anzeigen
  const meineMeldungenTab = document.querySelector('a.tab[href="meine-meldungen.html"]');
  if (meineMeldungenTab && (!session || session.role === 'anonymous')) {
    meineMeldungenTab.style.display = 'none';
  }
}

document.addEventListener('DOMContentLoaded', updateNavbar);
