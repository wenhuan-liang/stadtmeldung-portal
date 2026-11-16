# Abschlussbericht: FixMyCity

## 1. Einleitung

Das Projekt „FixMyCity“ wurde im Rahmen der Vorlesung Software Engineering entwickelt. Ziel war die Umsetzung einer praxisnahen Applikation, mit der Bürger Probleme in ihrer Stadt oder Gemeinde digital melden können. Die Software unterstützt dabei die Erfassung von Defekten, Verunreinigungen und optionalen Parkverstößen mit geografischer Positionierung, Beschreibung und optionalem Foto.

Das Projekt wurde als Webapplikation realisiert, da diese Lösung die schnellste und robusteste Umsetzung bei begrenzter Entwicklungszeit erlaubt. Sie eignet sich für reale Einsatzszenarien in kommunalen Behörden und kombiniert Nutzerfreundlichkeit mit klarer Zustandsverwaltung.

## 2. Problemstellung und Anforderungen

Die Aufgabenstellung beschreibt drei zentrale Meldungskategorien:

- Defekte und Schäden, zum Beispiel kaputte Straßenbeleuchtung oder Schlaglöcher
- Verunreinigungen, zum Beispiel überquellende Mülleimer oder wild abgeladener Müll
- optionale Parkverstöße, zum Beispiel Parken auf Radwegen

Für jede Meldung müssen Angaben zur Position, eine textuelle Beschreibung und optional ein Foto hinterlegt werden können. Zuständige Mitarbeiter der Stadt sollen in der Lage sein, die Meldungen einzusehen, zu bearbeiten und nach Erledigung zu löschen.

Die Anwendung erfüllt diese Anforderungen durch eine zentrale Weboberfläche, in der Meldungen erfasst und anhand ihrer Kategorie sowie ihres Status verwaltet werden. Bei Defekten wird zusätzlich eine konkrete Unterkategorie ausgewählt, beispielsweise Schlagloch, Fahrbahnschaden oder defekte Straßenbeleuchtung.

## 3. Anforderungen an Funktionalität und Benutzerinteraktion

Die Software umfasst die folgenden Kernfunktionalitäten:

1. Erfassung neuer Meldungen
   - Titel
   - Kategorie
   - Beschreibung
   - Standort auf einer Karte
   - optionales Foto
   - anonyme Erfassung ohne verpflichtendes Bürgerkonto

2. Verwaltung der Meldungen
   - Anzeige aller Meldungen
   - Filterung nach Kategorie und konkreter Defektart
   - Statusverwaltung: Neu, In Bearbeitung, Erledigt
   - Mitarbeitende und Administratoren können erledigte Meldungen manuell löschen. Nicht vorher gelöschte Meldungen werden sieben Tage nach der Erledigung automatisch entfernt.

3. Visualisierung
   - Interaktive OpenStreetMap-Karte via Leaflet
   - Einfache, verständliche Darstellung der Meldungen

4. Backend-API
   - HTTP-Endpunkte für Healthcheck, Anmeldung, Meldungen und Statusänderungen
   - SQLite als lokale, relationale Datenhaltung
5. Bürgerbeteiligung
   - Angemeldete Bürger können für vorhandene Meldungen stimmen und ihre Stimme zurücknehmen.
   - Bürger können ergänzende Informationen und optionale Fotos zum aktuellen Stand einer Meldung teilen.
   - Mitarbeitende können Meldungen nach der Anzahl der Stimmen sortieren.
6. Rollen und interne Zusammenarbeit
   - Anonyme Besucher können Meldungen ohne Konto einreichen. Bürgerkonten ermöglichen die Verwaltung eigener Meldungen und Beteiligungen.
   - Administratoren verwalten Mitarbeiterkonten; Mitarbeitende und Administratoren können Status ändern und interne, nur für das Stadt-Team sichtbare Kommentare hinterlegen.

## 4. Softwarearchitektur

### 4.1 Architekturüberblick

