package com.stadtmeldung.controller;

import com.jayway.jsonpath.JsonPath;
import com.stadtmeldung.service.ImageDataService;
import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import repository.interfaces.ReportRepository;
import service.interfaces.ReportService;
import service.interfaces.UserService;
import testsupport.ApiClient;
import testsupport.TestDatabase;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API-Tests: Das komplette Backend (Spring Boot, Spring Security, Controller,
 * Services, SQLite-Testdatenbank) wird gestartet. MockMvc schickt echte
 * HTTP-Anfragen, ohne dass ein Server-Port oder Browser nötig ist.
 *
 * Geprüft werden HTTP-Statuscodes, JSON-Antworten, CSRF-Schutz und
 * die Berechtigungen der Rollen anonym / Bürger / Mitarbeiter / Admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("REST-API (MockMvc)")
class ApiIntegrationTest {

    static {
        // Muss vor dem Start von Spring passieren, da beim Start die Tabelle users gelesen wird
        TestDatabase.init();
    }

    private static final String PASSWORD = "test-passwort-123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ImageDataService imageDataService;

    private User admin;

    @BeforeAll
    void createUsers() {
        TestDatabase.clearAll();
        userService.registerCitizen("buerger", PASSWORD);
        userService.registerCitizen("buergerin", PASSWORD);
        admin = userService.provisionAdmin("chef", PASSWORD);
        userService.createEmployee(admin.getId(), "mitarbeiter", PASSWORD);
    }

    @BeforeEach
    void clearReports() {
        TestDatabase.clearReports();
    }

    private ApiClient anonymous() {
        return ApiClient.anonymous(mvc);
    }

    private ApiClient as(String username) throws Exception {
        return ApiClient.loggedIn(mvc, username, PASSWORD);
    }

    private static String reportJson(String category, String defectType, double lat, double lng) {
        return """
                {"title":"Schlagloch","category":"%s","defectType":"%s",
                 "description":"Tiefes Loch vor Hausnummer 5","latitude":%s,"longitude":%s}
                """.formatted(category, defectType, lat, lng);
    }

    private static final String VALID_REPORT = reportJson("Defekt", "POTHOLE", 49.4875, 8.4660);

    private Report existingReport(Status status) {
        Report report = reportService.createReport(null, "Müll", Category.LITTER,
                "", "Sperrmüll", "", 49.48, 8.46);
        if (status != Status.NEW) {
            report.setStatus(status);
            reportRepository.update(report);
        }
        return report;
    }

    @Nested
    @DisplayName("Öffentlicher Zugriff ohne Anmeldung")
    class Anonymous {

        @Test
        @DisplayName("GET /api/health → 200 {status: ok}")
        void health() throws Exception {
            anonymous().get("/api/health")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"));
        }

        @Test
        @DisplayName("Meldungsliste ist öffentlich lesbar")
        void reportsArePublic() throws Exception {
            existingReport(Status.NEW);

            anonymous().get("/api/reports")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("new"))
                    .andExpect(jsonPath("$[0].hasVoted").value(false));
        }

        @Test
        @DisplayName("anonyme Meldung wird mit Status 201 angelegt")
        void anonymousCanCreateReport() throws Exception {
            anonymous().post("/api/reports", VALID_REPORT)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.report.id").isNumber())
                    .andExpect(jsonPath("$.report.category").value("Defekt"))
                    .andExpect(jsonPath("$.report.status").value("new"))
                    .andExpect(jsonPath("$.report.submittedBy").doesNotExist());
        }

        @Test
        @DisplayName("Anfrage ohne CSRF-Token wird mit 403 abgewiesen")
        void csrfProtection() throws Exception {
            anonymous().postWithoutCsrf("/api/reports", VALID_REPORT)
                    .andExpect(status().isForbidden());

            assertTrue(reportService.findAll().isEmpty(), "es darf keine Meldung entstehen");
        }

        @Test
        @DisplayName("ungültige Kategorie → 400 mit Fehlermeldung")
        void invalidCategory() throws Exception {
            anonymous().post("/api/reports", reportJson("Erdbeben", "", 49.0, 8.0))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Ungültige Kategorie."));
        }

