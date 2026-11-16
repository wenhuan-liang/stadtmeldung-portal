package testsupport;

import database.connection.DatabaseManager;
import database.initialization.DatabaseInitializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Hilfsklasse für Integrationstests mit einer echten SQLite-Datenbank.
 *
 * Maven setzt beim Testlauf PORTAL_DB_PATH auf target/test-db/portal-test.db
 * (siehe pom.xml). Damit arbeiten die Tests nie mit der echten portal.db.
 * Fehlt die Variable (z. B. Start direkt aus der IDE), werden die Tests
 * übersprungen statt echte Daten zu löschen.
 */
public final class TestDatabase {

    private static boolean initialized;

    private TestDatabase() {
    }

    private static boolean isTestDatabaseConfigured() {
        String path = System.getenv("PORTAL_DB_PATH");
        return path != null && path.contains("test-db");
    }

    /**
     * Legt beim ersten Aufruf eine frische, leere Testdatenbank an.
     * Ohne Test-Konfiguration passiert nichts (die Bereinigung wird dann
     * in {@link #clearReports()} übersprungen).
     */
    public static synchronized void init() {
        if (initialized || !isTestDatabaseConfigured()) {
            return;
        }
        String path = System.getenv("PORTAL_DB_PATH");
        try {
            for (String suffix : new String[] {"", "-wal", "-shm"}) {
                Files.deleteIfExists(Path.of(path + suffix));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DatabaseInitializer.initialize();
        initialized = true;
    }

    /** Löscht alle Daten aus allen Tabellen. */
    public static void clearAll() {
        clearReports();
        execute("DELETE FROM users;");
    }

    /** Löscht Meldungen samt Kommentaren, Stimmen und Anhängen, Benutzer bleiben. */
    public static void clearReports() {
        assumeTrue(isTestDatabaseConfigured(),
                "Integrationstests nur über 'mvn test' ausführen (PORTAL_DB_PATH fehlt).");
        init();
        execute("DELETE FROM attachments;");
        execute("DELETE FROM report_votes;");
        execute("DELETE FROM comments;");
        execute("DELETE FROM reports;");
    }

    private static void execute(String sql) {
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Testdatenbank konnte nicht bereinigt werden", e);
        }
    }
}
