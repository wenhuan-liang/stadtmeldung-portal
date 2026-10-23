const CATEGORY_COLORS = {
  'Defekt': '#D4622A',
  'Verunreinigung': '#4A9A5E',
  'Parkvergehen': '#3A78BE',
  'Sonstiges': '#888888'
};
const STATUS_LABELS = { new: 'Neu', in_progress: 'In Bearbeitung', resolved: 'Erledigt' };
const STATUS_COLORS = { new: '#E0822A', in_progress: '#3A88C4', resolved: '#4AA868' };
const ALL_CATEGORIES = ['Defekt', 'Verunreinigung', 'Parkvergehen', 'Sonstiges'];

let map, detailMap;
let markerMap = {};
let activeCategories = new Set(ALL_CATEGORIES);
let activeStatusFilter = null;
let dateVon = null, dateBis = null;
let currentReportId = null;

// ---- Datenzugriff ----
// TODO: getReports → GET /api/reports, saveReports → PATCH /api/reports/:id

function getReports() {
  try { return JSON.parse(localStorage.getItem('stadtmeldung_reports') || '[]'); }
  catch (_) { return []; }
}

function saveReports(reports) {
  localStorage.setItem('stadtmeldung_reports', JSON.stringify(reports));
}

function getFiltered() {
  return getReports().filter(r => {
    if (!activeCategories.has(r.category)) return false;
    if (activeStatusFilter && r.status !== activeStatusFilter) return false;
    if (dateVon && r.createdAt.slice(0, 10) < dateVon) return false;
    if (dateBis && r.createdAt.slice(0, 10) > dateBis) return false;
    return true;
  });
}

// ---- Hilfsfunktionen ----

function toCssClass(cat) {
  return cat.toLowerCase()
    .replace(/ß/g,'ss').replace(/ä/g,'ae').replace(/ö/g,'oe').replace(/ü/g,'ue')
    .replace(/\s+/g,'-');
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('de-DE', { day:'2-digit', month:'2-digit', year:'numeric' });
}

// ---- Karte ----

function initMap() {
  map = L.map('map').setView([52.52, 13.405], 12);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(map);
}

function renderMarkers() {
  Object.values(markerMap).forEach(m => m.remove());
  markerMap = {};
  getFiltered().forEach(r => {
    const color = CATEGORY_COLORS[r.category] || '#888';
    const marker = L.circleMarker([r.latitude, r.longitude], {
      radius: 9, fillColor: color, color: '#fff', weight: 2, opacity: 1, fillOpacity: 0.88
    }).addTo(map);
    marker.bindPopup(`<strong>${r.title}</strong><br>
      <span style="color:${color}">${r.category}</span> — <em>${STATUS_LABELS[r.status]}</em>`);
    marker.on('click', () => showDetail(r.id));
    markerMap[r.id] = marker;
  });
}

// ---- Statistiken ----

function renderStats() {
  const row = document.getElementById('stats-row');
  if (!row) return;
  const all = getReports().filter(r => {
    if (!activeCategories.has(r.category)) return false;
    if (dateVon && r.createdAt.slice(0, 10) < dateVon) return false;
    if (dateBis && r.createdAt.slice(0, 10) > dateBis) return false;
    return true;
  });
  const counts = { new: 0, in_progress: 0, resolved: 0 };
  all.forEach(r => { if (r.status in counts) counts[r.status]++; });

  const cards = [
    { key: null,          label: 'Alle',          count: all.length },
    { key: 'new',         label: 'Neu',            count: counts.new },
    { key: 'in_progress', label: 'In Bearbeitung', count: counts.in_progress },
    { key: 'resolved',    label: 'Erledigt',       count: counts.resolved },
  ];

  row.innerHTML = cards.map(c => `
    <div class="stat-card${activeStatusFilter === c.key ? ' stat-card--active' : ''}" data-status="${c.key ?? ''}">
      <div class="stat-label">${c.label}</div>
      <div class="stat-value">${c.count}</div>
    </div>
  `).join('');

  row.querySelectorAll('.stat-card').forEach(card => {
    card.addEventListener('click', () => {
      const key = card.dataset.status || null;
      activeStatusFilter = activeStatusFilter === key ? null : key;
      renderStats();
      renderMarkers();
      renderList();
    });
  });
}

// ---- Liste ----

function renderList() {
  const list = document.getElementById('reports-list');
  if (!list) return;
  const filtered = getFiltered();
  if (filtered.length === 0) {
    list.innerHTML = '<li class="reports-empty">Keine Meldungen für den gewählten Filter.</li>';
    return;
  }
  list.innerHTML = filtered.map(r => `
    <li class="report-card" data-id="${r.id}">
      <div class="report-card-header">
        <span class="category-badge cat-${toCssClass(r.category)}">${r.category}</span>
        <span class="status-badge" style="background:${STATUS_COLORS[r.status]}">${STATUS_LABELS[r.status]}</span>
      </div>
      <h3 class="report-card-title">${r.title}</h3>
      ${r.description ? `<p class="report-card-desc">${r.description}</p>` : ''}
      <div class="report-card-meta">
        <div><span class="meta-label">Erfasst am</span>
             <span class="meta-value">${formatDate(r.createdAt)}</span></div>
        <div><span class="meta-label">Koordinaten</span>
             <span class="meta-value">${Number(r.latitude).toFixed(4)}, ${Number(r.longitude).toFixed(4)}</span></div>
      </div>
    </li>
  `).join('');
  list.querySelectorAll('.report-card').forEach(card => {
    card.addEventListener('click', () => showDetail(card.dataset.id));
  });
}

