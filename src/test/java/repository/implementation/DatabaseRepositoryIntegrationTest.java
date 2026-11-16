package repository.implementation;

import database.connection.ConnectionProvider;
import database.connection.DatabaseConnectionProvider;
import model.Attachment;
import model.Comment;
import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Role;
import model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import repository.RepositoryException;
import repository.implementation.mapper.AttachmentResultSetMapper;
import repository.implementation.mapper.CommentResultSetMapper;
import repository.implementation.mapper.ReportResultSetMapper;
import repository.implementation.mapper.UserResultSetMapper;
import testsupport.TestDatabase;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integrationstests der Repositories gegen eine echte SQLite-Testdatenbank.
 *
 * Im Gegensatz zu den Mockito-Tests wird hier geprüft, ob die SQL-Befehle,
 * das Datenbankschema und die Fremdschlüssel wirklich funktionieren.
 */
@DisplayName("Repositories (SQLite-Integration)")
class DatabaseRepositoryIntegrationTest {

    private final ConnectionProvider connections = new DatabaseConnectionProvider();
    private final DatabaseUserRepository users =
            new DatabaseUserRepository(connections, new UserResultSetMapper());
    private final DatabaseReportRepository reports =
            new DatabaseReportRepository(connections, new ReportResultSetMapper());
    private final DatabaseCommentRepository comments =
            new DatabaseCommentRepository(connections, new CommentResultSetMapper());
    private final DatabaseReportVoteRepository votes =
            new DatabaseReportVoteRepository(connections);
    private final DatabaseAttachmentRepository attachments =
            new DatabaseAttachmentRepository(connections, new AttachmentResultSetMapper());

    @BeforeEach
    void cleanDatabase() {
        TestDatabase.clearAll();
    }

    private User savedUser(String username, Role role) {
        User user = new User(username, username + "@example.de", "hash", role);
        user.setUsername(username);
        users.save(user);
        return user;
    }

    private Report savedReport(Long ownerId) {
        Report report = new Report(ownerId, "Schlagloch", Category.POTHOLE,
                "POTHOLE", "Tiefes Loch", "", 49.48, 8.46);
        reports.save(report);
        return report;
    }

    @Nested
    @DisplayName("Benutzer")
    class Users {

        @Test
        @DisplayName("gespeicherter Benutzer erhält ID und ist per ID, Name und E-Mail auffindbar")
        void saveAndFind() {
            User saved = savedUser("max", Role.CITIZEN);

            assertNotNull(saved.getId());
            User byId = users.findById(saved.getId());
            assertEquals("max", byId.getUsername());
            assertEquals(Role.CITIZEN, byId.getRole());
            assertNotNull(byId.getCreatedAt());
            assertEquals(saved.getId(), users.findByUsername("max").getId());
            assertEquals(saved.getId(), users.findByEmail("max@example.de").getId());
        }

        @Test
        @DisplayName("unbekannter Benutzer liefert null")
        void unknownUserIsNull() {
            assertNull(users.findById(999));
            assertNull(users.findByUsername("niemand"));
        }

        @Test
        @DisplayName("Datenbank verhindert doppelte Benutzernamen")
        void usernameIsUnique() {
            savedUser("max", Role.CITIZEN);

            User duplicate = new User("Max 2", "andere@example.de", "hash", Role.CITIZEN);
            duplicate.setUsername("max");

            assertThrows(RepositoryException.class, () -> users.save(duplicate));
        }

        @Test
        @DisplayName("Rolle und Name lassen sich ändern")
        void update() {
            User user = savedUser("max", Role.CITIZEN);
            user.setName("Max Mustermann");
            user.setRole(Role.EMPLOYEE);

            users.update(user);

            User reloaded = users.findById(user.getId());
            assertEquals("Max Mustermann", reloaded.getName());
            assertEquals(Role.EMPLOYEE, reloaded.getRole());
        }

        @Test
        @DisplayName("Löschen eines Benutzers behält seine Meldungen, aber anonymisiert (ON DELETE SET NULL)")
        void deletingUserKeepsReportsAnonymously() {
            User user = savedUser("max", Role.CITIZEN);
            Report report = savedReport(user.getId());

            users.deleteById(user.getId());

            assertNull(users.findById(user.getId()));
            Report reloaded = reports.findById(report.getId());
            assertNotNull(reloaded, "Meldung muss erhalten bleiben");
            assertNull(reloaded.getUserId(), "Bezug zum gelöschten Benutzer muss entfernt sein");
        }
    }

    @Nested
    @DisplayName("Meldungen")
    class Reports {

        @Test
        @DisplayName("gespeicherte Meldung wird vollständig wieder geladen")
        void saveAndFind() {
            User owner = savedUser("max", Role.CITIZEN);
            Report saved = savedReport(owner.getId());

            Report loaded = reports.findById(saved.getId());

            assertEquals("Schlagloch", loaded.getTitle());
            assertEquals(Category.POTHOLE, loaded.getCategory());
            assertEquals("POTHOLE", loaded.getDefectType());
            assertEquals(Status.NEW, loaded.getStatus());
            assertEquals(49.48, loaded.getLatitude(), 0.000001);
            assertEquals(8.46, loaded.getLongitude(), 0.000001);
            assertEquals(owner.getId(), loaded.getUserId());
            assertNotNull(loaded.getCreatedAt());
        }

        @Test
        @DisplayName("anonyme Meldung ohne Benutzer wird gespeichert")
        void anonymousReport() {
            Report saved = savedReport(null);

            assertNull(reports.findById(saved.getId()).getUserId());
        }

