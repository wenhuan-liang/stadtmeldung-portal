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
const CITIZEN_STATUS_LABELS = {
  new: 'Wartet auf Bearbeitung durch die Stadt',
  in_progress: 'In Bearbeitung durch die Stadt',
  resolved: 'Von der Stadt erledigt'
};

const STATUS_COLORS = {
  new: '#E0822A',
  in_progress: '#3A88C4',
  resolved: '#4AA868'
};

const ALL_CATEGORIES = ['Defekt', 'Verunreinigung', 'Parkvergehen', 'Sonstiges'];
const DEFECT_TYPE_LABELS = {
  POTHOLE: 'Schlagloch / abgesackte Fahrbahn',
  ROAD_SURFACE: 'Risse oder Abplatzungen in der Fahrbahn',
  SIDEWALK: 'Beschädigter Gehweg oder Bordstein',
  STREETLIGHT: 'Defekte Straßenbeleuchtung',
  TRAFFIC_SIGN: 'Beschädigtes oder fehlendes Verkehrsschild',
  BENCH_OR_PLAYGROUND: 'Beschädigte Bank oder Spielgerät',
  OTHER_DEFECT: 'Anderer Defekt'
};

let activeStatusFilter = null;
let activeParticipationFilter = 'all';
let reportSort = 'newest';
let currentReports = [];
const commentCache = new Map();

let map;
let markerMap = {};
let activeCategories = new Set(ALL_CATEGORIES);

function getPageReports() {
  return currentReports;
}

function escapeHtml(value = '') {
  return String(value).replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
}

function fileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error('Foto konnte nicht gelesen werden.'));
    reader.readAsDataURL(file);
  });
}

async function loadReports() {
  const isMine = document.body.dataset.page === 'meine-meldungen';
  const response = await fetch(isMine
    ? '/api/reports?mine=true'
    : '/api/reports');
  const body = await response.json();
  if (!response.ok) throw new Error(body.message || 'Meldungen konnten nicht geladen werden.');
  currentReports = body;
  renderStats();
  renderMarkers();
  renderList();
}

function toCssClass(category) {
  return category.toLowerCase()
    .replace(/ß/g, 'ss').replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue')
    .replace(/\s+/g, '-');
}

function initMap() {
  map = L.map('map').setView([52.52, 13.405], 12);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
    referrerPolicy: 'strict-origin-when-cross-origin'
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
        `<strong>${escapeHtml(report.title)}</strong><br>` +
        `<span style="color:${color}">${escapeHtml(report.category)}</span> &mdash; ` +
        `<em>${currentStatusLabels()[report.status]}</em>`
      );

      markerMap[report.id] = marker;
    });
}

function currentStatusLabels() {
  return ['staff', 'admin'].includes(window.stadtmeldungSession?.role)
    ? STATUS_LABELS
    : CITIZEN_STATUS_LABELS;
}

