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

Die Anwendung erfüllt diese Anforderungen durch eine zentrale Weboberfläche, in der Meldungen erfasst und anhand ihrer Kategorie sowie ihres Status verwaltet werden.

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
   - Filterung nach Kategorie
   - Statusverwaltung: Neu, In Bearbeitung, Erledigt
   - Löschen von Meldungen

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

## 4. Architekturbeschreibung

Die Anwendung folgt einer einfachen dreischichtigen Struktur:

### Frontend

Die Benutzeroberfläche wird als statische HTML-, CSS- und JavaScript-Anwendung implementiert. Die Darstellung erfolgt in einem modernen Dashboard-Stil mit zentralem Formular zur Meldungserfassung und einer Meldungsliste zur Verwaltung.

### Backend

Das Backend basiert auf Java 17 und stellt mit dem JDK-HTTP-Server eine same-origin HTTP-API bereit. Die Oberfläche wird vom selben Prozess ausgeliefert; API und Frontend laufen damit lokal auf demselben Computer. Die Anwendung bindet standardmäßig ausschließlich an Loopback (127.0.0.1).

Die API ermöglicht Anmeldung und Abmeldung, optionale Bürgerregistrierung, anonyme Meldungserfassung, das Abrufen eigener bzw. relevanter Meldungen, widerrufbare Bürgerstimmen, öffentliche Fortschrittsinformationen mit optionalen Fotos sowie Statusänderungen durch Mitarbeitende und Administratoren. In „Meine Meldungen“ werden eigene Meldungen sowie Meldungen angezeigt, für die der Bürger gestimmt oder die er ergänzt hat. Diese Beteiligungsarten lassen sich getrennt filtern und werden zusätzlich an den jeweiligen Meldungen gekennzeichnet. Die Anmeldung unterscheidet Bürger, Mitarbeitende der Stadt und Administratoren; Anmeldelinks aus Bürgeransichten öffnen direkt die Bürgeranmeldung. Anonyme Besucher können ohne Konto Meldungen einreichen. Die Mitarbeiteroberfläche dient ausschließlich der Einsicht und Bearbeitung und bietet keine Erfassungs- oder „Meine Meldungen“-Funktion. Bürger sehen verständliche Statusbezeichnungen („Wartet auf Bearbeitung durch die Stadt“, „In Bearbeitung durch die Stadt“, „Von der Stadt erledigt“). „Alle Meldungen“ steht als eigener Filter außerhalb dieser Statusgruppen. Der erste Administrator wird bei der Ersteinrichtung provisioniert; anschließend können ausschließlich Administratoren neue Mitarbeiterkonten anlegen, vorhandene Mitarbeiter einsehen und Mitarbeiterkonten löschen. Beim Löschen werden die Sitzungen des Mitarbeiters beendet. Service- und Repository-Schichten aus dem Backend-Zweig bleiben erhalten.

Die Leaflet-Karten verwenden den offiziellen OSM-Tile-Endpunkt, sichtbare Attribution und eine Referrer-Policy, die den für Browser-Tileanfragen notwendigen Referer zulässt.

### Datenhaltung und Kontosicherheit

SQLite speichert Benutzer und Meldungen lokal in `portal.db`; optionale Fotos liegen unter `uploads/`. Passwörter werden serverseitig mit BCrypt gehasht und nicht im Klartext gespeichert. Nach der Anmeldung erhält der Browser ein zufälliges, serverseitig geprüftes Sitzungstoken in einem HttpOnly-/SameSite-Cookie. Rollen werden serverseitig geprüft und können nicht über das Frontend gesetzt werden.

Da die Anwendung nur auf Loopback lauscht, verlassen Zugangsdaten den Computer nicht über das LAN. HTTP und Cookies sind jedoch keine Transportverschlüsselung. Für einen Zugriff von anderen Geräten ist eine HTTPS-Konfiguration erforderlich.

## 5. Technische Umsetzung

### Web-Technologien

- HTML5, CSS3 und JavaScript
- Java 17 HTTP-Server und Maven
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

Automatisierte JUnit-Tests prüfen einzelne Backend-Funktionen, insbesondere Registrierung und Passwortprüfung, Validierung von Meldungen, konkrete Defektarten, Koordinaten, die Admin-Berechtigung zur Mitarbeiterverwaltung, Rollenberechtigungen, Löschen von Mitarbeiterkonten, Stimmen und Zurücknehmen sowie das Anlegen und Validieren von Fortschrittsinformationen. Die Tests werden mit `mvn test` ausgeführt.

Benutzerfreundlichkeit und vollständige Browserabläufe werden manuell geprüft: Anmeldung, Meldungserfassung mit Karte und Foto, Anzeige eigener Meldungen sowie Statuswechsel durch Mitarbeitende. Diese Usability-Prüfung ergänzt die automatisierten Service-Tests.

## 8. Risiken und Erweiterungen

Wichtige Erweiterungsmöglichkeiten sind:

- HTTPS-Bereitstellung für Zugriff über das lokale Netzwerk
- CSRF-Schutz zusätzlich zu den SameSite-Cookie-Einstellungen, falls neue cross-site Integrationen hinzukommen
- E-Mail- oder Push-Benachrichtigungen
- Export und Analyse der Meldungen
- Bildoptimierung und Cloud-Speicherung

Diese Erweiterungen zeigen, dass die Architektur für reale Anwendungen mit größerem Umfang geeignet ist.

## 9. Fazit

„FixMyCity“ erfüllt die wesentlichen Anforderungen der Aufgabenstellung und demonstriert eine vollständige, nutzbare Webanwendung. Die Kombination aus Kartenfunktion, Formularerfassung, Statusverwaltung und Foto-Upload bildet eine praxisnahe Lösung für die Kommunikation zwischen Bürgern und Stadtverwaltung.

Das Projekt zeigt, wie Theorie und praktische Softwareentwicklung im Team zusammengeführt werden können. Es kombiniert grundlegende Architekturmuster, sinnvolle Technologieauswahl und eine verständliche Benutzeroberfläche in einem realistischen Anwendungsszenario.