        @Test
        @DisplayName("ungültige Koordinaten → 400")
        void invalidCoordinates() throws Exception {
            anonymous().post("/api/reports", reportJson("Defekt", "POTHOLE", 91.0, 8.0))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("ohne Anmeldung: Sitzung abfragen → 401")
        void sessionRequiresLogin() throws Exception {
            anonymous().get("/api/session")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Bitte melden Sie sich an."));
        }

        @Test
        @DisplayName("ohne Anmeldung: Status ändern und Mitarbeiterverwaltung gesperrt (401)")
        void protectedEndpoints() throws Exception {
            Report report = existingReport(Status.NEW);

            anonymous().patch("/api/reports/" + report.getId(), "{\"status\":\"resolved\"}")
                    .andExpect(status().isUnauthorized());
            anonymous().get("/api/admin/employees")
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("„Meine Meldungen“ erfordert ein Bürgerkonto")
        void mineRequiresCitizen() throws Exception {
            anonymous().get("/api/reports?mine=true")
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Registrierung, Login und Logout")
    class Authentication {

        @Test
        @DisplayName("Registrierung legt Bürgerkonto an und meldet direkt an")
        void register() throws Exception {
            ApiClient client = anonymous();

            client.post("/api/register", ApiClient.credentials("Neu.Buerger", PASSWORD))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.username").value("neu.buerger"))
                    .andExpect(jsonPath("$.user.role").value("user"));
            client.get("/api/session")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("neu.buerger"));
        }

        @Test
        @DisplayName("Registrierung mit zu kurzem Passwort → 400")
        void registerWeakPassword() throws Exception {
            anonymous().post("/api/register", ApiClient.credentials("kurz", "123"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Registrierung mit vergebenem Namen → 400")
        void registerDuplicate() throws Exception {
            anonymous().post("/api/register", ApiClient.credentials("buerger", PASSWORD))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Login mit falschem Passwort → 401 mit neutraler Meldung")
        void loginWrongPassword() throws Exception {
            anonymous().post("/api/login", ApiClient.credentials("buerger", "falsches-passwort"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Benutzername oder Passwort ungültig."));
        }

        @Test
        @DisplayName("Login mit leerem Feld → 400")
        void loginEmpty() throws Exception {
            anonymous().post("/api/login", ApiClient.credentials("", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Login liefert Rolle; nach Logout ist die Sitzung beendet")
        void loginAndLogout() throws Exception {
            ApiClient staff = as("mitarbeiter");
            staff.get("/api/session")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.role").value("staff"));

            staff.post("/api/logout").andExpect(status().isNoContent());

            staff.get("/api/session").andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Rechte von Bürgern und Mitarbeitenden")
    class Permissions {

        @Test
        @DisplayName("Bürger darf den Status nicht ändern (403)")
        void citizenCannotChangeStatus() throws Exception {
            Report report = existingReport(Status.NEW);

            as("buerger").patch("/api/reports/" + report.getId(), "{\"status\":\"resolved\"}")
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Mitarbeiter ändert Status auf erledigt (200)")
        void staffChangesStatus() throws Exception {
            Report report = existingReport(Status.NEW);

            as("mitarbeiter").patch("/api/reports/" + report.getId(), "{\"status\":\"resolved\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.report.status").value("resolved"));
        }

        @Test
        @DisplayName("Status einer unbekannten Meldung ändern → 404")
        void unknownReport() throws Exception {
            as("mitarbeiter").patch("/api/reports/999999", "{\"status\":\"resolved\"}")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ungültiger Statuswert → 400")
        void invalidStatus() throws Exception {
            Report report = existingReport(Status.NEW);

            as("mitarbeiter").patch("/api/reports/" + report.getId(), "{\"status\":\"fertig\"}")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("nicht erledigte Meldung löschen → 409, erledigte → 204")
        void deleteOnlyCompleted() throws Exception {
            Report open = existingReport(Status.IN_PROGRESS);
            Report done = existingReport(Status.COMPLETED);
            ApiClient staff = as("mitarbeiter");

            staff.delete("/api/reports/" + open.getId()).andExpect(status().isConflict());
            staff.delete("/api/reports/" + done.getId()).andExpect(status().isNoContent());

            assertNull(reportService.findById(done.getId()));
        }

        @Test
        @DisplayName("Bürger darf Meldungen nicht löschen (403)")
        void citizenCannotDelete() throws Exception {
            Report done = existingReport(Status.COMPLETED);

            as("buerger").delete("/api/reports/" + done.getId())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Mitarbeitende können keine Meldungen einreichen (403)")
        void staffCannotCreateReport() throws Exception {
            as("mitarbeiter").post("/api/reports", VALID_REPORT)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("„Meine Meldungen“ zeigt nur eigene Meldungen des Bürgers")
        void mineShowsOnlyOwnReports() throws Exception {
            as("buerger").post("/api/reports", VALID_REPORT).andExpect(status().isCreated());
            as("buergerin").post("/api/reports", VALID_REPORT).andExpect(status().isCreated());

            as("buerger").get("/api/reports?mine=true")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].submittedByMe").value(true));
        }
    }

    @Nested
    @DisplayName("Abstimmen")
    class Voting {

        @Test
        @DisplayName("Bürger stimmt ab (201), zweite Stimme → 409, Rücknahme → 200")
        void voteLifecycle() throws Exception {
            Report report = existingReport(Status.NEW);
            ApiClient citizen = as("buerger");
            String url = "/api/reports/" + report.getId() + "/votes";

            citizen.post(url)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.voteCount").value(1))
                    .andExpect(jsonPath("$.hasVoted").value(true));
            citizen.post(url).andExpect(status().isConflict());
            citizen.delete(url)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteCount").value(0));
        }

        @Test
        @DisplayName("Mitarbeiter dürfen nicht abstimmen (403)")
        void staffCannotVote() throws Exception {
            Report report = existingReport(Status.NEW);

            as("mitarbeiter").post("/api/reports/" + report.getId() + "/votes")
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Kommentare")
    class Comments {

        @Test
        @DisplayName("interne Kommentare sieht nur das Stadt-Team, nicht Bürger oder Besucher")
        void internalCommentsAreHiddenFromPublic() throws Exception {
            Report report = existingReport(Status.NEW);
            String url = "/api/reports/" + report.getId() + "/comments";

            as("buerger").post(url, "{\"text\":\"Wird immer größer\"}")
                    .andExpect(status().isCreated());
            as("mitarbeiter").post(url + "/internal", "{\"text\":\"Bauhof beauftragt\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.comment.internal").value(true));

            anonymous().get(url)
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].text").value("Wird immer größer"));
            as("buergerin").get(url)
                    .andExpect(jsonPath("$", hasSize(1)));
            as("mitarbeiter").get(url)
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Bürger darf keine internen Kommentare schreiben (403)")
        void citizenCannotWriteInternal() throws Exception {
            Report report = existingReport(Status.NEW);

            as("buerger").post("/api/reports/" + report.getId() + "/comments/internal",
                            "{\"text\":\"Hallo\"}")
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Kommentare einer unbekannten Meldung → 404")
        void unknownReport() throws Exception {
            anonymous().get("/api/reports/999999/comments")
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Mitarbeiterverwaltung (Admin)")
    class Administration {

        @Test
        @DisplayName("Admin legt Mitarbeiter an, dieser kann sich danach anmelden")
        void adminCreatesEmployee() throws Exception {
            as("chef").post("/api/admin/employees", ApiClient.credentials("neuer.kollege", PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employee.username").value("neuer.kollege"))
                    .andExpect(jsonPath("$.employee.role").value("staff"));

            as("neuer.kollege").get("/api/session")
                    .andExpect(jsonPath("$.user.role").value("staff"));
        }

        @Test
        @DisplayName("Mitarbeiter und Bürger haben keinen Zugriff auf die Verwaltung (403)")
        void nonAdminsAreForbidden() throws Exception {
            as("mitarbeiter").get("/api/admin/employees").andExpect(status().isForbidden());
            as("buerger").post("/api/admin/employees", ApiClient.credentials("hacker", PASSWORD))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Löschen eines Mitarbeiters beendet dessen aktive Sitzung sofort")
        void deletingEmployeeEndsSession() throws Exception {
            User employee = userService.createEmployee(admin.getId(), "wird.geloescht", PASSWORD);
            ApiClient employeeClient = as("wird.geloescht");
            employeeClient.get("/api/session").andExpect(status().isOk());

            as("chef").delete("/api/admin/employees/" + employee.getId())
                    .andExpect(status().isOk());

            employeeClient.get("/api/session").andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Foto-Upload")
    class PhotoUpload {

        // kleinstmögliches gültiges PNG (1×1 Pixel)
        private static final String PNG_1PX = "data:image/png;base64,"
                + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

        private String reportWithPhoto(String photoData) {
            return """
                    {"title":"Foto","category":"Sonstiges","defectType":"","description":"Mit Bild",
                     "photoData":"%s","latitude":49.0,"longitude":8.0}
                    """.formatted(photoData);
        }

        @Test
        @DisplayName("gültiges PNG wird gespeichert und ist der Meldung zugeordnet")
        void validPhoto() throws Exception {
            String body = anonymous().post("/api/reports", reportWithPhoto(PNG_1PX))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.report.photo").value(startsWith("/uploads/")))
                    .andReturn().getResponse().getContentAsString();

            String photoPath = JsonPath.read(body, "$.report.photo");
            Path file = Path.of(photoPath.substring(1));
            assertTrue(Files.exists(file), "Datei muss im uploads-Ordner liegen");
            imageDataService.deleteSavedFile(photoPath); // aufräumen
        }

        @Test
        @DisplayName("andere Dateitypen (z. B. Text/HTML) werden abgelehnt (400)")
        void rejectsNonImages() throws Exception {
            anonymous().post("/api/reports", reportWithPhoto("data:text/html;base64,PHNjcmlwdD4="))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Nur JPG-, PNG-, GIF- oder WEBP-Fotos sind erlaubt."));

            assertTrue(reportService.findAll().isEmpty());
        }

        @Test
        @DisplayName("bei ungültiger Meldung wird das bereits gespeicherte Foto wieder gelöscht")
        void photoIsRemovedWhenReportInvalid() throws Exception {
            long filesBefore = countUploads();
            String invalid = """
                    {"title":"","category":"Sonstiges","defectType":"","description":"x",
                     "photoData":"%s","latitude":49.0,"longitude":8.0}
                    """.formatted(PNG_1PX);

            anonymous().post("/api/reports", invalid).andExpect(status().isBadRequest());

            assertFalse(countUploads() > filesBefore, "kein verwaistes Foto im uploads-Ordner");
        }

        private long countUploads() throws Exception {
            Path uploads = Path.of("uploads");
            if (!Files.exists(uploads)) {
                return 0;
            }
            try (var files = Files.list(uploads)) {
                return files.count();
            }
        }
    }
}