        @Test
        @DisplayName("Statusänderung wird gespeichert und Filter nach Status funktioniert")
        void updateStatusAndFilter() {
            Report first = savedReport(null);
            Report second = savedReport(null);
            second.setStatus(Status.COMPLETED);

            reports.update(second);

            assertEquals(Status.COMPLETED, reports.findById(second.getId()).getStatus());
            assertEquals(List.of(first.getId()),
                    reports.findByStatus(Status.NEW).stream().map(Report::getId).toList());
            assertEquals(List.of(second.getId()),
                    reports.findByStatus(Status.COMPLETED).stream().map(Report::getId).toList());
            assertTrue(reports.findByStatus(Status.IN_PROGRESS).isEmpty());
        }

        @Test
        @DisplayName("neueste Meldung steht in der Liste oben")
        void findAllNewestFirst() {
            Report older = savedReport(null);
            Report newer = savedReport(null);

            assertEquals(List.of(newer.getId(), older.getId()),
                    reports.findAll().stream().map(Report::getId).toList());
        }

        @Test
        @DisplayName("Aktualisieren einer nicht existierenden Meldung schlägt fehl")
        void updateUnknownReportFails() {
            Report ghost = savedReport(null);
            reports.deleteById(ghost.getId());

            assertThrows(RepositoryException.class, () -> reports.update(ghost));
        }

        @Test
        @DisplayName("Löschen einer Meldung entfernt auch Kommentare, Stimmen und Anhänge (CASCADE)")
        void deleteCascades() {
            User citizen = savedUser("max", Role.CITIZEN);
            Report report = savedReport(citizen.getId());
            comments.save(new Comment(report.getId(), citizen.getId(), "Update"));
            votes.addVote(report.getId(), citizen.getId());
            attachments.save(new Attachment(report.getId(), "/uploads/a.png"));

            reports.deleteById(report.getId());

            assertNull(reports.findById(report.getId()));
            assertTrue(comments.findByReportId(report.getId()).isEmpty());
            assertEquals(0, votes.countByReportId(report.getId()));
            assertTrue(attachments.findByReportId(report.getId()).isEmpty());
        }
    }

    @Nested
    @DisplayName("Stimmen")
    class Votes {

        @Test
        @DisplayName("jeder Bürger kann pro Meldung nur einmal stimmen")
        void oneVotePerCitizen() {
            User citizen = savedUser("max", Role.CITIZEN);
            Report report = savedReport(null);

            assertTrue(votes.addVote(report.getId(), citizen.getId()));
            assertFalse(votes.addVote(report.getId(), citizen.getId()));
            assertEquals(1, votes.countByReportId(report.getId()));
            assertTrue(votes.existsByReportIdAndUserId(report.getId(), citizen.getId()));
        }

        @Test
        @DisplayName("Stimmen mehrerer Bürger werden gezählt und lassen sich zurücknehmen")
        void countAndRemove() {
            User anna = savedUser("anna", Role.CITIZEN);
            User ben = savedUser("ben", Role.CITIZEN);
            Report report = savedReport(null);
            votes.addVote(report.getId(), anna.getId());
            votes.addVote(report.getId(), ben.getId());

            assertEquals(2, votes.countByReportId(report.getId()));
            assertTrue(votes.removeVote(report.getId(), anna.getId()));
            assertFalse(votes.removeVote(report.getId(), anna.getId()));
            assertEquals(1, votes.countByReportId(report.getId()));
            assertEquals(Set.of(report.getId()), votes.findReportIdsByUserId(ben.getId()));
        }

        @Test
        @DisplayName("Stimme für nicht existierende Meldung wird von der Datenbank abgelehnt (Fremdschlüssel)")
        void foreignKeyIsEnforced() {
            User citizen = savedUser("max", Role.CITIZEN);

            assertThrows(RepositoryException.class, () -> votes.addVote(999, citizen.getId()));
        }
    }

    @Nested
    @DisplayName("Kommentare und Anhänge")
    class CommentsAndAttachments {

        @Test
        @DisplayName("interner Kommentar bleibt nach dem Speichern als intern markiert")
        void internalFlagIsPersisted() {
            User employee = savedUser("worker", Role.EMPLOYEE);
            Report report = savedReport(null);
            Comment internal = new Comment(report.getId(), employee.getId(), "Intern", "", true);

            comments.save(internal);

            assertTrue(comments.findById(internal.getId()).isInternal());
        }

        @Test
        @DisplayName("Kommentare einer Meldung werden in zeitlicher Reihenfolge geladen")
        void commentsInChronologicalOrder() {
            User citizen = savedUser("max", Role.CITIZEN);
            Report report = savedReport(null);
            comments.save(new Comment(report.getId(), citizen.getId(), "Erstes"));
            comments.save(new Comment(report.getId(), citizen.getId(), "Zweites"));

            assertEquals(List.of("Erstes", "Zweites"),
                    comments.findByReportId(report.getId()).stream().map(Comment::getText).toList());
            assertEquals(Set.of(report.getId()), comments.findReportIdsByUserId(citizen.getId()));
        }

        @Test
        @DisplayName("Anhänge werden gespeichert und gemeinsam pro Meldung gelöscht")
        void attachments() {
            Report report = savedReport(null);
            Attachment attachment = new Attachment(report.getId(), "/uploads/a.png");
            attachments.save(attachment);
            attachments.save(new Attachment(report.getId(), "/uploads/b.png"));

            assertEquals("/uploads/a.png", attachments.findById(attachment.getId()).getFilePath());
            assertEquals(2, attachments.findByReportId(report.getId()).size());

            attachments.deleteByReportId(report.getId());

            assertTrue(attachments.findByReportId(report.getId()).isEmpty());
        }
    }
}