function renderStats() {
  const statsRow = document.getElementById('stats-row');
  if (!statsRow) return;

  const all = getPageReports().filter(r => activeCategories.has(r.category));
  const counts = { new: 0, in_progress: 0, resolved: 0 };
  all.forEach(r => { if (r.status in counts) counts[r.status]++; });

  const isCitizen = !['staff', 'admin'].includes(
    window.stadtmeldungSession?.role);
  const cards = [
    { key: 'new', label: isCitizen ? CITIZEN_STATUS_LABELS.new : 'Neu', count: counts.new },
    { key: 'in_progress', label: isCitizen ? CITIZEN_STATUS_LABELS.in_progress : 'In Bearbeitung', count: counts.in_progress },
    { key: 'resolved', label: isCitizen ? CITIZEN_STATUS_LABELS.resolved : 'Erledigt', count: counts.resolved },
    { key: null, label: 'Alle Meldungen', count: all.length }
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
      activeStatusFilter = key;
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
  if (document.body.dataset.page === 'meine-meldungen' &&
      activeParticipationFilter !== 'all') {
    const participationKey = {
      submitted: 'submittedByMe',
      voted: 'votedByMe',
      updated: 'updatedByMe'
    }[activeParticipationFilter];
    filtered = filtered.filter(report => report[participationKey]);
  }
  if (activeStatusFilter) {
    filtered = filtered.filter(r => r.status === activeStatusFilter);
  }
  filtered.sort((first, second) => {
    if (reportSort === 'votes') return second.voteCount - first.voteCount;
    if (reportSort === 'votes-ascending') return first.voteCount - second.voteCount;
    return new Date(second.createdAt) - new Date(first.createdAt);
  });

  if (filtered.length === 0) {
    const isMeinePage = document.body.dataset.page === 'meine-meldungen';
    const emptyMessages = {
      submitted: 'Sie haben noch keine Meldung selbst eingereicht.',
      voted: 'Sie haben noch für keine Meldung abgestimmt.',
      updated: 'Sie haben noch keine Meldung ergänzt.'
    };
    list.innerHTML = `<li class="reports-empty">${
      isMeinePage
      ? emptyMessages[activeParticipationFilter] ||
        'Sie haben noch keine Meldungen eingereicht, für Meldungen abgestimmt oder Meldungen ergänzt.'
        : 'Keine Meldungen für den gewählten Filter.'
    }</li>`;
    return;
  }

  const isCitizen = window.stadtmeldungSession?.role === 'user';
  const statusLabels = currentStatusLabels();
  list.innerHTML = filtered.map(r => `
    <li class="report-card" data-id="${escapeHtml(r.id)}">
      <div class="report-card-header">
        <span class="category-badge cat-${toCssClass(r.category)}">${escapeHtml(r.category)}</span>
        <span class="status-badge" style="background: ${STATUS_COLORS[r.status]}">${statusLabels[r.status]}</span>
      </div>
      ${document.body.dataset.page === 'meine-meldungen' ? `
        <div class="report-participation">
          ${r.submittedByMe ? '<span>Von mir gemeldet</span>' : ''}
          ${r.votedByMe ? '<span>Von mir abgestimmt</span>' : ''}
          ${r.updatedByMe ? '<span>Von mir ergänzt</span>' : ''}
        </div>
      ` : ''}
      <h3 class="report-card-title">${escapeHtml(r.title)}</h3>
      ${r.defectType ? `<p class="report-card-defect"><strong>Defektart:</strong> ${escapeHtml(DEFECT_TYPE_LABELS[r.defectType] || 'Anderer Defekt')}</p>` : ''}
      <p class="report-card-desc">${escapeHtml(r.description)}</p>
      <div class="photo-placeholder">${r.photo ? `<a href="${escapeHtml(r.photo)}" target="_blank" rel="noopener">Foto ansehen</a>` : 'Kein Foto hinterlegt'}</div>
      <div class="report-card-meta">
        <div>
          <span class="meta-label">Koordinaten</span>
          <span class="meta-value">${Number(r.latitude).toFixed(5)}, ${Number(r.longitude).toFixed(5)}</span>
        </div>
        <div>
          <span class="meta-label">Erfasst am</span>
          <span class="meta-value">${escapeHtml(formatDate(r.createdAt))}</span>
        </div>
      </div>
      <div class="report-community">
        <span class="vote-count" aria-label="${r.voteCount} ${r.voteCount === 1 ? 'Stimme' : 'Stimmen'}">👍 ${r.voteCount} ${r.voteCount === 1 ? 'Stimme' : 'Stimmen'}</span>
        ${window.stadtmeldungSession?.role === 'user' ? `<button type="button" class="community-button vote-button" data-action="vote" data-id="${escapeHtml(r.id)}">${r.hasVoted ? 'Stimme zurücknehmen' : 'Für diese Meldung stimmen'}</button>` : ''}
        ${!window.stadtmeldungSession ? `<a class="community-button" href="login.html#user">Anmelden, um abzustimmen</a>` : ''}
        ${window.stadtmeldungSession?.role === 'user' ? `<button type="button" class="community-button comments-toggle" data-action="add-update" data-id="${escapeHtml(r.id)}">Update ergänzen</button>` : ''}
      </div>
      <section class="report-comments" id="comments-${escapeHtml(r.id)}" data-report-id="${escapeHtml(r.id)}" aria-label="Updates zur Meldung"></section>
      ${['staff', 'admin'].includes(window.stadtmeldungSession?.role) ? `
        <label class="status-edit">Status
          <select class="form-input report-status" data-id="${escapeHtml(r.id)}">
            ${Object.entries(STATUS_LABELS).map(([key, label]) => `
              <option value="${key}"${r.status === key ? ' selected' : ''}>${label}</option>
            `).join('')}
          </select>
        </label>
      ` : ''}
    </li>
  `).join('');
  list.querySelectorAll('.report-comments').forEach(panel => {
    renderReportComments(panel.dataset.reportId, panel)
      .catch(error => {
        console.error(error);
        panel.textContent = 'Updates konnten nicht geladen werden.';
      });
  });
}

function formatDate(iso) {
  const date = parseDate(iso);
  return date ? date.toLocaleDateString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }) : 'Datum unbekannt';
}

