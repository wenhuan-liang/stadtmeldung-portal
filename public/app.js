const form = document.getElementById('reportForm');
const reportList = document.getElementById('reportList');
const categoryFilter = document.getElementById('categoryFilter');
const latitudeInput = document.getElementById('latitude');
const longitudeInput = document.getElementById('longitude');
const loginForm = document.getElementById('loginForm');
const guestState = document.getElementById('guestState');
const userState = document.getElementById('userState');
const userNameLabel = document.getElementById('userNameLabel');
const userRoleBadge = document.getElementById('userRoleBadge');
const dashboardNotice = document.getElementById('dashboardNotice');
const logoutBtn = document.getElementById('logoutBtn');

const AUTH_KEY = 'stadtmeldung-user';
const defaultLocation = { lat: 52.52, lng: 13.405 };

const appState = {
  role: 'guest',
  username: '',
  displayName: 'Gast'
};

const map = L.map('map').setView([defaultLocation.lat, defaultLocation.lng], 11);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  attribution: '&copy; OpenStreetMap contributors',
  maxZoom: 19
}).addTo(map);

const marker = L.marker([defaultLocation.lat, defaultLocation.lng]).addTo(map);

function setLocation(lat, lng) {
  const location = { lat, lng };
  marker.setLatLng(location);
  map.panTo(location, { animate: true });
  latitudeInput.value = Number(lat).toFixed(6);
  longitudeInput.value = Number(lng).toFixed(6);
}

map.on('click', (event) => {
  setLocation(event.latlng.lat, event.latlng.lng);
});

setLocation(defaultLocation.lat, defaultLocation.lng);

function escapeHtml(value = '') {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function statusLabel(status) {
  const labels = {
    new: 'Neu',
    in_progress: 'In Bearbeitung',
    resolved: 'Erledigt'
  };
  return labels[status] || status;
}

function roleLabel(role) {
  const labels = {
    admin: 'Administrator',
    staff: 'Mitarbeiter',
    citizen: 'Bürger'
  };
  return labels[role] || 'Gast';
}

function formatDate(dateString) {
  if (!dateString) {
    return 'Keine Angabe';
  }

  const date = new Date(dateString);
  return new Intl.DateTimeFormat('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(date);
}

function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY) || 'null');
  } catch (error) {
    return null;
  }
}

function canManageReports() {
  return ['admin', 'staff'].includes(appState.role);
}

function getAuthHeaders() {
  const user = getStoredUser();
  return {
    'x-user-role': user?.role || appState.role || 'guest',
    'x-username': user?.username || appState.username || ''
  };
}

function updateAuthUI() {
  const storedUser = getStoredUser();

  if (storedUser) {
    appState.role = storedUser.role;
    appState.username = storedUser.username;
    appState.displayName = storedUser.displayName || storedUser.username;
  } else {
    appState.role = 'guest';
    appState.username = '';
    appState.displayName = 'Gast';
  }

  guestState.classList.toggle('hidden', Boolean(storedUser));
  userState.classList.toggle('hidden', !storedUser);
  userNameLabel.textContent = storedUser ? `${storedUser.displayName} (${storedUser.username})` : 'Nicht angemeldet';
  userRoleBadge.textContent = roleLabel(appState.role);

  if (storedUser) {
    dashboardNotice.textContent = `Angemeldet als ${storedUser.displayName} (${roleLabel(storedUser.role)}). Verwaltungsfunktionen sind aktiviert.`;
  } else {
    dashboardNotice.textContent = 'Gastmodus: Meldungen können erfasst werden, aber Verwaltungsfunktionen sind nur mit Login verfügbar.';
  }
}

function renderSummary(reports) {
  const counts = { new: 0, in_progress: 0, resolved: 0 };

  reports.forEach((report) => {
    if (counts[report.status] !== undefined) {
      counts[report.status] += 1;
    }
  });

  document.getElementById('summaryNew').textContent = counts.new;
  document.getElementById('summaryProgress').textContent = counts.in_progress;
  document.getElementById('summaryResolved').textContent = counts.resolved;
}

