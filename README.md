# Stadtmeldung Portal

Ein vollständiges Web-Portal zur Erfassung, Verwaltung und Bearbeitung von Bürgermeldungen für Städte und Gemeinden. Das Projekt erfüllt die im Praktikum beschriebenen Anforderungen für ein Stadt- und Gemeindemeldesystem mit Kartenposition, Textbeschreibung, optionalem Foto, Statusverwaltung und Behörden-UI.

## Funktionen

- Bürger können Defekte, Verunreinigungen und Parkverstöße melden.
- Meldungen können mit Ortsangabe auf einer Karte erfasst werden.
- Textuelle Beschreibung und optionales Foto sind möglich.
- Zuständige Mitarbeiter können Meldungen einsehen und Statusänderungen durchführen.
- Meldungen können als erledigt markiert oder gelöscht werden.
- Statusübersicht mit Filtern und Dashboard-Kartenansicht.
- Einfache Rollenlogin-Funktion für Bürger, Mitarbeiter und Administratoren.

## Technischer Stack

- Node.js + Express.js
- HTML + CSS + JavaScript
- Leaflet OpenStreetMap-Karte
- Multer für Foto-Uploads
- Lokale JSON-Datenbank für einfache Persistenz

## Projektstruktur

- `server.js` – Express-Backend und API
- `public/index.html` – Frontend-Layout
- `public/styles.css` – Stylesheet
- `public/app.js` – Frontend-Logik, Login und Karte
- `data/reports.json` – Beispielmeldungen und Persistenz
- `data/users.json` – Demo-Benutzer und Rollen
- `uploads/` – hochgeladene Bilder

## Demo-Benutzer

Im Projekt sind bereits Demo-Accounts eingerichtet:

- Administrator: Benutzername `admin`, Passwort `admin123`
- Mitarbeiter: Benutzername `mitarbeiter`, Passwort `mitarbeiter123`
- Bürger: Benutzername `buerger`, Passwort `buerger123`

> Nur angemeldete Mitarbeiter oder Administratoren können Meldungen verwalten, löschen und Statusänderungen vornehmen.

## Schnellstart auf Windows

1. Node.js installieren.
   - Download: https://nodejs.org/
   - Danach in einer PowerShell prüfen:
     ```powershell
     node -v
     npm -v
     ```

2. Im Projektordner öffnen:
   ```powershell
   cd "c:\Users\<dein Benutzername>\Desktop\Software_Engineering\stadtmeldung-portal"
   npm install
   npm start
   ```

3. Browser öffnen:
   ```text
   http://localhost:3000
   ```

4. Login mit einem der Demo-Accounts testen.

## Schnellstart auf macOS

1. Node.js installieren.
   - Empfehlung: `brew install node` oder Node.js-Installer von nodejs.org
   - Prüfen:
     ```bash
     node -v
     npm -v
     ```

2. In das Projektverzeichnis wechseln:
   ```bash
   cd ~/Desktop/Software_Engineering/stadtmeldung-portal
   npm install
   npm start
   ```

3. Browser öffnen:
   ```text
   http://localhost:3000
   ```

4. Login mit Demo-Account testen.

## API-Endpunkte

- `GET /api/health` – Statusprüfung
- `POST /api/login` – Login mit Benutzername und Passwort
- `GET /api/reports` – Liste der Meldungen
- `POST /api/reports` – Neue Meldung anlegen
- `PATCH /api/reports/:id` – Status aktualisieren
- `DELETE /api/reports/:id` – Meldung löschen

## Hinweise für die Bewertung

Die Anwendung demonstriert die wichtigsten Aspekte der Projektaufgabe:

- praktische Web-Anwendung
- Teamorientierte Softwareentwicklung
- übersichtliche Architektur
- Nutzung von Kartenmaterial und Datei-Uploads
- Zustandsverwaltung und einfache Qualitätssicherung
- Rollenbasierte Nutzung mit Login

## Beispiel für die Präsentation

Im Abschlussgespräch kann das Team die folgenden Punkte hervorheben:

- Bürgerfreundliche Erfassung von Meldungen
- Verwaltung mit Status-Workflow
- Nutzung von OpenStreetMap und Leaflet
- Rollenmodell für Bürger, Mitarbeiter und Verwaltung
- Einfache, aber skalierbare Architektur
- Produktionsnahe Umsetzung mit lokaler Persistenz
