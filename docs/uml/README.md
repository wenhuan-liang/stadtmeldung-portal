# UML-Diagramme

Diese Diagramme dokumentieren den aktuellen Stand von FixMyCity: Spring Boot mit REST-API, einer HTML-/JavaScript-Oberflaeche und SQLite.

## Enthaltene Diagramme

- [Use-Case-Diagramm](./use-cases.puml): Funktionen fuer anonyme Besucher, Buerger, Mitarbeiter und Administratoren.
- [Domaenen-Klassendiagramm](./domain-classes.puml): zentrale Datenobjekte, Beziehungen und Rollen-/Status-/Kategorieaufzaehlungen.
- [Komponentendiagramm](./components.puml): Frontend, Spring-Controller, Services, Repositories, Sicherheit und Datenspeicher.
- [Aktivitaetsdiagramm](./report-submission-activity.puml): Ablauf beim Einreichen einer Meldung.
- [Sequenzdiagramm](./report-submission-sequence.puml): Zusammenarbeit der Systemteile beim Erstellen einer Meldung.
- [Bereitstellungsdiagramm](./deployment.puml): lokale Laufzeit, SQLite, Uploads und OpenStreetMap-Kacheln.

## Anzeigen und exportieren

Die Dateien sind PlantUML-Quellen (`.puml`). In Visual Studio Code koennen sie mit einer PlantUML-Erweiterung angezeigt und als SVG oder PNG exportiert werden. SVG bleibt beim Vergroessern und Verkleinern scharf; PNG ist ein Rasterbild. Die Workspace-Einstellung exportiert standardmaessig alle Diagramme nach `docs/uml/export/` als SVG. Im Befehlspaletten-Menue (`Ctrl+Shift+P`) dafuer **PlantUML: Export Workspace Diagrams** auswaehlen.

Die Diagrammtexte schreiben Umlaute als `ae`, `oe`, `ue` und `ss`, damit sie auch mit eingeschraenkter Zeichensatzunterstuetzung lesbar bleiben.

Die Diagramme sind Dokumentation, keine Implementierung. Bei Aenderungen an Rollen, API-Endpunkten, Datenmodellen oder Bereitstellung sollten die betroffenen Diagramme mit aktualisiert werden.
