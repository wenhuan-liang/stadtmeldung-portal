package service.implementation;

import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Role;
import model.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.interfaces.ReportRepository;
import repository.interfaces.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link ReportServiceImpl}: Erstellen, Bearbeiten,
 * Statusänderung und Löschen von Meldungen inklusive Rechteprüfung.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService")
class ReportServiceImplTest {

    // Koordinaten in Mannheim
    private static final double LAT = 49.4875;
    private static final double LNG = 8.4660;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private static User user(long id, Role role) {
        User user = new User("user" + id, "user" + id, "hash", role);
        user.setId(id);
        return user;
    }

    private static Report report(long id, Long ownerId, Status status) {
        Report report = new Report(ownerId, "Schlagloch", Category.POTHOLE,
                "POTHOLE", "Tiefes Loch", "", LAT, LNG);
        report.setId(id);
        report.setStatus(status);
        return report;
    }

    @Nested
    @DisplayName("Meldung erstellen")
    class CreateReport {

        @Test
        @DisplayName("speichert gültige Meldung mit Status NEU und gekürzten Texten")
        void createsValidReport() {
            when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));

            Report result = reportService.createReport(1L, "  Schlagloch  ", Category.POTHOLE,
                    "POTHOLE", "  Großes Loch vor Hausnummer 5  ", null, LAT, LNG);