// ---- Detailansicht ----

function showDetail(reportId) {
  const report = getReports().find(r => r.id === reportId);
  if (!report) return;
  currentReportId = reportId;

  document.getElementById('view-list').style.display = 'none';
  document.getElementById('view-detail').style.display = 'block';

  document.getElementById('detail-title').textContent = report.title;

  document.getElementById('detail-meta').innerHTML = `
    <div class="detail-meta-grid">
      <div>
        <span class="meta-label">Kategorie</span>
        <span class="category-badge cat-${toCssClass(report.category)}">${report.category}</span>
      </div>
      <div>
        <span class="meta-label">Status</span>
        <span class="status-badge" style="background:${STATUS_COLORS[report.status]}">${STATUS_LABELS[report.status]}</span>
      </div>
      <div>
        <span class="meta-label">Erfasst am</span>
        <span class="meta-value">${formatDate(report.createdAt)}</span>
      </div>
      <div>
        <span class="meta-label">Koordinaten</span>
        <span class="meta-value">${Number(report.latitude).toFixed(5)}, ${Number(report.longitude).toFixed(5)}</span>
      </div>
    </div>
    ${report.description ? `<p class="detail-description">${report.description}</p>` : ''}
  `;

  document.getElementById('detail-photo').innerHTML = report.photo
    ? `<div class="photo-placeholder">Foto: ${report.photo}</div>`
    : `<div class="photo-placeholder">Kein Foto hinterlegt</div>`;

  document.getElementById('status-select').value = report.status;
  document.getElementById('comment-input').value = '';
  renderComments(report);

  // Detailkarte initialisieren (nach DOM-Update)
  setTimeout(() => {
    if (detailMap) { detailMap.remove(); detailMap = null; }
    const el = document.getElementById('detail-map');
    el.style.height = '200px';
    detailMap = L.map('detail-map').setView([report.latitude, report.longitude], 15);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(detailMap);
    L.marker([report.latitude, report.longitude]).addTo(detailMap);
  }, 50);

  // Hauptkarte zentrieren
  if (markerMap[reportId]) {
    map.setView([report.latitude, report.longitude], 15, { animate: true });
    markerMap[reportId].openPopup();
  }
}

function showList() {
  currentReportId = null;
  document.getElementById('view-detail').style.display = 'none';
  document.getElementById('view-list').style.display = 'block';
  if (detailMap) { detailMap.remove(); detailMap = null; }
}

// ---- Kommentare ----

function renderComments(report) {
  const comments = report.comments || [];
  const el = document.getElementById('comments-list');
  if (comments.length === 0) { el.innerHTML = ''; return; }
  el.innerHTML = `
    <div class="comments-section">
      <h3 class="comments-title">Notizen (${comments.length})</h3>
      ${comments.map(c => `
        <div class="comment-item">
          <div class="comment-meta">${formatDate(c.createdAt)}</div>
          <div class="comment-text">${c.text}</div>
        </div>
      `).join('')}
    </div>
  `;
}

// ---- Aktionen ----

function changeStatus(reportId, newStatus) {
  const reports = getReports();
  const r = reports.find(r => r.id === reportId);
  if (!r) return;
  r.status = newStatus;
  saveReports(reports);
  renderStats();
  renderList();
  renderMarkers();
  showDetail(reportId);
}

function addComment(reportId, text) {
  if (!text.trim()) return;
  const reports = getReports();
  const r = reports.find(r => r.id === reportId);
  if (!r) return;
  if (!r.comments) r.comments = [];
  r.comments.push({ text: text.trim(), createdAt: new Date().toISOString() });
  saveReports(reports);
  renderComments(r);
  document.getElementById('comment-input').value = '';
}

function deleteReport(reportId) {
  if (!confirm('Meldung wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.')) return;
  saveReports(getReports().filter(r => r.id !== reportId));
  showList();
  renderStats();
  renderList();
  renderMarkers();
}

// ---- Filter ----

function setupFilters() {
  document.querySelectorAll('#category-filter-group input[type="checkbox"]').forEach(cb => {
    cb.addEventListener('change', () => {
      cb.checked ? activeCategories.add(cb.value) : activeCategories.delete(cb.value);
      renderStats(); renderMarkers(); renderList();
    });
  });
  document.getElementById('date-von').addEventListener('change', e => {
    dateVon = e.target.value || null;
    renderStats(); renderMarkers(); renderList();
  });
  document.getElementById('date-bis').addEventListener('change', e => {
    dateBis = e.target.value || null;
    renderStats(); renderMarkers(); renderList();
  });
}

// ---- Init ----

document.addEventListener('DOMContentLoaded', () => {
  initMap();
  renderMarkers();
  renderStats();
  renderList();
  setupFilters();

  document.getElementById('btn-back').addEventListener('click', showList);

  document.getElementById('btn-status-save').addEventListener('click', () => {
    if (currentReportId) changeStatus(currentReportId, document.getElementById('status-select').value);
  });

  document.getElementById('btn-comment-add').addEventListener('click', () => {
    if (currentReportId) addComment(currentReportId, document.getElementById('comment-input').value);
  });

  document.getElementById('btn-delete').addEventListener('click', () => {
    if (currentReportId) deleteReport(currentReportId);
  });
});
