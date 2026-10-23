// TODO: Ersetze getReports() mit fetch('/api/reports') wenn Backend bereit ist
const DEFAULT_REPORTS = [
  {
    id: 'sample-1',
    title: 'Defekte Straßenbeleuchtung',
    category: 'Defekt',
    description: 'Die Leuchte an der Ecke Hauptstraße / Lindenweg ist seit zwei Tagen außer Betrieb.',
    latitude: 52.520388,
    longitude: 13.40599,
    status: 'new',
    createdAt: '2026-10-05T09:30:00.000Z'
  },
  {
    id: 'sample-2',
    title: 'Wild abgeladener Müll',
    category: 'Verunreinigung',
    description: 'Auf dem Grünstreifen neben dem Parkplatz wurde Kunststoffmüll und Verpackung entsorgt.',
    latitude: 52.507641,
    longitude: 13.390512,
    status: 'in_progress',
    createdAt: '2026-10-06T14:20:00.000Z'
  },
  {
    id: 'sample-3',
    title: 'Falsch geparkt auf dem Radweg',
    category: 'Parkvergehen',
    description: 'Ein Fahrzeug blockiert den Radweg nahe der Schule.',
    latitude: 52.536452,
    longitude: 13.42119,
    status: 'resolved',
    createdAt: '2026-10-03T08:45:00.000Z'
  }
];

const CATEGORY_COLORS = {
  'Defekt': '#D4622A',
  'Verunreinigung': '#4A9A5E',
  'Parkvergehen': '#3A78BE',
  'Sonstiges': '#888888'
};

const STATUS_LABELS = {
  new: 'Neu',
  in_progress: 'In Bearbeitung',
  resolved: 'Erledigt'
};

const STATUS_COLORS = {
  new: '#E0822A',
  in_progress: '#3A88C4',
  resolved: '#4AA868'
};

const ALL_CATEGORIES = ['Defekt', 'Verunreinigung', 'Parkvergehen', 'Sonstiges'];

let activeStatusFilter = null;
let editingReportId = null;

let map;
let markerMap = {};
let activeCategories = new Set(ALL_CATEGORIES);

// Lädt Meldungen aus localStorage, setzt Demo-Daten beim ersten Start
function getReports() {
  try {
    const stored = localStorage.getItem('stadtmeldung_reports');
    if (stored) return JSON.parse(stored);
  } catch (_) {}
  localStorage.setItem('stadtmeldung_reports', JSON.stringify(DEFAULT_REPORTS));
  return DEFAULT_REPORTS;
}

// Auf "Meine Meldungen" nur selbst eingereichte Meldungen zeigen
function getPageReports() {
  const reports = getReports();
  if (document.body.dataset.page === 'meine-meldungen') {
    const session = JSON.parse(sessionStorage.getItem('stadtmeldung_user') || 'null');
    console.log('[app] getPageReports – Session:', session);
    console.log('[app] Alle Reports:', reports);
    if (!session || session.role === 'anonymous') return [];
    const mine = reports.filter(r => r.submittedByMe || r.submittedBy === session.username);
    console.log('[app] Meine Reports:', mine);
    return mine;
  }
  return reports;
}

function toCssClass(category) {
  return category.toLowerCase()
    .replace(/ß/g, 'ss').replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue')
    .replace(/\s+/g, '-');
}

function initMap() {
  map = L.map('map').setView([52.52, 13.405], 12);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(map);
}

function renderMarkers() {
  Object.values(markerMap).forEach(m => m.remove());
  markerMap = {};

  getPageReports()
    .filter(r => activeCategories.has(r.category) && (!activeStatusFilter || r.status === activeStatusFilter))
    .forEach(report => {
      const color = CATEGORY_COLORS[report.category] || '#888';
      const marker = L.circleMarker([report.latitude, report.longitude], {
        radius: 9,
        fillColor: color,
        color: '#fff',
        weight: 2,
        opacity: 1,
        fillOpacity: 0.88
      }).addTo(map);

      marker.bindPopup(
        `<strong>${report.title}</strong><br>` +
        `<span style="color:${color}">${report.category}</span> &mdash; ` +
        `<em>${STATUS_LABELS[report.status]}</em>`
      );

      markerMap[report.id] = marker;
    });
}

function renderStats() {
  const statsRow = document.getElementById('stats-row');
  if (!statsRow) return;

  const all = getPageReports().filter(r => activeCategories.has(r.category));
  const counts = { new: 0, in_progress: 0, resolved: 0 };
  all.forEach(r => { if (r.status in counts) counts[r.status]++; });

  const cards = [
    { key: null,         label: 'Alle',          count: all.length },
    { key: 'new',        label: 'Neu',            count: counts.new },
    { key: 'in_progress',label: 'In Bearbeitung', count: counts.in_progress },
    { key: 'resolved',   label: 'Erledigt',       count: counts.resolved },
  ];

  statsRow.innerHTML = cards.map(c => `
    <div class="stat-card${activeStatusFilter === c.key ? ' stat-card--active' : ''}" data-status="${c.key ?? ''}">
      <div class="stat-label">${c.label}</div>
      <div class="stat-value">${c.count}</div>
    </div>
  `).join('');

  statsRow.querySelectorAll('.stat-card').forEach(card => {
    card.addEventListener('click', () => {
      const key = card.dataset.status || null;
      activeStatusFilter = activeStatusFilter === key ? null : key;
      renderStats();
      if (map) renderMarkers();
      renderList();
    });
  });
}

