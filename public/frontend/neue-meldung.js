let selectedLat = null;
let selectedLng = null;
let map;
let marker = null;

function initMap() {
  map = L.map('map').setView([52.52, 13.405], 12);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
    referrerPolicy: 'strict-origin-when-cross-origin'
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

function showError(msg) {
  const el = document.getElementById('form-error');
  el.textContent = msg;
  el.style.display = 'block';
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function hideError() {
  document.getElementById('form-error').style.display = 'none';
}

function fileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error('Foto konnte nicht gelesen werden.'));
    reader.readAsDataURL(file);
  });
}

async function handleSubmit(e) {
  e.preventDefault();
  hideError();
  if (window.stadtmeldungSessionReady) {
    await window.stadtmeldungSessionReady;
  }

  const title = document.getElementById('input-title').value.trim();
  const category = document.getElementById('input-category').value;
  const description = document.getElementById('input-description').value.trim();
  const defectType = document.getElementById('input-defect-type').value;
  const photoInput = document.getElementById('input-photo');

  if (!title)       { showError('Bitte geben Sie einen Titel ein.'); return; }
  if (!category)    { showError('Bitte wählen Sie eine Kategorie.'); return; }
  if (category === 'Defekt' && !defectType) {
    showError('Bitte wählen Sie die konkrete Defektart aus.');
    return;
  }
  if (!description) { showError('Bitte geben Sie eine Beschreibung ein.'); return; }
  if (selectedLat === null) { showError('Bitte wählen Sie einen Standort auf der Karte aus.'); return; }

  const photo = photoInput.files[0];
  if (photo && photo.size > 5 * 1024 * 1024) {
    showError('Das Foto darf höchstens 5 MB groß sein.');
    return;
  }

  try {
    const response = await fetch('/api/reports', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title,
        category,
        defectType,
        description,
        latitude: selectedLat,
        longitude: selectedLng,
        photoData: photo ? await fileAsDataUrl(photo) : ''
      })
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || 'Meldung konnte nicht gespeichert werden.');
    window.location.href = window.stadtmeldungSession?.role === 'user'
      ? 'meine-meldungen.html'
      : 'index.html';
  } catch (error) {
    showError(error.message);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initMap();

  document.getElementById('btn-gps').addEventListener('click', useGPS);
  document.getElementById('neue-meldung-form').addEventListener('submit', handleSubmit);
  const categoryInput = document.getElementById('input-category');
  const defectTypeGroup = document.getElementById('defect-type-group');
  const defectTypeInput = document.getElementById('input-defect-type');
  categoryInput.addEventListener('change', () => {
    const isDefect = categoryInput.value === 'Defekt';
    defectTypeGroup.style.display = isDefect ? 'block' : 'none';
    defectTypeInput.required = isDefect;
    if (!isDefect) defectTypeInput.value = '';
  });

  document.getElementById('input-photo').addEventListener('change', e => {
    const file = e.target.files[0];
    if (file && file.size > 5 * 1024 * 1024) {
      showError('Das Foto darf höchstens 5 MB groß sein.');
      e.target.value = '';
      return;
    }
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