            verify(reportRepository).save(result);
            assertEquals(Status.NEW, result.getStatus());
            assertEquals("Schlagloch", result.getTitle());
            assertEquals("Großes Loch vor Hausnummer 5", result.getDescription());
            assertEquals("", result.getPhotoPath());
            assertEquals(1L, result.getUserId());
        }

        @Test
        @DisplayName("anonyme Meldung ohne Benutzerkonto ist erlaubt")
        void anonymousReportIsAllowed() {
            Report result = reportService.createReport(null, "Müll", Category.LITTER,
                    "", "Sperrmüll am Straßenrand", "", LAT, LNG);

            assertNull(result.getUserId());
            verify(reportRepository).save(result);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("lehnt Meldung eines unbekannten Benutzers ab")
        void rejectsUnknownUser() {
            when(userRepository.findById(42L)).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(42L, "Müll", Category.LITTER,
                            "", "Text", "", LAT, LNG));
            verify(reportRepository, never()).save(any());
        }

        @ParameterizedTest(name = "ungültige Koordinaten: lat={0}, lng={1}")
        @CsvSource({
                "90.0001, 0", "-90.0001, 0", "0, 180.0001", "0, -180.0001",
                "NaN, 0", "0, NaN", "Infinity, 0", "0, -Infinity"
        })
        @DisplayName("lehnt Koordinaten außerhalb des gültigen Bereichs ab")
        void rejectsInvalidCoordinates(double latitude, double longitude) {
            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "Müll", Category.LITTER,
                            "", "Text", "", latitude, longitude));
            verify(reportRepository, never()).save(any());
        }

        @ParameterizedTest(name = "gültige Grenzkoordinaten: lat={0}, lng={1}")
        @CsvSource({"90, 180", "-90, -180", "0, 0"})
        @DisplayName("akzeptiert Koordinaten genau an den Grenzen")
        void acceptsBoundaryCoordinates(double latitude, double longitude) {
            reportService.createReport(null, "Müll", Category.LITTER,
                    "", "Text", "", latitude, longitude);

            verify(reportRepository).save(any(Report.class));
        }

        @Test
        @DisplayName("Titel: 120 Zeichen erlaubt, 121 Zeichen abgelehnt, leer abgelehnt")
        void titleLengthLimits() {
            reportService.createReport(null, "t".repeat(120), Category.LITTER,
                    "", "Text", "", LAT, LNG);

            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "t".repeat(121), Category.LITTER,
                            "", "Text", "", LAT, LNG));
            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "   ", Category.LITTER,
                            "", "Text", "", LAT, LNG));
            verify(reportRepository).save(any(Report.class));
        }

        @Test
        @DisplayName("Beschreibung: 2000 Zeichen erlaubt, 2001 Zeichen abgelehnt, leer abgelehnt")
        void descriptionLengthLimits() {
            reportService.createReport(null, "Titel", Category.LITTER,
                    "", "d".repeat(2000), "", LAT, LNG);

            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "Titel", Category.LITTER,
                            "", "d".repeat(2001), "", LAT, LNG));
            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "Titel", Category.LITTER,
                            "", " ", "", LAT, LNG));
            verify(reportRepository).save(any(Report.class));
        }

        @Test
        @DisplayName("lehnt Meldung ohne Kategorie ab")
        void rejectsMissingCategory() {
            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "Titel", null,
                            "", "Text", "", LAT, LNG));
        }

        @ParameterizedTest(name = "Defektart {0} ist gültig")
        @ValueSource(strings = {"POTHOLE", "ROAD_SURFACE", "SIDEWALK", "STREETLIGHT",
                "TRAFFIC_SIGN", "BENCH_OR_PLAYGROUND", "OTHER_DEFECT"})
        @DisplayName("Kategorie Defekt akzeptiert jede der sieben Defektarten")
        void acceptsAllDefectTypes(String defectType) {
            Report result = reportService.createReport(null, "Defekt", Category.POTHOLE,
                    defectType, "Text", "", LAT, LNG);

            assertEquals(defectType, result.getDefectType());
        }

        @ParameterizedTest(name = "Defektart \"{0}\" ist ungültig")
        @ValueSource(strings = {"", "pothole", "UNBEKANNT"})
        @DisplayName("Kategorie Defekt verlangt eine konkrete, gültige Defektart")
        void rejectsMissingOrUnknownDefectType(String defectType) {
            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "Defekt", Category.POTHOLE,
                            defectType, "Text", "", LAT, LNG));
        }

        @ParameterizedTest(name = "Kategorie {0} darf keine Defektart haben")
        @EnumSource(value = Category.class, names = "POTHOLE", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("andere Kategorien dürfen keine Defektart haben")
        void otherCategoriesRejectDefectType(Category category) {
            assertThrows(IllegalArgumentException.class,
                    () -> reportService.createReport(null, "Titel", category,
                            "POTHOLE", "Text", "", LAT, LNG));
        }
    }

    @Nested
    @DisplayName("Status ändern")
    class UpdateStatus {

        @ParameterizedTest(name = "Rolle {0} darf den Status ändern")
        @EnumSource(value = Role.class, names = {"EMPLOYEE", "ADMIN"})
        @DisplayName("Mitarbeiter und Admins dürfen den Status ändern")
        void staffCanUpdateStatus(Role role) {
            Report report = report(10, 1L, Status.NEW);
            when(userRepository.findById(2L)).thenReturn(user(2, role));
            when(reportRepository.findById(10L)).thenReturn(report);

            reportService.updateStatus(10L, Status.IN_PROGRESS, 2L);

            assertEquals(Status.IN_PROGRESS, report.getStatus());
            verify(reportRepository).update(report);
        }

        @Test
        @DisplayName("Bürger darf den Status nicht ändern – auch nicht bei eigener Meldung")
        void citizenCannotUpdateStatus() {
            when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));

            assertThrows(SecurityException.class,
                    () -> reportService.updateStatus(10L, Status.COMPLETED, 1L));
            verify(reportRepository, never()).update(any());
        }

        @Test
        @DisplayName("unbekannte Meldung führt zu Fehler")
        void unknownReport() {
            when(userRepository.findById(2L)).thenReturn(user(2, Role.EMPLOYEE));
            when(reportRepository.findById(99L)).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> reportService.updateStatus(99L, Status.COMPLETED, 2L));
        }

        @Test
        @DisplayName("Status darf nicht leer sein")
        void statusMustNotBeNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> reportService.updateStatus(10L, null, 2L));
            verifyNoInteractions(reportRepository);
        }
    }

    @Nested
    @DisplayName("Meldung bearbeiten (Bürger)")
    class UpdateReport {

        @Test
        @DisplayName("Eigentümer kann Meldung bearbeiten, Status und Eigentümer bleiben erhalten")
        void ownerCanEditButNotChangeStatusOrOwner() {
            when(reportRepository.findById(10L)).thenReturn(report(10, 1L, Status.IN_PROGRESS));

            // Manipulationsversuch: Status auf erledigt und fremden Eigentümer setzen
            Report changed = report(10, 1L, Status.COMPLETED);
            changed.setUserId(999L);
            changed.setDescription("  Neue Beschreibung  ");

            reportService.updateReport(changed, 1L);

            ArgumentCaptor<Report> updated = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).update(updated.capture());
            assertEquals(Status.IN_PROGRESS, updated.getValue().getStatus());
            assertEquals(1L, updated.getValue().getUserId());
            assertEquals("Neue Beschreibung", updated.getValue().getDescription());
        }

        @Test
        @DisplayName("fremde Meldung kann nicht bearbeitet werden")
        void otherUserCannotEdit() {
            when(reportRepository.findById(10L)).thenReturn(report(10, 1L, Status.NEW));

            assertThrows(SecurityException.class,
                    () -> reportService.updateReport(report(10, 1L, Status.NEW), 2L));
            verify(reportRepository, never()).update(any());
        }

        @Test
        @DisplayName("anonyme Meldung kann von niemandem bearbeitet werden")
        void anonymousReportCannotBeEdited() {
            when(reportRepository.findById(10L)).thenReturn(report(10, null, Status.NEW));

            assertThrows(SecurityException.class,
                    () -> reportService.updateReport(report(10, null, Status.NEW), 1L));
            verify(reportRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Meldung löschen")
    class DeleteReport {

        @Test
        @DisplayName("Eigentümer kann eigene Meldung löschen")
        void ownerCanDelete() {
            when(reportRepository.findById(10L)).thenReturn(report(10, 1L, Status.NEW));

            reportService.deleteReport(10L, 1L);

            verify(reportRepository).deleteById(10L);
        }

        @Test
        @DisplayName("fremde Meldung kann nicht gelöscht werden")
        void otherUserCannotDelete() {
            when(reportRepository.findById(10L)).thenReturn(report(10, 1L, Status.NEW));

            assertThrows(SecurityException.class, () -> reportService.deleteReport(10L, 2L));
            verify(reportRepository, never()).deleteById(anyLong());
        }

        @ParameterizedTest(name = "Rolle {0} darf erledigte Meldungen löschen")
        @EnumSource(value = Role.class, names = {"EMPLOYEE", "ADMIN"})
        @DisplayName("Mitarbeiter und Admins dürfen erledigte Meldungen löschen")
        void staffCanDeleteCompletedReport(Role role) {
            when(userRepository.findById(2L)).thenReturn(user(2, role));
            when(reportRepository.findById(10L)).thenReturn(report(10, 1L, Status.COMPLETED));

            reportService.deleteCompletedReport(10L, 2L);

            verify(reportRepository).deleteById(10L);
        }

        @ParameterizedTest(name = "Meldung mit Status {0} darf nicht gelöscht werden")
        @EnumSource(value = Status.class, names = "COMPLETED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("nicht erledigte Meldungen dürfen nicht gelöscht werden")
        void cannotDeleteUnfinishedReport(Status status) {
            when(userRepository.findById(2L)).thenReturn(user(2, Role.EMPLOYEE));
            when(reportRepository.findById(10L)).thenReturn(report(10, 1L, status));

            assertThrows(IllegalStateException.class,
                    () -> reportService.deleteCompletedReport(10L, 2L));
            verify(reportRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Bürger darf erledigte Meldungen nicht über die Mitarbeiterfunktion löschen")
        void citizenCannotDeleteCompletedReport() {
            when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));

            assertThrows(SecurityException.class,
                    () -> reportService.deleteCompletedReport(10L, 1L));
            verify(reportRepository, never()).deleteById(anyLong());
        }

        @ParameterizedTest(name = "ID {0} ist ungültig")
        @ValueSource(longs = {0, -1})
        @DisplayName("IDs kleiner 1 werden abgelehnt")
        void rejectsInvalidIds(long id) {
            assertThrows(IllegalArgumentException.class, () -> reportService.deleteReport(id, 1L));
            assertThrows(IllegalArgumentException.class, () -> reportService.deleteReport(1L, id));
            verifyNoInteractions(reportRepository);
        }
    }
}
