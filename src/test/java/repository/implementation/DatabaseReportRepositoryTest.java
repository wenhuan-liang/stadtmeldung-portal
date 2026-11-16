package repository.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import database.connection.ConnectionProvider;
import model.Report;
import model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.implementation.mapper.ReportResultSetMapper;

class DatabaseReportRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseReportRepository repository;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() throws Exception {
        Path databasePath = temporaryDirectory.resolve("reports.db");
        connectionProvider = () ->
                DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        repository = new DatabaseReportRepository(
                connectionProvider, new ReportResultSetMapper());

        try (Connection connection = connectionProvider.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    defect_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    photo_path TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    completed_at TEXT
                );
                """);
        }
    }

    @Test
    void onlyCompletedReportsOlderThanSevenDaysAreExpired() throws Exception {
        long expiredId = insertReport(Status.COMPLETED);
        setCompletionAge(expiredId, "-8 days");
        long recentId = insertReport(Status.COMPLETED);
        setCompletionAge(recentId, "-6 days");
        long legacyId = insertReport(Status.COMPLETED);
        long activeId = insertReport(Status.IN_PROGRESS);
        setCompletionAge(activeId, "-8 days");

        assertEquals(List.of(expiredId), repository
                .findExpiredCompletedReports().stream()
                .map(Report::getId)
                .toList());

        assertTrue(repository.deleteExpiredCompletedReport(expiredId));
        assertFalse(repository.deleteExpiredCompletedReport(expiredId));
        assertNotNull(repository.findById(recentId));
        assertNotNull(repository.findById(legacyId));
        assertNotNull(repository.findById(activeId));
    }

    @Test
    void completingStartsRetentionAndReopeningRestartsIt() throws Exception {
        long reportId = insertReport(Status.IN_PROGRESS);

        repository.updateStatus(reportId, Status.COMPLETED);
        assertTrue(repository.findExpiredCompletedReports().isEmpty());

        setCompletionAge(reportId, "-8 days");
        repository.updateStatus(reportId, Status.COMPLETED);
        assertEquals(List.of(reportId), repository
                .findExpiredCompletedReports().stream()
                .map(Report::getId)
                .toList());

        repository.updateStatus(reportId, Status.IN_PROGRESS);
        assertTrue(repository.findExpiredCompletedReports().isEmpty());

        repository.updateStatus(reportId, Status.COMPLETED);
        assertTrue(repository.findExpiredCompletedReports().isEmpty());
    }

    private long insertReport(Status status) throws Exception {
        String sql = """
            INSERT INTO reports
                (title, category, defect_type, description, photo_path,
                 latitude, longitude, status)
            VALUES ('Test', 'OTHER', '', 'Description', '',
                    52.5, 13.4, ?);
            """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, status.name());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private void setCompletionAge(long reportId, String modifier)
            throws Exception {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 UPDATE reports
                 SET completed_at = datetime('now', ?)
                 WHERE id = ?;
                 """)) {
            statement.setString(1, modifier);
            statement.setLong(2, reportId);
            assertEquals(1, statement.executeUpdate());
        }
    }
}
