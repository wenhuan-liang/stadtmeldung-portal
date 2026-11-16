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
   - Mitarbeitende und Administratoren können erledigte Meldungen löschen.

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

## 4. Architekturbeschreibung

Die Anwendung verwendet eine geschichtete Struktur mit getrenntem Frontend, Spring-Boot-REST-API, Geschäftslogik und Datenzugriff:

### Frontend

Die Benutzeroberfläche wird als statische HTML-, CSS- und JavaScript-Anwendung implementiert. Die Darstellung erfolgt in einem modernen Dashboard-Stil mit zentralem Formular zur Meldungserfassung und einer Meldungsliste zur Verwaltung.

### Backend und REST-API

Das Backend basiert auf Java 17 und Spring Boot. REST-Controller stellen die API unter `/api` bereit; Spring Boot liefert außerdem die statische Oberfläche aus `public/` aus. Dadurch verwenden Frontend und API dieselbe Herkunft und laufen lokal im selben Prozess. Die Anwendung bindet standardmäßig ausschließlich an Loopback (127.0.0.1).

Die API ermöglicht Anmeldung und Abmeldung, Bürgerregistrierung, anonyme Meldungserfassung, das Abrufen eigener bzw. relevanter Meldungen, widerrufbare Bürgerstimmen, öffentliche Fortschrittsinformationen mit optionalen Fotos sowie Statusänderungen durch Mitarbeitende und Administratoren. Zusätzlich existieren REST-Endpunkte zur Verwaltung weiterer Anhänge an eigenen Meldungen. Dieser separate Anhangsablauf ist derzeit nicht in die Benutzeroberfläche eingebunden; das Formular zum Einreichen einer Meldung übermittelt sein optionales Foto direkt mit der Meldung. Interne Kommentare werden separat gespeichert und nur an Mitarbeitende und Administratoren ausgeliefert. In „Meine Meldungen“ werden eigene Meldungen sowie Meldungen angezeigt, für die der Bürger gestimmt oder die er ergänzt hat. Diese Beteiligungsarten lassen sich getrennt filtern und werden zusätzlich an den jeweiligen Meldungen gekennzeichnet. Die Mitarbeiteroberfläche dient der Einsicht und Bearbeitung; sie bietet keine Meldungserfassung und keine „Meine Meldungen“-Funktion. Der erste Administrator wird bei der Ersteinrichtung provisioniert; anschließend können ausschließlich Administratoren Mitarbeiterkonten anlegen, anzeigen und löschen. Beim Löschen werden die Sitzungen des Mitarbeiters beendet. Die Service- und Repository-Schichten des Backend-Zweigs bleiben erhalten.

Die Leaflet-Karten verwenden den offiziellen OSM-Tile-Endpunkt, sichtbare Attribution und eine Referrer-Policy, die den für Browser-Tileanfragen notwendigen Referer zulässt.

### Datenhaltung und Kontosicherheit

SQLite speichert Benutzer, Meldungen, Kommentare, Stimmen und Anhangspfade lokal in `portal.db`; Bilddateien liegen unter `uploads/`. Passwörter werden serverseitig mit BCrypt gehasht und nicht im Klartext gespeichert. Spring Security verwaltet die Anmeldung über eine serverseitige Sitzung im Sitzungscookie. Schreibzugriffe sind mit CSRF-Schutz abgesichert; Rollen und Berechtigungen werden serverseitig geprüft und können nicht über das Frontend gesetzt werden.

Da die Anwendung nur auf Loopback lauscht, verlassen Zugangsdaten den Computer nicht über das LAN. HTTP und Cookies sind jedoch keine Transportverschlüsselung. Für einen Zugriff von anderen Geräten ist eine HTTPS-Konfiguration erforderlich.

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

Automatisierte JUnit-Service-Tests prüfen einzelne Backend-Funktionen, darunter Registrierung und Passwortprüfung, Meldungsvalidierung, konkrete Defektarten und Koordinaten, die Admin-Berechtigung zur Mitarbeiterverwaltung, Statusänderungen, das Löschen erledigter Meldungen, interne Kommentare mit Rollenprüfung, Stimmen und Zurücknehmen sowie öffentliche Fortschrittsinformationen. Die Tests werden mit `mvn test` ausgeführt. REST-Controller und vollständige Browserabläufe werden dadurch nicht vollständig automatisiert abgedeckt und sollten ergänzend manuell geprüft werden.

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
