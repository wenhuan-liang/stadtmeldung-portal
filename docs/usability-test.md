# Usability-Test FixMyCity – Leitfaden und Vorlagen

Usability-Tests prüfen, ob echte Menschen die Anwendung **ohne Erklärung** verstehen und bedienen können. Automatisierte Tests können das nicht leisten: Selenium findet einen Button immer, ein Mensch vielleicht nicht.

## 1. Ablauf

1. **Testpersonen:** 5 Personen außerhalb des Projektteams. Fünf Personen decken erfahrungsgemäß die meisten Bedienprobleme auf. Idealerweise sind es 3–4 „Bürger“ (verschiedenes Alter, auch technisch unerfahren) und 1–2 Personen in der Rolle „Mitarbeiter der Stadt“.
2. **Rollen im Team:** Eine Person leitet den Test (liest die Aufgaben vor und hilft **nicht**), eine Person protokolliert.
3. **Methode „Lautes Denken“:** Die Testperson sagt laut, was sie denkt, sucht und erwartet („Ich suche jetzt, wo ich … kann“).
4. **Dauer:** ca. 20 Minuten pro Person, davon 15 Minuten Aufgaben und 5 Minuten Fragebogen.
5. **Geräte:** Mindestens eine Person testet am Smartphone.
6. **Vorbereitung:** App starten, ein Mitarbeiterkonto anlegen und 3–4 Beispielmeldungen erfassen.

Einleitung für die Testperson (vorlesen):

> „Wir testen die Anwendung, nicht Sie. Sie können nichts falsch machen. Bitte sagen Sie laut, was Sie denken. Wir helfen Ihnen während der Aufgaben nicht, weil wir sehen möchten, wo die Anwendung unklar ist.“

## 2. Aufgaben

| Nr. | Rolle | Aufgabe (vorlesen) | Erfolgskriterium |
|---|---|---|---|
| A1 | Bürger | „Vor Ihrem Haus ist ein Schlagloch. Melden Sie es, ohne ein Konto anzulegen.“ | Meldung mit Defektart und Standort gesendet |
| A2 | Bürger | „Legen Sie ein Bürgerkonto an.“ | Registrierung erfolgreich |
| A3 | Bürger | „Finden Sie heraus, ob Ihre Meldung schon bearbeitet wird.“ | „Meine Meldungen“ gefunden, Status richtig gedeutet |
| A4 | Bürger | „Eine andere Person hat eine kaputte Laterne gemeldet. Unterstützen Sie diese Meldung.“ | Stimme abgegeben |
| A5 | Bürger | „Der Schaden ist größer geworden. Ergänzen Sie diese Information mit einem Foto.“ | Update mit Foto gespeichert |
| A6 | Mitarbeiter | „Melden Sie sich als Mitarbeiter an und zeigen Sie nur neue Meldungen an.“ | Login im richtigen Bereich, Filter genutzt |
| A7 | Mitarbeiter | „Setzen Sie die Meldung mit den meisten Stimmen auf ‚In Bearbeitung‘ und hinterlassen Sie eine Notiz für Kollegen.“ | Sortierung genutzt, Status geändert, interne Notiz gespeichert |

## 3. Beobachtungsbogen (pro Testperson)

Testperson: ______ · Alter: ___ · Gerät: Laptop / Smartphone · Datum: ______

| Aufgabe | Gelöst (ja / mit Mühe / nein) | Zeit (min) | Fehlversuche / Umwege | Zitate und Beobachtungen |
|---|---|---|---|---|
| A1 | | | | |
| A2 | | | | |
| A3 | | | | |
| A4 | | | | |
| A5 | | | | |
| A6 | | | | |
| A7 | | | | |

Schweregrad gefundener Probleme:

- **3:** Aufgabe nicht lösbar.
- **2:** Aufgabe nur mit großer Mühe lösbar.
- **1:** Kleine Irritation.

## 4. Fragebogen nach dem Test

Bewertung von 1 (trifft gar nicht zu) bis 5 (trifft voll zu):

| Bereich | Aussage | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| Intuitivität | Ich wusste jederzeit, was ich als Nächstes tun muss. | | | | | |
| Intuitivität | Die Begriffe und Beschriftungen waren verständlich. | | | | | |
| Struktur | Ich habe die gesuchten Funktionen schnell gefunden. | | | | | |
| Struktur | Die Aufteilung der Seiten war übersichtlich. | | | | | |
| Visueller Eindruck | Die Gestaltung wirkt ansprechend und vertrauenswürdig. | | | | | |
| Visueller Eindruck | Statusanzeigen und Farben waren eindeutig. | | | | | |
| Performance | Die Anwendung reagierte schnell genug. | | | | | |
| Gesamt | Ich würde FixMyCity nutzen, um einen Schaden zu melden. | | | | | |

Offene Fragen:

1. Was hat Ihnen am besten gefallen?
2. Wo sind Sie hängen geblieben?
3. Was würden Sie als Erstes ändern?

## 5. Auswertung (Vorlage für den Bericht)

| Problem | Aufgabe | Betroffene Personen (x/5) | Schweregrad | Maßnahme | Umgesetzt |
|---|---|---|---|---|---|
| | | | | | |

Hinweise aus den automatisierten Tests, auf die bei der Beobachtung geachtet werden sollte:

- **Englische Fehlermeldungen:** Werden Fehlermeldungen in englischer Sprache verstanden (z. B. bei zu kurzem Passwort)?
- **Startposition der Karte:** Die Karte startet auf Berlin. Finden Testpersonen ihren Standort?
