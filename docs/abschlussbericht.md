# Abschlussbericht: Stadtmeldung Portal

## 1. Einleitung

Das Projekt „Stadtmeldung Portal“ wurde im Rahmen der Vorlesung Software Engineering entwickelt. Ziel war die Umsetzung einer praxisnahen Applikation, mit der Bürger Probleme in ihrer Stadt oder Gemeinde digital melden können. Die Software unterstützt dabei die Erfassung von Defekten, Verunreinigungen und optionalen Parkverstößen mit geografischer Positionierung, Beschreibung und optionalem Foto.

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

2. Verwaltung der Meldungen
   - Anzeige aller Meldungen
   - Filterung nach Kategorie
   - Statusverwaltung: Neu, In Bearbeitung, Erledigt
   - Löschen von Meldungen

3. Visualisierung
   - Interaktive OpenStreetMap-Karte via Leaflet
   - Einfache, verständliche Darstellung der Meldungen

4. Backend-API
   - REST-ähnliche Endpunkte für Healthcheck, Lesen, Anlegen, Aktualisieren und Löschen
   - Lokale JSON-Datei als persistenter Datensatz

## 4. Architekturbeschreibung

Die Anwendung folgt einer einfachen dreischichtigen Struktur:

### Frontend

Die Benutzeroberfläche wird als statische HTML-, CSS- und JavaScript-Anwendung implementiert. Die Darstellung erfolgt in einem modernen Dashboard-Stil mit zentralem Formular zur Meldungserfassung und einer Meldungsliste zur Verwaltung.

### Backend

Das Backend basiert auf Node.js und Express.js. Es stellt die notwendigen API-Endpunkte bereit und verarbeitet Formulare sowie Bilddateien. Die wichtigsten Endpunkte sind:

- GET /api/health
- GET /api/reports
- POST /api/reports
- PATCH /api/reports/:id
- DELETE /api/reports/:id

### Datenhaltung

Für die einfache Nutzung und schnelle Umsetzung wird eine lokale JSON-Datei verwendet. Diese speichert die Meldungen im Dateisystem und ermöglicht dadurch unkomplizierte Tests und Demonstration ohne zusätzliche Datenbankinfrastruktur.

## 5. Technische Umsetzung

### Web-Technologien

- HTML5
- CSS3
- JavaScript
- Express.js
- Leaflet
- Multer

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

Für die Qualitätssicherung wurden verschiedene Maßnahmen umgesetzt:

- Serverstart und Healthcheck erfolgreich getestet
- API-Endpunkte durch reale HTTP-Anfragen verifiziert
- Validierung der Formular- und Statuslogik
- Prüfung der Datenpersistenz in der JSON-Datei
- Aufbau einer robusten Fehlerbehandlung im Backend

Zusätzlich wurde der Code so strukturiert, dass spätere Erweiterungen, etwa eine Datenbank, Benutzerauthentifizierung oder automatische Benachrichtigungen, ohne weitreichende Umbauten möglich sind.

## 8. Risiken und Erweiterungen

Wichtige Risiken und Erweiterungsmöglichkeiten sind:

- eigene Benutzerrollen für Bürger und Behörden
- Login- und Rechteverwaltung
- Datenbanklösung statt JSON-Persistenz
- E-Mail- oder Push-Benachrichtigungen
- Export und Analyse der Meldungen
- Bildoptimierung und Cloud-Speicherung

Diese Erweiterungen zeigen, dass die Architektur für reale Anwendungen mit größerem Umfang geeignet ist.

## 9. Fazit

Das „Stadtmeldung Portal“ erfüllt die wesentlichen Anforderungen der Aufgabenstellung und demonstriert eine vollständige, nutzbare Webanwendung. Die Kombination aus Kartenfunktion, Formularerfassung, Statusverwaltung und Foto-Upload bildet eine praxisnahe Lösung für die Kommunikation zwischen Bürgern und Stadtverwaltung.

Das Projekt zeigt, wie Theorie und praktische Softwareentwicklung im Team zusammengeführt werden können. Es kombiniert grundlegende Architekturmuster, sinnvolle Technologieauswahl und eine verständliche Benutzeroberfläche in einem realistischen Anwendungsszenario.
