let selectedLat = null;
let selectedLng = null;
let map;
let marker = null;

function initMap() {
  map = L.map('map').setView([52.52, 13.405], 12);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(map);

  map.on('click', e => setLocation(e.latlng.lat, e.latlng.lng));
}

function setLocation(lat, lng) {
  selectedLat = lat;
  selectedLng = lng;

  if (marker) marker.remove();
  marker = L.marker([lat, lng]).addTo(map);

  document.getElementById('lat-display').textContent = lat.toFixed(5);
  document.getElementById('lng-display').textContent = lng.toFixed(5);
  document.getElementById('coord-display').style.display = 'grid';

  const hint = document.getElementById('location-hint');
  hint.textContent = 'Standort ausgewählt.';
  hint.classList.add('location-hint--ok');
}

function useGPS() {
  if (!navigator.geolocation) {
    showError('GPS wird von diesem Browser nicht unterstützt.');
    return;
  }
  navigator.geolocation.getCurrentPosition(
    pos => {
      setLocation(pos.coords.latitude, pos.coords.longitude);
      map.setView([pos.coords.latitude, pos.coords.longitude], 15);
    },
    () => showError('GPS-Standort konnte nicht ermittelt werden.')
  );
}

function setFieldError(inputId, errorId, msg) {
  const input = document.getElementById(inputId);
  const err = document.getElementById(errorId);
  if (msg) {
    if (input) input.classList.add('input-error');
    if (err) err.textContent = msg;
  } else {
    if (input) input.classList.remove('input-error');
    if (err) err.textContent = '';
  }
}

function clearFieldErrors() {
  ['input-title', 'input-category', 'input-description'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.classList.remove('input-error');
  });
  ['error-title', 'error-category', 'error-description'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.textContent = '';
  });
  const hint = document.getElementById('location-hint');
  if (hint) hint.classList.remove('location-hint--error');
}

function showError(msg) {
  const el = document.getElementById('form-error');
  if (el) { el.textContent = msg; el.style.display = 'block'; }
}

function hideError() {
  const el = document.getElementById('form-error');
  if (el) el.style.display = 'none';
}

function handleSubmit(e) {
  e.preventDefault();
  clearFieldErrors();

  const title       = document.getElementById('input-title').value.trim();
  const category    = document.getElementById('input-category').value;
  const description = document.getElementById('input-description').value.trim();
  const photoInput  = document.getElementById('input-photo');

  let valid = true;
  if (!title)       { setFieldError('input-title',    'error-title',    'Bitte gib einen Titel ein.');  valid = false; }
  if (!category)    { setFieldError('input-category', 'error-category', 'Bitte wähle eine Kategorie.'); valid = false; }
  if (selectedLat === null) {
    const hint = document.getElementById('location-hint');
    if (hint) { hint.textContent = 'Bitte wähle einen Standort auf der Karte aus.'; hint.classList.add('location-hint--error'); }
    valid = false;
  }
  if (!valid) return;

  const hasPhoto = photoInput.files.length > 0;

  const session = JSON.parse(sessionStorage.getItem('stadtmeldung_user') || 'null');
  console.log('[neue-meldung] Session beim Absenden:', session);

  const newReport = {
    id: 'user-' + Date.now(),
    title,
    category,
    description,
    latitude: selectedLat,
    longitude: selectedLng,
    status: 'new',
    photo: hasPhoto ? photoInput.files[0].name : '',
    submittedByMe: !!(session && session.role === 'user'),
    submittedBy: session ? session.username : null,
    createdAt: new Date().toISOString()
  };

  console.log('[neue-meldung] Neuer Report:', newReport);

  const stored = localStorage.getItem('stadtmeldung_reports');
  const reports = stored ? JSON.parse(stored) : [];
  reports.unshift(newReport);
  localStorage.setItem('stadtmeldung_reports', JSON.stringify(reports));
  console.log('[neue-meldung] Gespeichert, Anzahl Reports:', reports.length);

  window.location.href = (session && session.role === 'user') ? 'meine-meldungen.html' : 'index.html';
}

document.addEventListener('DOMContentLoaded', () => {
  initMap();

  document.getElementById('btn-gps').addEventListener('click', useGPS);
  document.getElementById('neue-meldung-form').addEventListener('submit', handleSubmit);

  document.getElementById('input-photo').addEventListener('change', e => {
    const file = e.target.files[0];
    const wrap = document.getElementById('photo-preview-wrap');
    const img = document.getElementById('photo-preview');
    if (file) {
      img.src = URL.createObjectURL(file);
      wrap.style.display = 'block';
    } else {
      wrap.style.display = 'none';
    }
  });
});
