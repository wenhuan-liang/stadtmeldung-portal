# Testdokumentation FixMyCity

Stand: 16.11.2026 · Branch `testing`

## 1. Überblick

Die Qualitätssicherung von FixMyCity besteht aus vier Ebenen, die zusammen eine Testpyramide bilden. Je weiter unten, desto mehr Tests, desto schneller und desto genauer zeigt ein Fehler auf die Ursache.

| Ebene | Werkzeug | Anzahl | Was wird geprüft | Laufzeit |
|---|---|---|---|---|
| Unit-Tests | JUnit 5 + Mockito | 136 | Geschäftslogik der Services isoliert (Validierung, Rollenrechte) | < 3 s |
| Integrationstests Datenbank | JUnit 5 + SQLite | 17 | SQL, Schema, Fremdschlüssel, Kaskaden | < 1 s |
| API-Tests | Spring MockMvc | 34 | HTTP-Statuscodes, JSON, CSRF, Login/Sitzung, Rechte pro Rolle, Foto-Upload | ca. 15 s |
| End-to-End-Tests | Selenium + Chrome | 7 | Komplette Abläufe in der echten Oberfläche | ca. 30 s |
| Usability-Tests | Testpersonen | – | Verständlichkeit und Bedienbarkeit (siehe `usability-test.md`) | manuell |

**Ergebnis:** 194 automatisierte Tests, alle bestanden. Zeilenabdeckung des Backends 80,8 %, Zweigabdeckung 73,0 %.

## 2. Tests ausführen

```bash
mvn test            # Unit-, Integrations- und API-Tests (187 Tests)
mvn test -Pe2e      # Selenium-Browsertests (7 Tests, benötigt Chrome + Internet)
mvn test -Pe2e -Dheadless=false   # Browser sichtbar mitlaufen lassen
```

Ergebnisse:

- Testabdeckung: `target/site/jacoco/index.html` im Browser öffnen
- Screenshots der Browsertests: `target/e2e-screenshots/`
- Einzelberichte: `target/surefire-reports/`

Die Tests verwenden eine eigene Datenbank (`target/test-db/portal-test.db`). Die echte `portal.db` wird nie verändert. Werden Integrationstests direkt aus der IDE ohne Maven gestartet, überspringen sie sich selbst, statt echte Daten zu löschen.

## 3. Ebene 1: Unit-Tests mit JUnit und Mockito

**Prinzip:** Ein Service wird ohne Datenbank getestet. Mockito ersetzt die Repositories durch Attrappen (Mocks), deren Rückgabewerte im Test festgelegt werden. Anschließend wird geprüft, ob der Service richtig reagiert und ob er z. B. `save()` aufruft oder bei fehlender Berechtigung gerade **nicht** aufruft.

Jeder Test folgt dem Muster **Arrange – Act – Assert** (Ausgangslage herstellen, Aktion ausführen, Ergebnis prüfen) und hat einen deutschen `@DisplayName`.

Beispiel (`ReportServiceImplTest`):

```java
@Test
@DisplayName("Bürger darf den Status nicht ändern – auch nicht bei eigener Meldung")
void citizenCannotUpdateStatus() {
    when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));    // Arrange

    assertThrows(SecurityException.class,                                    // Act + Assert
            () -> reportService.updateStatus(10L, Status.COMPLETED, 1L));
    verify(reportRepository, never()).update(any());                         // nichts gespeichert
}
```

| Testklasse | Tests | Schwerpunkte |
|---|---|---|
| `UserServiceImplTest` | 43 | Registrierung, Login, Mitarbeiterverwaltung, Grenzwerte Benutzername (3/32 Zeichen) und Passwort (12 Zeichen / 72 Bytes) |
| `ReportServiceImplTest` | 48 | Meldung anlegen, bearbeiten, löschen; Koordinaten, Titel- und Textlänge, 7 Defektarten, Statusrechte |
| `CommentServiceImplTest` | 14 | Öffentliche Updates nur von Bürgern, interne Kommentare nur vom Stadt-Team |
| `ReportVoteServiceImplTest` | 8 | Abstimmen, Doppelstimme, Rücknahme, Stadt-Team darf nicht abstimmen |
| `AttachmentServiceImplTest` | 7 | Nur Eigentümer verwalten Anhänge |
| `BCryptPasswordHasherTest` | 5 | Echtes Hashing: Salt, Verifikation, fehlerhafte Eingaben |
| `BackendServiceTest` (bestehend) | 11 | Ursprüngliche Tests des Teams mit selbstgeschriebenen Fakes |

**Testmethoden:**