function renderList() {
  const list = document.getElementById('reports-list');
  if (!list) return;

  let filtered = getPageReports().filter(r => activeCategories.has(r.category));
  if (activeStatusFilter) {
    filtered = filtered.filter(r => r.status === activeStatusFilter);
  }

  if (filtered.length === 0) {
    const isMeinePage = document.body.dataset.page === 'meine-meldungen';
    list.innerHTML = `<li class="reports-empty">${
      isMeinePage
        ? 'Du hast noch keine Meldungen eingereicht.'
        : 'Keine Meldungen für den gewählten Filter.'
    }</li>`;
    return;
  }

  const isMeinePage = document.body.dataset.page === 'meine-meldungen';

  list.innerHTML = filtered.map(r => `
    <li class="report-card" data-id="${r.id}">
      <div class="report-card-header">
        <span class="category-badge cat-${toCssClass(r.category)}">${r.category}</span>
        <span class="status-badge" style="background: ${STATUS_COLORS[r.status]}">${STATUS_LABELS[r.status]}</span>
        ${isMeinePage ? `<button class="btn-edit-report" data-id="${r.id}">Bearbeiten</button>` : ''}
      </div>
      <h3 class="report-card-title">${r.title}</h3>
      <p class="report-card-desc">${r.description}</p>
      <div class="photo-placeholder">${r.photo ? 'Foto hinterlegt' : 'Kein Foto hinterlegt'}</div>
      <div class="report-card-meta">
        <div>
          <span class="meta-label">Koordinaten</span>
          <span class="meta-value">${Number(r.latitude).toFixed(5)}, ${Number(r.longitude).toFixed(5)}</span>
        </div>
        <div>
          <span class="meta-label">Erfasst am</span>
          <span class="meta-value">${formatDate(r.createdAt)}</span>
        </div>
      </div>
    </li>
  `).join('');

  if (isMeinePage) {
    list.querySelectorAll('.btn-edit-report').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        showEditView(btn.dataset.id);
      });
    });
  }
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
}

function setupFilters() {
  document.querySelectorAll('.filter-group input[type="checkbox"]').forEach(cb => {
    cb.addEventListener('change', () => {
      if (cb.checked) {
        activeCategories.add(cb.value);
      } else {
        activeCategories.delete(cb.value);
      }
      renderStats();
      renderMarkers();
      renderList();
    });
  });
}

function setupListClick() {
  const list = document.getElementById('reports-list');
  if (!list) return;

  list.addEventListener('click', e => {
    if (e.target.closest('.btn-edit-report')) return;
    const item = e.target.closest('.report-card');
    if (!item) return;

    const report = getPageReports().find(r => r.id === item.dataset.id);
    const marker = markerMap[item.dataset.id];
    if (report && marker) {
      map.setView([report.latitude, report.longitude], 15, { animate: true });
      marker.openPopup();
    }
  });
}

function showEditView(reportId) {
  const report = getReports().find(r => r.id === reportId);
  if (!report) return;
  editingReportId = reportId;
  document.getElementById('edit-title').value       = report.title;
  document.getElementById('edit-category').value    = report.category;
  document.getElementById('edit-description').value = report.description || '';
  document.getElementById('edit-error-title').textContent = '';
  document.getElementById('edit-title').classList.remove('input-error');
  document.getElementById('view-list').style.display = 'none';
  document.getElementById('view-edit').style.display = 'block';
}

function showListView() {
  editingReportId = null;
  document.getElementById('view-edit').style.display = 'none';
  document.getElementById('view-list').style.display = 'block';
}

function saveEdit(e) {
  e.preventDefault();
  const title       = document.getElementById('edit-title').value.trim();
  const category    = document.getElementById('edit-category').value;
  const description = document.getElementById('edit-description').value.trim();

  if (!title) {
    document.getElementById('edit-title').classList.add('input-error');
    document.getElementById('edit-error-title').textContent = 'Bitte gib einen Titel ein.';
    return;
  }

  const reports = getReports();
  const r = reports.find(r => r.id === editingReportId);
  if (!r) return;
  r.title       = title;
  r.category    = category;
  r.description = description;
  localStorage.setItem('stadtmeldung_reports', JSON.stringify(reports));

  showListView();
  renderStats();
  renderMarkers();
  renderList();
}

document.addEventListener('DOMContentLoaded', () => {
  if (document.getElementById('map')) {
    initMap();
    renderMarkers();
  }
  renderStats();
  renderList();
  setupFilters();
  setupListClick();

  const fabBtn = document.getElementById('btn-neue-meldung');
  if (fabBtn) fabBtn.addEventListener('click', () => { window.location.href = 'neue-meldung.html'; });

  const editForm = document.getElementById('edit-form');
  if (editForm) editForm.addEventListener('submit', saveEdit);

  const btnBack = document.getElementById('btn-edit-back');
  if (btnBack) btnBack.addEventListener('click', showListView);
});