function renderReports(reports) {
  const selectedCategory = categoryFilter.value;
  const visibleReports = selectedCategory === 'all'
    ? reports
    : reports.filter((report) => report.category === selectedCategory);

  if (!visibleReports.length) {
    reportList.innerHTML = '<div class="empty-state">Keine Meldungen für diesen Filter vorhanden.</div>';
    return;
  }

  const canManage = canManageReports();

  reportList.innerHTML = visibleReports.map((report) => `
    <article class="report-item">
      <div class="report-header">
        <span class="category-pill">${escapeHtml(report.category)}</span>
        <span class="status-badge ${escapeHtml(report.status)}">${statusLabel(report.status)}</span>
      </div>
      <h3>${escapeHtml(report.title)}</h3>
      <p>${escapeHtml(report.description)}</p>

      ${report.photo ? `<img class="report-photo" src="${report.photo}" alt="Meldungsfoto" />` : '<div class="no-photo">Kein Foto hinterlegt</div>'}

      <div class="meta-grid">
        <div class="meta-box">
          <strong>Koordinaten</strong>
          <span>${Number(report.latitude).toFixed(5)}, ${Number(report.longitude).toFixed(5)}</span>
        </div>
        <div class="meta-box">
          <strong>Erfasst am</strong>
          <span>${formatDate(report.createdAt)}</span>
        </div>
      </div>

      ${canManage
        ? `<div class="report-actions">
            <button class="progress-btn" data-action="in_progress" data-id="${report.id}">In Bearbeitung</button>
            <button class="resolved-btn" data-action="resolved" data-id="${report.id}">Erledigt</button>
            <button class="delete-btn" data-action="delete" data-id="${report.id}">Löschen</button>
          </div>`
        : '<div class="limited-access">Nur eingeloggte Mitarbeiterinnen und Mitarbeiter können Meldungen verwalten.</div>'}
    </article>
  `).join('');
}

async function loadReports() {
  try {
    const response = await fetch('/api/reports');
    const reports = await response.json();
    renderSummary(reports);
    renderReports(reports);
  } catch (error) {
    reportList.innerHTML = '<div class="empty-state">Die Meldungen konnten nicht geladen werden.</div>';
    console.error(error);
  }
}

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = new FormData(loginForm);
  const username = String(formData.get('username') || '').trim();
  const password = String(formData.get('password') || '').trim();

  if (!username || !password) {
    alert('Bitte Benutzername und Passwort eingeben.');
    return;
  }

  try {
    const response = await fetch('/api/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password })
    });

    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Login fehlgeschlagen.');
    }

    localStorage.setItem(AUTH_KEY, JSON.stringify(result.user));
    updateAuthUI();
    loginForm.reset();
    await loadReports();
  } catch (error) {
    alert(error.message || 'Anmeldung fehlgeschlagen.');
  }
});

logoutBtn.addEventListener('click', () => {
  localStorage.removeItem(AUTH_KEY);
  updateAuthUI();
  loadReports();
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = new FormData(form);
  const title = String(formData.get('title') || '').trim();
  const description = String(formData.get('description') || '').trim();
  const latitude = Number(formData.get('latitude'));
  const longitude = Number(formData.get('longitude'));

  if (!title || !description || Number.isNaN(latitude) || Number.isNaN(longitude)) {
    alert('Bitte füllen Sie alle Pflichteinträge aus und wählen Sie einen Kartenpunkt aus.');
    return;
  }

  try {
    const response = await fetch('/api/reports', {
      method: 'POST',
      body: formData
    });

    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Meldevorgang fehlgeschlagen.');
    }

    form.reset();
    setLocation(defaultLocation.lat, defaultLocation.lng);
    await loadReports();
    alert('Meldung wurde erfolgreich gespeichert.');
  } catch (error) {
    alert(error.message || 'Ein Fehler ist aufgetreten.');
  }
});

categoryFilter.addEventListener('change', () => {
  loadReports();
});

reportList.addEventListener('click', async (event) => {
  const button = event.target.closest('button');
  if (!button) {
    return;
  }

  const { action, id } = button.dataset;
  if (!action || !id) {
    return;
  }

  const roleHeader = getAuthHeaders();

  try {
    if (action === 'delete') {
      const confirmDelete = window.confirm('Meldung wirklich löschen?');
      if (!confirmDelete) {
        return;
      }

      const response = await fetch(`/api/reports/${id}`, {
        method: 'DELETE',
        headers: {
          ...roleHeader
        }
      });
      if (!response.ok) {
        throw new Error('Löschen fehlgeschlagen.');
      }
    } else {
      const nextStatus = action === 'in_progress' ? 'in_progress' : 'resolved';
      const response = await fetch(`/api/reports/${id}`, {
        method: 'PATCH',
        headers: {
          ...roleHeader,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status: nextStatus })
      });

      if (!response.ok) {
        throw new Error('Statusaktualisierung fehlgeschlagen.');
      }
    }

    await loadReports();
  } catch (error) {
    alert(error.message || 'Aktion fehlgeschlagen.');
  }
});

updateAuthUI();
loadReports();