- **Grenzwertanalyse:** Jede Grenze wird von beiden Seiten getestet. Ein Titel mit 120 Zeichen ist gültig, einer mit 121 Zeichen ungültig. Breitengrad 90 ist gültig, 90,0001 ungültig.
- **Äquivalenzklassen:** Mit parametrisierten Tests (`@ParameterizedTest`) laufen viele Eingaben derselben Klasse durch einen Test, z. B. NaN, Unendlich und Werte außerhalb des Bereichs bei den Koordinaten.
- **Rollenmatrix:** Jede geschützte Aktion wird für jede Rolle geprüft (Bürger, Mitarbeiter, Admin).

## 4. Ebene 2: Integrationstests mit der Datenbank

**Prinzip:** Die echten Repository-Klassen arbeiten gegen eine echte SQLite-Testdatenbank. Vor jedem Test wird sie geleert. Damit wird geprüft, was Mocks nicht prüfen können: ob SQL-Befehle, Tabellen und Fremdschlüssel korrekt sind.

Testklasse: `DatabaseRepositoryIntegrationTest`

| Geprüftes Verhalten | Erwartung |
|---|---|
| Speichern und Laden aller Felder | Werte identisch, ID und Erstellungszeit gesetzt |
| Doppelter Benutzername | Datenbank lehnt ab (UNIQUE) |
| Benutzer löschen | Seine Meldungen bleiben erhalten, aber anonymisiert (ON DELETE SET NULL) |
| Meldung löschen | Kommentare, Stimmen und Anhänge werden mitgelöscht (ON DELETE CASCADE) |
| Zweite Stimme desselben Bürgers | wird ignoriert, Zähler bleibt bei 1 |
| Stimme für nicht existierende Meldung | Datenbank lehnt ab (Fremdschlüssel aktiv) |
| Sortierung und Statusfilter | neueste Meldung zuerst, Filter liefert nur passende |

## 5. Ebene 3: API-Tests mit MockMvc

**Prinzip:** Das komplette Backend (Spring Boot, Spring Security, Controller, Services, Testdatenbank) startet. MockMvc schickt HTTP-Anfragen wie das Frontend, aber ohne Browser und ohne Netzwerk. Die Hilfsklasse `ApiClient` verhält sich wie ein Browser: Sie meldet sich über `/api/login` an, behält das Session-Cookie und sendet das CSRF-Token mit.

Testklasse: `ApiIntegrationTest`

| Bereich | Beispiele für geprüfte Fälle |
|---|---|
| Öffentlicher Zugriff | Meldungsliste lesbar (200), anonyme Meldung (201), ungültige Kategorie/Koordinaten (400), geschützte Endpunkte ohne Login (401) |
| Sicherheit | Anfrage ohne CSRF-Token wird abgewiesen (403) und erzeugt keine Daten |
| Konten | Registrierung (201), schwaches Passwort/vergebener Name (400), falsches Passwort (401, neutrale Meldung), Logout beendet Sitzung |
| Rollenrechte | Bürger ändert Status → 403, Mitarbeiter → 200; nicht erledigte Meldung löschen → 409; Mitarbeitende reichen keine Meldungen ein → 403 |
| Abstimmung | Stimme (201), zweite Stimme (409), Rücknahme (200), Mitarbeiter (403) |
| Kommentare | Interne Kommentare sieht nur das Stadt-Team, Besucher und Bürger nicht |
| Administration | Admin legt Mitarbeiter an; Löschen beendet dessen aktive Sitzung sofort |
| Foto-Upload | PNG wird gespeichert; HTML/Text abgelehnt (400); bei ungültiger Meldung bleibt kein verwaistes Foto zurück |

## 6. Ebene 4: End-to-End-Tests mit Selenium

**Prinzip:** Die Anwendung wird auf einem freien Port gestartet. Selenium steuert einen echten Chrome-Browser ohne Fenster, klickt Buttons, füllt Formulare aus und prüft, was auf dem Bildschirm erscheint. Bei jedem Schritt wird ein Screenshot gespeichert.

Testklasse: `FrontendEndToEndTest`