FixMyCity ist als lokale Webanwendung mit einer geschichteten Client-Server-Architektur umgesetzt. Der Browser stellt die Benutzeroberfläche dar und kommuniziert über eine REST-API mit dem Java-Backend. Dieses verarbeitet Anfragen, setzt die fachlichen Regeln um und greift für dauerhafte Daten auf eine relationale SQLite-Datenbank sowie für Bilddateien auf das lokale Dateisystem zu. Die Trennung in Präsentation, API, Geschäftslogik und Datenzugriff erleichtert die Wartung: Änderungen an der Oberfläche müssen beispielsweise nicht unmittelbar Änderungen am Datenbankschema nach sich ziehen.

Das Backend basiert auf Java 17 und Spring Boot. Es stellt sowohl die API als auch die statischen Dateien der Oberfläche bereit. Die API ist unter `/api` erreichbar; die Frontend-Dateien liegen im Verzeichnis `public/` und insbesondere unter `public/frontend/`. Standardmäßig bindet der Server an `127.0.0.1` und Port `3000`. Frontend und Backend laufen dadurch auf demselben Computer und verwenden dieselbe Herkunft (Origin). Ein separates Frontend-Webserver- oder Node.js-Backend ist für den normalen Betrieb nicht erforderlich. Die Datenbank und die hochgeladenen Bilder bleiben lokal. Für den Zugriff über das lokale Netzwerk müsste die Serverbindung angepasst und eine geeignete HTTPS-Konfiguration eingerichtet werden.

Die Kartenansicht verwendet Leaflet im Browser. Kartendaten in Form von Kartenkacheln werden vom OpenStreetMap-Tile-Server bezogen. Diese Kartenkacheln laufen nicht durch die REST-API und werden nicht in der SQLite-Datenbank gespeichert; dafür benötigt der Browser eine Internetverbindung.

### 4.2 Komponenten und ihre Aufgaben

#### Frontend

Das Frontend besteht aus HTML-, CSS- und JavaScript-Dateien. Die Seiten ermöglichen das Ansehen und Filtern von Meldungen, das Anmelden beziehungsweise Registrieren, das Erstellen einer Meldung und – abhängig von der Rolle – die Verwaltung von Meldungen oder Mitarbeiterkonten. Leaflet visualisiert die Meldungen und unterstützt die Auswahl eines Standorts. Die JavaScript-Komponenten senden HTTP-Anfragen an die REST-API und stellen die empfangenen JSON-Daten in der Oberfläche dar.

Die Oberfläche unterscheidet anonyme Besucher, Bürger, Mitarbeitende und Administratoren. Anonyme Besucher können Meldungen einreichen, ohne ein Konto anzulegen. Bürger können zusätzlich ihre eigenen Meldungen und Beteiligungen ansehen, für Meldungen stimmen oder öffentliche Updates ergänzen. Mitarbeitende bearbeiten den Status und können interne Notizen hinzufügen. Administratoren verwalten Mitarbeiterkonten. Die Rollen dienen dabei nicht nur der Darstellung unterschiedlicher Funktionen: Die tatsächlichen Berechtigungen werden zusätzlich im Backend geprüft.

#### REST-API und Spring-Konfiguration

Die Controller bilden die Schnittstelle zwischen HTTP-Anfragen und der Geschäftslogik. Der `AuthController` verarbeitet Anmeldung, Abmeldung, Registrierung und Sitzungsinformationen. Der `ReportController` stellt die wesentlichen Endpunkte zum Abrufen, Erstellen, Ändern des Status und – für erledigte Meldungen – Löschen bereit. Weitere Controller behandeln Kommentare, Stimmen, Anhänge, Mitarbeiterverwaltung und den Healthcheck. Der `ApiResponseMapper` formt interne Objekte in die JSON-Struktur für das Frontend um. Der `AuthenticatedUserResolver` ermittelt den angemeldeten Benutzer aus dem serverseitigen Sicherheitskontext. Ein zentraler Fehlerbehandler gibt API-Fehler als HTTP-Status und JSON-Antwort zurück.