function formatDateTime(iso) {
  const date = parseDate(iso);
  return date ? date.toLocaleString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }) : 'Datum unbekannt';
}

function parseDate(value) {
  const date = new Date(String(value || '').replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
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

function setupParticipationFilter() {
  const filter = document.getElementById('participation-filter');
  if (!filter) return;

  filter.addEventListener('change', () => {
    activeParticipationFilter = filter.value;
    renderList();
  });
}

function setupListClick() {
  const list = document.getElementById('reports-list');
  if (!list) return;

  list.addEventListener('click', e => {
    if (e.target.closest('button, a, input, select, textarea, form')) return;
    const item = e.target.closest('.report-card');
    if (!item) return;

    const report = getPageReports().find(
      r => String(r.id) === item.dataset.id
    );
    const marker = markerMap[item.dataset.id];
    if (report && marker) {
      map.setView([report.latitude, report.longitude], 15, { animate: true });
      marker.openPopup();
    }
  });
}

async function updateReportStatus(id, status) {
  const response = await fetch(`/api/reports/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  });
  const body = await response.json();
  if (!response.ok) throw new Error(body.message || 'Status konnte nicht gespeichert werden.');
  const index = currentReports.findIndex(report => String(report.id) === String(id));
  if (index !== -1) currentReports[index] = body.report;
  renderStats();
  renderMarkers();
  renderList();
}

async function toggleReportVote(id, button, currentlyVoted) {
  button.disabled = true;
  try {
    const response = await fetch(`/api/reports/${encodeURIComponent(id)}/votes`, {
      method: currentlyVoted ? 'DELETE' : 'POST'
    });
    const body = await response.json();
    if (!response.ok) {
      throw new Error(body.message || 'Stimme konnte nicht geändert werden.');
    }
    const report = currentReports.find(item => String(item.id) === String(id));
    if (report) {
      report.voteCount = body.voteCount;
      report.hasVoted = body.hasVoted;
    }
    renderStats();
    renderMarkers();
    renderList();
  } catch (error) {
    button.disabled = false;
    throw error;
  }
}

async function renderReportComments(id, panel, showForm = false) {
  let comments = commentCache.get(String(id));
  if (!comments) {
    const response = await fetch(`/api/reports/${encodeURIComponent(id)}/comments`);
    const body = await response.json();
    if (!response.ok) throw new Error(body.message || 'Informationen konnten nicht geladen werden.');
    comments = body;
    commentCache.set(String(id), comments);
  }

  panel.replaceChildren();
  const heading = document.createElement('h4');
  heading.textContent = 'Aktuelle Updates zur Meldung';
  panel.append(heading);
  if (comments.length === 0) {
    const empty = document.createElement('p');
    empty.className = 'comments-empty';
    empty.textContent = 'Noch keine zusätzlichen Informationen.';
    panel.append(empty);
  } else {
    const list = document.createElement('ul');
    list.className = 'comments-list';
    comments.forEach(comment => {
      const item = document.createElement('li');
      const author = document.createElement('strong');
      author.textContent = comment.username;
      const date = document.createElement('time');
      date.textContent = formatDateTime(comment.createdAt);
      item.append(author, date);
      if (comment.text) {
        const text = document.createElement('p');
        text.textContent = comment.text;
        item.append(text);
      }
      if (comment.photo) {
        const photoLink = document.createElement('a');
        photoLink.href = comment.photo;
        photoLink.target = '_blank';
        photoLink.rel = 'noopener';
        photoLink.textContent = 'Foto öffnen';
        const photo = document.createElement('img');
        photo.src = comment.photo;
        photo.alt = 'Foto zu dieser Fortschrittsinformation';
        photo.className = 'comment-photo';
        item.append(photoLink, photo);
      }
      list.append(item);
    });
    panel.append(list);
  }

  if (showForm && window.stadtmeldungSession?.role === 'user') {
    const form = document.createElement('form');
    form.className = 'comment-form';
    form.dataset.reportId = String(id);
    const input = document.createElement('textarea');
    input.className = 'form-input form-textarea';
    input.name = 'text';
    input.maxLength = 1000;
    input.placeholder = 'Was hat sich seit der Meldung verändert? (optional, wenn Sie ein Foto hinzufügen)';
    input.required = false;
    const photoLabel = document.createElement('label');
    photoLabel.className = 'comment-photo-label';
    photoLabel.textContent = 'Foto hinzufügen (optional, max. 5 MB)';
    const photoInput = document.createElement('input');
    photoInput.type = 'file';
    photoInput.name = 'photo';
    photoInput.accept = 'image/jpeg,image/png,image/webp,image/gif';
    photoInput.className = 'form-input form-file';
    photoLabel.append(photoInput);
    const button = document.createElement('button');
    button.className = 'community-button';
    button.type = 'submit';
    button.textContent = 'Update teilen';
    form.append(input, photoLabel, button);
    panel.append(form);
  }
}

async function submitReportComment(form) {
  const id = form.dataset.reportId;
  const text = form.elements.text.value.trim();
  const photo = form.elements.photo.files[0];
  if (!text && !photo) {
    throw new Error('Bitte geben Sie einen Text ein oder fügen Sie ein Foto hinzu.');
  }
  if (photo && photo.size > 5 * 1024 * 1024) {
    throw new Error('Das Foto darf höchstens 5 MB groß sein.');
  }
  const submitButton = form.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  try {
    const response = await fetch(`/api/reports/${encodeURIComponent(id)}/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text,
        photoData: photo ? await fileAsDataUrl(photo) : ''
      })
    });
    const body = await response.json();
    if (!response.ok) {
      throw new Error(body.message || 'Update konnte nicht gespeichert werden.');
    }
    commentCache.delete(String(id));
    await renderReportComments(id, form.closest('.report-comments'));
  } catch (error) {
    submitButton.disabled = false;
    throw error;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  if (document.getElementById('map')) {
    initMap();
    renderMarkers();
  }
  setupFilters();
  setupParticipationFilter();
  setupListClick();
  Promise.resolve(window.stadtmeldungSessionReady).then(() => {
    if (!['staff', 'admin'].includes(window.stadtmeldungSession?.role)) {
      document.body.classList.add('role-citizen');
    }
    if (window.stadtmeldungSession?.role === 'admin') {
      return;
    }
    return loadReports();
  }).catch(error => {
    console.error(error);
    const list = document.getElementById('reports-list');
    if (list) list.innerHTML = `<li class="reports-empty">${escapeHtml(error.message)}</li>`;
  });

  const list = document.getElementById('reports-list');
  if (list) {
    list.addEventListener('click', event => {
      const button = event.target.closest('[data-action]');
      if (!button) return;
      const { id, action } = button.dataset;
      if (action === 'vote') {
        const report = currentReports.find(item => String(item.id) === String(id));
        toggleReportVote(id, button, Boolean(report?.hasVoted)).catch(error => {
          window.alert(error.message);
        });
      } else if (action === 'add-update') {
        const panel = document.getElementById(`comments-${id}`);
        const existingForm = panel.querySelector('.comment-form');
        if (existingForm) {
          existingForm.remove();
          return;
        }
        renderReportComments(id, panel, true)
          .catch(error => window.alert(error.message));
      }
    });
    list.addEventListener('submit', event => {
      if (!event.target.matches('.comment-form')) return;
      event.preventDefault();
      submitReportComment(event.target).catch(error => window.alert(error.message));
    });
    list.addEventListener('change', event => {
      if (!event.target.matches('.report-status')) return;
      updateReportStatus(event.target.dataset.id, event.target.value)
        .catch(error => {
          window.alert(error.message);
          loadReports().catch(loadError => console.error(loadError));
        });
    });
  }

  const sortSelect = document.getElementById('report-sort');
  if (sortSelect) {
    sortSelect.addEventListener('change', () => {
      reportSort = sortSelect.value;
      renderList();
    });
  }

  const fabBtn = document.getElementById('btn-neue-meldung');
  if (fabBtn) {
    fabBtn.addEventListener('click', () => {
      window.location.href = 'neue-meldung.html';
    });
  }
});