| ID | Vorbedingung | Testschritte | Erwartetes Ergebnis | Ergebnis |
|---|---|---|---|---|
| E2E-1 | Nicht angemeldet | Startseite öffnen → „Neue Meldung“ → Titel, Kategorie „Defekt“, Defektart, Beschreibung eingeben → Standort auf Karte klicken → „Meldung senden“ | Weiterleitung zur Startseite; Meldung erscheint mit Status „Wartet auf Bearbeitung durch die Stadt“; genau 1 Eintrag in der Datenbank | ✅ bestanden |
| E2E-2 | Nicht angemeldet | Formular ohne Kartenklick absenden | Fehlermeldung „Bitte wählen Sie einen Standort auf der Karte aus.“; keine Meldung gespeichert | ✅ bestanden |
| E2E-3 | Kein Konto | Registrieren → Meldung anlegen → bei „Meine Meldungen“ abstimmen | Name in Navigationsleiste; Weiterleitung zu „Meine Meldungen“ mit Kennzeichnung „Von mir gemeldet“; nach Abstimmung „1 Stimme“ und Button „Stimme zurücknehmen“ | ✅ bestanden |
| E2E-4 | Meldung existiert, Mitarbeiterkonto existiert | Als Mitarbeiter anmelden → Status-Auswahl auf „Erledigt“ | Statusanzeige „Erledigt“; in der Datenbank COMPLETED | ✅ bestanden |
| E2E-5 | Bürgerkonto existiert | Login mit falschem Passwort | Fehlermeldung „Benutzername oder Passwort ungültig.“; bleibt auf Login-Seite | ✅ bestanden |
| E2E-6 | Bürgerkonto existiert | Bürger versucht Login im Mitarbeiterbereich | Fehlermeldung „Dieser Account kann sich in diesem Bereich nicht anmelden.“ | ✅ bestanden |
| E2E-7 | Adminkonto existiert | Als Admin anmelden → „Mitarbeiterkonten verwalten“ → Name und Passwort eingeben → absenden | Erfolgsmeldung; neues Konto erscheint in der Liste | ✅ bestanden |

**Vorteile und Grenzen von Selenium in diesem Projekt:**

| Vorteile | Grenzen |
|---|---|
| Testet das Zusammenspiel von HTML, JavaScript und Backend | Langsamer als die anderen Testarten |
| Wiederholbar, ersetzt manuelles Durchklicken | Braucht Chrome und Internet (Leaflet-Karte von unpkg.com) |
| Screenshots dienen als Nachweis | Zeitabhängig: Die Tests müssen auf asynchrones Nachladen warten |
| Läuft in Java/JUnit im selben Projekt | Prüft nicht, ob die Oberfläche verständlich ist (dafür Usability-Tests) |

## 7. Testabdeckung (JaCoCo)

| Paket | Zeilen | Zweige |
|---|---|---|
| `service.implementation` (Geschäftslogik) | 87,7 % | 80,8 % |
| `com.stadtmeldung.controller` (REST-API) | 89,3 % | 70,1 % |
| `com.stadtmeldung.security` | 86,2 % | 58,3 % |
| `repository.implementation` (SQL) | 65,7 % | 69,7 % |
| `com.stadtmeldung.config` | 100 % | 100 % |
| **Gesamt** | **80,8 %** | **73,0 %** |

Die niedrigere Abdeckung der Repositories liegt hauptsächlich an `catch (SQLException …)`-Blöcken. Diese laufen nur bei Datenbankausfällen und lassen sich nur mit unverhältnismäßigem Aufwand gezielt auslösen. Abdeckung ist ein Hinweis auf ungetestete Stellen, aber kein Qualitätsbeweis.

## 8. Befunde aus dem Testen

Beim Testen wurden keine Fehler gefunden, die die Funktion oder Sicherheit beeinträchtigen. Folgende Auffälligkeiten wurden notiert; der Produktivcode wurde bewusst nicht verändert:

| Nr. | Befund | Art | Empfehlung |
|---|---|---|---|
| B1 | Fehlermeldungen aus der Service-Schicht sind englisch und werden so im deutschen Frontend angezeigt, z. B. bei zu kurzem Passwort: „Password must be at least 12 characters and at most 72 UTF-8 bytes“ | Usability | Meldungen in den Services auf Deutsch umstellen |
| B2 | Die Karte startet zentriert auf Berlin | Usability | Startposition auf das Einsatzgebiet der Stadt setzen |
| B3 | Die Karte (Leaflet) wird von einem externen Server geladen; ohne Internet ist keine Standortauswahl möglich | Robustheit | Leaflet lokal in `public/` ausliefern |
| B4 | `ReportService.deleteReport` (Bürger löscht eigene Meldung) und `updateReport` sind getestet, aber über die API nicht erreichbar | Wartbarkeit | Entweder Endpunkt ergänzen oder ungenutzte Methoden entfernen |

## 9. Abgrenzung

Nicht automatisiert getestet wurden:

- die GPS-Funktion (sie braucht einen echten Standort),
- die Darstellung auf Smartphones und die Barrierefreiheit,
- die Last bei vielen gleichzeitigen Nutzern.

Die mobile Darstellung und Verständlichkeit werden über die Usability-Tests abgedeckt.