Die Spring-Konfiguration verbindet Controller, Services, Repositories und Datenbankzugriff. Dadurch werden Abhängigkeiten an einer Stelle bereitgestellt und Komponenten können unabhängig getestet werden. Die Web-Konfiguration liefert sowohl die Frontend-Dateien als auch Bilder aus `uploads/` aus.

#### Geschäftslogik und Datenzugriff

Die Services kapseln fachliche Abläufe und Regeln. Beispiele sind die Validierung neuer Meldungen, die Änderung des Meldungsstatus, die Prüfung von Rollen und Eigentümerschaft, das Hinzufügen von Kommentaren sowie die Verwaltung von Stimmen. Die Repository-Schnittstellen beschreiben, welche Datenoperationen benötigt werden. Ihre SQLite-Implementierungen führen die SQL-Abfragen aus und verwenden Result-Set-Mapper, um Datenbankzeilen in Java-Objekte umzuwandeln. Diese Aufteilung hält fachliche Entscheidungen von den konkreten SQL-Abfragen getrennt und unterstützt gezielte automatisierte Tests der Service-Funktionen.

### 4.3 Datenfluss

Der Ablauf beim Einreichen einer Meldung veranschaulicht das Zusammenspiel der Komponenten:

1. Im Browser gibt die meldende Person Titel, Kategorie, Beschreibung und Standort an. Bei einer Defektmeldung wird zusätzlich die konkrete Defektart ausgewählt. Ein Foto ist optional.
2. Das Frontend sendet die Angaben als JSON-Anfrage an `POST /api/reports`. Die Koordinaten werden zusammen mit den übrigen Meldungsdaten übertragen. Da anonyme Meldungen erlaubt sind, kann die Anfrage ohne angemeldeten Benutzer erfolgen.
3. Der `ReportController` prüft die Anfrage und ermittelt – sofern vorhanden – den angemeldeten Benutzer. Mitarbeitende dürfen keine Bürger-Meldungen einreichen. Ein übermitteltes Foto wird vom `ImageDataService` geprüft und unter einem zufälligen Dateinamen im Verzeichnis `uploads/` gespeichert; in der Datenbank wird dazu nur der Pfad abgelegt.
4. Der `ReportService` prüft die fachlichen Eingaben und übergibt die Meldung an das `ReportRepository`. Dieses speichert sie über JDBC in SQLite. Schlägt das Speichern nach dem Foto-Upload fehl, wird die bereits gespeicherte Bilddatei wieder entfernt.
5. Nach erfolgreichem Speichern wandelt der Response-Mapper das Ergebnis in JSON um. Der Controller sendet die Antwort an den Browser zurück; das Frontend aktualisiert anschließend die Anzeige.

Ein ähnliches Prinzip gilt für die Anmeldung: Die Zugangsdaten werden an die API übertragen und serverseitig geprüft. Passwörter werden nicht im Klartext in der Datenbank abgelegt, sondern mit BCrypt gehasht. Nach erfolgreicher Anmeldung verwendet Spring Security eine serverseitige Sitzung, die über ein Sitzungscookie dem Browser zugeordnet wird. Schreibende Anfragen werden zusätzlich durch CSRF-Schutz abgesichert.

Bei weiteren Interaktionen greifen die Rollenregeln entsprechend: Bürgerstimmen und öffentliche Updates setzen ein Bürgerkonto voraus; interne Kommentare und Statusänderungen sind Mitarbeitenden und Administratoren vorbehalten. Wenn Mitarbeitende oder Administratoren eine Meldung als „Erledigt“ markieren, weist die Oberfläche darauf hin, dass die Meldung manuell gelöscht werden kann. Erfolgt keine manuelle Löschung, entfernt das System die Meldung nach sieben Tagen. Die Prüfung erfolgt stündlich, solange das Portal läuft; nach einer Betriebspause wird die Bereinigung beim nächsten Start nachgeholt. Eine manuelle Löschung ist ebenfalls nur für erledigte Meldungen möglich. Dadurch werden Berechtigungs- und Zustandsregeln nicht allein dem Frontend überlassen.

### 4.4 Datenbank und Dateispeicherung

