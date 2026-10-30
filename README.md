# FixMyCity

Lokales Webportal zum Melden und Verwalten kommunaler Schäden. Die vorhandene HTML-/CSS-/JavaScript-Oberfläche spricht eine Java-17-API an. SQLite speichert Meldungen und Benutzer lokal auf demselben Computer.

## Voraussetzungen

- JDK 17 oder neuer
- Maven 3.9 oder neuer (getestet mit Maven 3.10)

## Start unter Windows

**Einmalige Ersteinrichtung:** Wenn die SQLite-Datenbank noch keinen Administrator enthält, legen Sie den ersten Admin über diese Variablen an. Verwenden Sie ein eigenes Passwort mit mindestens 12 Zeichen:

```powershell
cd "C:\Users\<Ihr Benutzername>\Desktop\Software_Engineering\stadtmeldung-portal"
$env:PORTAL_ADMIN_USERNAME = "admin"
$env:PORTAL_ADMIN_PASSWORD = "hier-ein-eigenes-starkes-Passwort-eintragen"
mvn test
mvn spring-boot:run
```

Danach `http://localhost:3000` im Browser öffnen und unter „Mitarbeiter der Stadt“ anmelden. Administratoren und Mitarbeitende verwenden dasselbe Dashboard; Administratoren erhalten dort zusätzlich den Bereich zur Verwaltung der Mitarbeiterkonten. Die Serveradresse ist auf Loopback beschränkt und somit nicht aus dem lokalen Netzwerk erreichbar.

**Normale Starts danach:** Sind die Admin-Variablen in diesem Terminal gesetzt, können sie entfernt werden. Danach benötigt der Start keine Konto-Variablen mehr:

```powershell
Remove-Item Env:PORTAL_ADMIN_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:PORTAL_ADMIN_PASSWORD -ErrorAction SilentlyContinue
mvn spring-boot:run
```

Der Admin erstellt und verwaltet Mitarbeiterkonten danach im Portal über **Mitarbeiter verwalten**. Mitarbeiter melden sich im Mitarbeiter-Bereich an und bearbeiten Meldungen im Dashboard. Bürgerkonten können weiterhin über „Registrieren“ angelegt werden.

Die Admin-Variablen sind nur für das einmalige Erstellen des ersten Administrators gedacht. Ist bereits mindestens ein Admin in der Datenbank vorhanden, startet das Backend auch ohne sie. Die Registrierungs-API vergibt ausschließlich die Bürgerrolle; nur ein angemeldeter Admin kann serverseitig Mitarbeiterkonten erstellen.

Falls PowerShell `mvn` nicht findet, obwohl Maven installiert ist, wurde das Terminal wahrscheinlich vor der Maven-Installation bzw. vor der PATH-Änderung geöffnet. Schließe alle VS-Code-Fenster und öffne VS Code erneut. Alternativ Maven direkt aufrufen:

```powershell
& "$HOME\Tools\apache-maven-3.10.0\bin\mvn.cmd" test
& "$HOME\Tools\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

In einer neu geöffneten PowerShell können Sie mit `mvn -version` prüfen, ob Maven im PATH gefunden wird.

## Funktionen

- Meldungen auf der Karte erfassen, mit Titel, Beschreibung, Position und optionalem Foto.
- Meldungen können ohne Konto anonym eingereicht werden; ein Bürgerkonto ist freiwillig und ermöglicht „Meine Meldungen“, Stimmabgaben und Updates.
- Bei „Defekt“ eine konkrete Defektart angeben: Schlagloch, Fahrbahnschaden, Gehweg/Bordstein, Straßenbeleuchtung, Verkehrsschild, Bank/Spielgerät oder anderer Defekt.
- Bürger können ihre eigenen Meldungen ansehen.
- Unter „Meine Meldungen“ kann der angemeldete Bürger zwischen eigenen Meldungen, Meldungen, für die er gestimmt hat, und Meldungen mit eigenen Updates filtern; die Art der Beteiligung ist zusätzlich an jeder Meldung gekennzeichnet.
- Bürger können für öffentliche Meldungen stimmen, ihre Stimme später zurücknehmen und zusätzliche Fortschrittsinformationen mit optionalem Foto veröffentlichen.
- Mitarbeiter sehen die Stimmenzahl und können Meldungen danach sortieren. Bürger sehen verständliche Statusangaben: wartet auf Bearbeitung durch die Stadt, in Bearbeitung durch die Stadt und von der Stadt erledigt. „Alle Meldungen“ ist ein separater Filter.
- Nicht gelöschte Meldungen erscheinen für Bürger und Mitarbeitende in der Karten- und Listenübersicht. Mitarbeitende und Administratoren können erledigte Meldungen löschen.
- Mitarbeitende und Administratoren können interne Kommentare zu Meldungen speichern. Diese Kommentare sind ausschließlich für das Stadt-Team sichtbar; Bürger-Updates bleiben öffentlich.
- Administratoren können Mitarbeiterkonten im Portal erstellen, anzeigen und löschen. Beim Löschen werden aktive Sitzungen des Mitarbeiters beendet.
- Mitarbeiter und Administratoren können Meldungsstatus ändern.
- Status- und Kategorieübersicht mit Karte und Filtern.
- Für Smartphones passt sich die Oberfläche an: Karte, Filter, Meldungsliste und Navigation werden einspaltig und touch-freundlich dargestellt.
- OSM-Kacheln werden über den dokumentierten Tile-Endpunkt mit sichtbarer Attribution und Referer-Policy geladen.
- BCrypt-Passworthashes, rollenbasierte Berechtigungen und serverseitige Sitzungen in HttpOnly-/SameSite-Cookies.

## Projektstruktur

- `src/main/java/com/stadtmeldung/StadtmeldungApplication.java` startet Spring Boot und initialisiert SQLite.
- `src/main/java/com/stadtmeldung/config/` enthält Spring-Konfiguration, Bean-Wiring, Sicherheit und das statische Serving-Verhalten.
- `src/main/java/com/stadtmeldung/controller/` enthält REST-Controller, nach Verantwortlichkeit getrennt: Anmeldung, Meldungen, Fortschrittsinformationen, Stimmen und Mitarbeiterkonten.
- `src/main/java/com/stadtmeldung/security/` enthält die Datenbank-Anbindung für Spring Security und die Verwaltung aktiver Sitzungen.
- Die bestehenden `model/`, `repository/` und `service/` Pakete bleiben die SQLite-Domänen- und Geschäftslogikschichten des Backend-Branches.
- `public/` ist wie im Frontend-Branch das öffentliche Stammverzeichnis für statische Dateien; die Oberfläche liegt weiterhin unter `public/frontend/` und ist unter `/frontend/` erreichbar. Der Spring-Webserver dient die Dateien nach demselben Verzeichnisprinzip wie Express aus `public/` aus. Die vorhandenen API-Pfade und JSON-Antworten bleiben für diese Oberfläche kompatibel.

## Daten und Konfiguration

- SQLite-Datenbank: `portal.db` im Projektordner
- Fotos: `uploads/`
- Optionaler Datenbankpfad: `PORTAL_DB_PATH`
- Optionaler Port: `PORTAL_PORT` (Standard `3000`)

Die Datenbank und Foto-Uploads werden nicht versioniert. Die Anwendung startet bewusst nur unter `127.0.0.1`. Der Browser sendet Zugangsdaten daher nicht über das LAN oder das Internet. Cookies und Tokens verschlüsseln den Transport nicht: Für einen Zugriff von anderen Geräten muss HTTPS vorgeschaltet und der Serverzugriff entsprechend abgesichert werden.

## Teamtest und Bereitstellung

Im aktuellen Entwicklungsstand ist die Website nur auf dem Computer erreichbar, auf dem der Java-Server läuft. Teammitglieder können sie daher noch nicht direkt von ihren eigenen Geräten öffnen. Für gemeinsame Tests sollte die Anwendung auf einem erreichbaren Testserver mit HTTPS bereitgestellt werden. Dabei müssen sowohl die SQLite-Datei als auch der Ordner `uploads/` dauerhaft gespeichert werden. Zusätzlich sind die Server-Bind-Adresse, der vom Hosting vorgegebene Port und das Sitzungscookie für HTTPS zu konfigurieren. Das ist gut machbar, aber mehr als nur die HTML-Seite hochzuladen. Bis dahin sollten keine echten personenbezogenen Daten oder wiederverwendeten Passwörter in Teamtests verwendet werden.

Die Karte verwendet den öffentlichen OpenStreetMap-Tile-Server gemäß dessen [Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/): Standard-URL, sichtbare Attribution und ein Referer bei Browseranfragen. Tile-Anfragen müssen im Browser normal zwischengespeichert werden; keine automatischen Tile-Downloads oder Cache-Bypässe hinzufügen.

## API

- `GET /api/health` – Backend-Status
- `POST /api/register` – Bürgerkonto erstellen (`username`, `password`)
- `POST /api/login` / `POST /api/logout` – Anmeldung und Abmeldung
- `GET /api/auth/csrf` – CSRF-Token für geschützte Änderungen
- `GET /api/session` – angemeldeten Benutzer prüfen
- `GET /api/admin/employees` – Mitarbeiter anzeigen (Admin)
- `POST /api/admin/employees` – Mitarbeiterkonto anlegen (Admin)
- `GET /api/reports` – Meldungen auflisten
- `GET /api/reports?mine=true` – eigene Meldungen auflisten
- `DELETE /api/admin/employees/:id` – Mitarbeiterkonto löschen (Admin)
- `POST /api/reports` – Meldung anlegen
- `POST /api/reports/:id/votes` – als Bürger einmal für eine Meldung stimmen
- `DELETE /api/reports/:id/votes` – eigene Stimme als Bürger zurücknehmen
- `GET /api/reports/:id/comments` – öffentliche Fortschrittsinformationen ansehen
- `POST /api/reports/:id/comments` – Fortschrittsinformation als Bürger ergänzen (`text` bis 1.000 Zeichen, optional `photoData`; Text oder Foto erforderlich)
- `POST /api/reports/:id/comments/internal` – internen Kommentar als Mitarbeiter/Admin ergänzen (`text`, nur für das Stadt-Team sichtbar)
- `PATCH /api/reports/:id` – Status ändern (Mitarbeiter/Admin)
- `DELETE /api/reports/:id` – erledigte Meldung löschen (Mitarbeiter/Admin)

## Qualitätssicherung

Automatisierte Backend-Funktionstests ausführen:

```powershell
mvn test
```

Die Tests prüfen unter anderem Benutzernamen-/Passwortvalidierung, Passwortprüfung, konkrete Defektarten, Koordinatenvalidierung, Admin-Berechtigung beim Erstellen und Löschen von Mitarbeiterkonten, Statusänderungen, Löschen erledigter Meldungen, interne Kommentare mit Rollenprüfung, Bürgerstimmen mit Zurücknehmen und die Rollen-/Inhaltsvalidierung bei Fortschrittsinformationen. Ergänzend sollten die Benutzerfreundlichkeit und die Oberfläche manuell geprüft werden: anonyme Meldung ohne Konto, erste Admin-Einrichtung, Mitarbeiterkonto erstellen/löschen, Mitarbeiteranmeldung, Mitarbeiter-Dashboard ohne Erfassungsfunktionen und Statuswechsel sowie Bürgerregistrierung und -login, Karte/GPS, Foto-Upload, Stimmen, Updates mit Foto, interne Team-Kommentare und „Meine Meldungen“.
