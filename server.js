const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const { randomUUID } = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;
const dataDir = path.join(__dirname, 'data');
const uploadDir = path.join(__dirname, 'uploads');
const reportsFile = path.join(dataDir, 'reports.json');
const usersFile = path.join(dataDir, 'users.json');

const defaultUsers = [
  {
    id: 'admin-user',
    username: 'admin',
    password: 'admin123',
    role: 'admin',
    displayName: 'Stadtverwaltung'
  },
  {
    id: 'staff-user',
    username: 'mitarbeiter',
    password: 'mitarbeiter123',
    role: 'staff',
    displayName: 'Mitarbeiter'
  },
  {
    id: 'citizen-user',
    username: 'buerger',
    password: 'buerger123',
    role: 'citizen',
    displayName: 'Bürger'
  }
];

fs.mkdirSync(dataDir, { recursive: true });
fs.mkdirSync(uploadDir, { recursive: true });

function readJson(filePath, fallback) {
  try {
    const raw = fs.readFileSync(filePath, 'utf8');
    const parsed = JSON.parse(raw);
    return parsed;
  } catch (error) {
    return fallback;
  }
}

function writeJson(filePath, value) {
  fs.writeFileSync(filePath, JSON.stringify(value, null, 2));
}

function readReports() {
  const reports = readJson(reportsFile, []);
  return Array.isArray(reports) ? reports : [];
}

function writeReports(reports) {
  writeJson(reportsFile, reports);
}

function readUsers() {
  const users = readJson(usersFile, defaultUsers);
  return Array.isArray(users) && users.length ? users : defaultUsers;
}

function writeUsers(users) {
  writeJson(usersFile, users);
}

function removeReportPhoto(photoPath) {
  if (!photoPath) {
    return;
  }

  const absolutePath = path.join(__dirname, photoPath.replace(/^\/+/, ''));
  if (fs.existsSync(absolutePath)) {
    fs.unlinkSync(absolutePath);
  }
}

const storage = multer.diskStorage({
  destination: function (_req, _file, cb) {
    cb(null, uploadDir);
  },
  filename: function (_req, file, cb) {
    const extension = path.extname(file.originalname) || '.jpg';
    const safeName = `${Date.now()}-${randomUUID()}${extension}`;
    cb(null, safeName);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: function (_req, file, cb) {
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    if (allowed.includes(file.mimetype)) {
      cb(null, true);
      return;
    }

    cb(new Error('Nur Bilddateien im Format JPG, PNG, GIF oder WEBP sind erlaubt.'));
  }
});

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));
app.use('/uploads', express.static(uploadDir));
app.use(express.static(path.join(__dirname, 'public')));

app.get('/api/health', (_req, res) => {
  res.json({ status: 'ok', message: 'Stadtmeldung backend is running.' });
});

app.post('/api/login', (req, res) => {
  const username = String(req.body?.username || '').trim();
  const password = String(req.body?.password || '').trim();
  const user = readUsers().find((entry) => entry.username === username && entry.password === password);

  if (!user) {
    return res.status(401).json({ message: 'Benutzername oder Passwort ungültig.' });
  }

  const { password: _password, ...publicUser } = user;
  res.json({ user: publicUser });
});

app.get('/api/reports', (_req, res) => {
  const reports = readReports().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  res.json(reports);
});

app.post('/api/reports', upload.single('photo'), (req, res) => {
  const title = String(req.body.title || '').trim();
  const category = String(req.body.category || '').trim();
  const description = String(req.body.description || '').trim();
  const latitude = Number(req.body.latitude);
  const longitude = Number(req.body.longitude);

  if (!title || !category || !description || Number.isNaN(latitude) || Number.isNaN(longitude)) {
    if (req.file) {
      removeReportPhoto(`/uploads/${req.file.filename}`);
    }
    return res.status(400).json({
      message: 'Bitte füllen Sie alle Pflichtfelder aus und wählen Sie eine Position auf der Karte.'
    });
  }

  const newReport = {
    id: randomUUID(),
    title,
    category,
    description,
    latitude,
    longitude,
    status: 'new',
    photo: req.file ? `/uploads/${req.file.filename}` : '',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  const reports = readReports();
  reports.push(newReport);
  writeReports(reports);

  res.status(201).json({
    message: 'Meldung erfolgreich gespeichert.',
    report: newReport
  });
});

app.patch('/api/reports/:id', (req, res) => {
  const userRole = String(req.headers['x-user-role'] || '').trim();

  if (!['admin', 'staff'].includes(userRole)) {
    return res.status(403).json({ message: 'Nur Mitarbeiter oder Administratoren können Meldungen verwalten.' });
  }

  const { status } = req.body;
  const allowedStatuses = ['new', 'in_progress', 'resolved'];

  if (!allowedStatuses.includes(status)) {
    return res.status(400).json({ message: 'Ungültiger Status.' });
  }

  const reports = readReports();
  const report = reports.find((entry) => entry.id === req.params.id);

  if (!report) {
    return res.status(404).json({ message: 'Meldung nicht gefunden.' });
  }

  report.status = status;
  report.updatedAt = new Date().toISOString();
  writeReports(reports);

  res.json({ message: 'Status aktualisiert.', report });
});

app.delete('/api/reports/:id', (req, res) => {
  const userRole = String(req.headers['x-user-role'] || '').trim();

  if (!['admin', 'staff'].includes(userRole)) {
    return res.status(403).json({ message: 'Nur Mitarbeiter oder Administratoren können Meldungen löschen.' });
  }

  const reports = readReports();
  const index = reports.findIndex((report) => report.id === req.params.id);

  if (index === -1) {
    return res.status(404).json({ message: 'Meldung nicht gefunden.' });
  }

  const removed = reports[index];
  reports.splice(index, 1);
  writeReports(reports);

  if (removed.photo) {
    removeReportPhoto(removed.photo);
  }

  res.json({ message: 'Meldung gelöscht.', id: removed.id });
});

app.use((error, _req, res, _next) => {
  if (error instanceof multer.MulterError) {
    res.status(400).json({ message: error.message });
    return;
  }

  res.status(500).json({ message: error.message || 'Ein Serverfehler ist aufgetreten.' });
});

app.use((req, res) => {
  if (req.path.startsWith('/api/')) {
    return res.status(404).json({ message: 'API endpoint not found.' });
  }

  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

writeJson(usersFile, readUsers());

app.listen(PORT, () => {
  console.log(`Stadtmeldung server running at http://localhost:${PORT}`);
});