SQLite wurde gewählt, weil die Anwendung lokal auf einem einzelnen Computer betrieben wird und dafür kein separater Datenbankserver erforderlich ist. Standardmäßig liegt die Datenbank in `portal.db`; der Speicherort kann über die Umgebungsvariable `PORTAL_DB_PATH` festgelegt werden. Die Datenbankinitialisierung legt die benötigten Tabellen an und ergänzt bei bestehenden Datenbanken fehlende Spalten. Fremdschlüssel werden für Datenbankverbindungen aktiviert.

Das Schema umfasst fünf zentrale Tabellen:

- `users` speichert Benutzername, Name, E-Mail, BCrypt-Passworthash, Rolle und Erstellungszeitpunkt. Die Rollen sind Bürger, Mitarbeitende und Administratoren.
- `reports` enthält die Meldungsdaten: optionalen Ersteller, Titel, Kategorie, konkrete Defektart, Beschreibung, Standortkoordinaten, Status, Zeitstempel, den Zeitpunkt der Erledigung und optionalen Fotopfad. Der Erledigungszeitpunkt startet die siebentägige Aufbewahrungsfrist. Bereits vor Einführung der Funktion erledigte Meldungen erhalten bei der Migration keinen rückwirkenden Fristbeginn und bleiben erhalten.
- `comments` speichert öffentliche Updates und interne Notizen zu einer Meldung. Ein Kennzeichen unterscheidet interne Kommentare, die nur für Mitarbeitende und Administratoren bestimmt sind. Kommentare können ebenfalls einen Fotopfad enthalten.
- `report_votes` speichert die Zuordnung zwischen Bürger und Meldung. Eine Eindeutigkeitsbedingung verhindert, dass derselbe Benutzer dieselbe Meldung mehrfach gleichzeitig bewertet.
- `attachments` speichert zusätzliche Dateipfade zu Meldungen. Die dafür vorhandenen REST-Endpunkte sind im Backend verfügbar; ein separater Ablauf für diese Anhänge ist aktuell jedoch nicht in die Benutzeroberfläche eingebunden.

Die Bilddateien selbst werden nicht als Binärdaten in SQLite gespeichert, sondern im lokalen Verzeichnis `uploads/`. In den Tabellen stehen lediglich die zugehörigen Pfade. Fremdschlüssel verbinden Benutzer, Meldungen, Kommentare, Stimmen und Anhänge: Wird ein Benutzer gelöscht, bleibt eine von ihm erstellte Meldung erhalten und verliert ihre Benutzerzuordnung; zu einer gelöschten Meldung gehörende Kommentare, Stimmen und Anhänge werden dagegen entfernt. So bleiben die Beziehungen zwischen den gespeicherten Daten nachvollziehbar.

### 4.5 UML-Diagramme

Die UML-Diagramme ergänzen die textuelle Architekturbeschreibung aus verschiedenen Blickwinkeln. Das Use-Case-Diagramm zeigt die Funktionen und Rollen von anonymen Besuchern, Bürgern, Mitarbeitenden und Administratoren. Das Domänen-Klassendiagramm stellt die zentralen Objekte wie `User`, `Report`, `Comment` und `Attachment` sowie ihre Beziehungen dar. Das Komponentendiagramm veranschaulicht die Abhängigkeiten zwischen Frontend, Spring-Controllern, Services, Repositories, SQLite und Dateisystem.

Für den Ablauf beim Erstellen einer Meldung gibt es ein Aktivitätsdiagramm und ein Sequenzdiagramm. Das Aktivitätsdiagramm zeigt die fachlichen Schritte und Verzweigungen, beispielsweise die optionale Defektart und das optionale Foto. Das Sequenzdiagramm ergänzt dies um die Zusammenarbeit zwischen Browser, Controller, Resolver, Bilddienst, Service, Repository und Datenbank. Das Bereitstellungsdiagramm zeigt schließlich, welche Teile lokal auf dem Computer laufen und für welche Funktion – die OpenStreetMap-Kartenkacheln – eine externe Verbindung benötigt wird.

Die Diagramme liegen als bearbeitbare PlantUML-Quellen unter `docs/uml/`; zusätzlich sind sie als SVG und PNG exportiert. SVG eignet sich besonders für den Bericht, weil es sich ohne Qualitätsverlust skalieren lässt. Die Diagramme dokumentieren den aktuellen Implementierungsstand und sollten angepasst werden, wenn sich Rollen, Datenmodell, API oder Bereitstellung ändern.

## 5. Technische Umsetzung

### Web-Technologien

- HTML5, CSS3 und JavaScript
- Java 17, Spring Boot und Maven
- REST-API und Spring Security
- SQLite/JDBC
- Leaflet
- BCrypt

### Auswahl der Technologien

Die Entscheidung für eine Webanwendung war durch die Anforderungen und den didaktischen Rahmen begründet. Sie bietet:

- schnelle Entwicklung
- einfache Präsentationsfähigkeit
- gute Nachvollziehbarkeit für Teammitglieder
- geringe Einstiegshürde für die Evaluation und Präsentation

OpenStreetMap und Leaflet wurden gewählt, weil sie kostenlos nutzbar sind und für geographische Anwendungsfälle besonders geeignet sind.

## 6. Projektmanagement und Teamarbeit

Das Projekt wurde mit einem klaren Ablauf geplant:

- Analyse der Aufgabenstellung
- Definition der Anforderungen
- Architektur- und Technologieentscheidung
- Implementierung von Frontend und Backend
- Verifikation der API-Funktionalität
- Vorbereitung von Dokumentation und Präsentation

Die Aufteilung in Frontend-, Backend- und Gesamtsystem-Komponenten ist in einem kleinen Softwareprojekt sinnvoll und zeigt typische agile Arbeitsweisen. Die trennbaren Module erlauben ein unkompliziertes Verständnis und spätere Erweiterungen.

## 7. Qualitätssicherung

Automatisierte JUnit-Service- und Repository-Tests prüfen einzelne Backend-Funktionen, darunter Registrierung und Passwortprüfung, Meldungsvalidierung, konkrete Defektarten und Koordinaten, die Admin-Berechtigung zur Mitarbeiterverwaltung, Statusänderungen, die siebentägige automatische Löschfrist, das Löschen erledigter Meldungen und zugehöriger Bilddateien, interne Kommentare mit Rollenprüfung, Stimmen und Zurücknehmen sowie öffentliche Fortschrittsinformationen. Die Tests werden mit `mvn test` ausgeführt. REST-Controller und vollständige Browserabläufe werden dadurch nicht vollständig automatisiert abgedeckt und sollten ergänzend manuell geprüft werden.

Benutzerfreundlichkeit und vollständige Browserabläufe werden manuell geprüft: Anmeldung, Meldungserfassung mit Karte und Foto, Anzeige eigener Meldungen sowie Statuswechsel durch Mitarbeitende. Diese Usability-Prüfung ergänzt die automatisierten Service-Tests.

## 8. Risiken und Erweiterungen

Wichtige Erweiterungsmöglichkeiten sind:

- HTTPS-Bereitstellung für Zugriff über das lokale Netzwerk
- zusätzliche Integrationstests für Controller, REST-Endpunkte und Datenbankmigrationen
- E-Mail- oder Push-Benachrichtigungen
- Export und Analyse der Meldungen
- Bildoptimierung und Cloud-Speicherung

Diese Erweiterungen zeigen, dass die Architektur für reale Anwendungen mit größerem Umfang geeignet ist.

## 9. Fazit

„FixMyCity“ erfüllt die wesentlichen Anforderungen der Aufgabenstellung und demonstriert eine vollständige, nutzbare Webanwendung. Die Kombination aus Kartenfunktion, Formularerfassung, Statusverwaltung und Foto-Upload bildet eine praxisnahe Lösung für die Kommunikation zwischen Bürgern und Stadtverwaltung.

Das Projekt zeigt, wie Theorie und praktische Softwareentwicklung im Team zusammengeführt werden können. Es kombiniert grundlegende Architekturmuster, sinnvolle Technologieauswahl und eine verständliche Benutzeroberfläche in einem realistischen Anwendungsszenario.
