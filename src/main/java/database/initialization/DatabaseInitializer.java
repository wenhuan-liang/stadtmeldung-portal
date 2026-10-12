package database.initialization;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.connection.DatabaseManager;

public final class DatabaseInitializer {

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseInitializer.class);

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection conn = DatabaseManager.getConnection()) {

            executeSchemaCreation(conn);

            logger.info("Database was correctly initialized");

        } catch (SQLException e) {
            logger.error("Initialization error", e);
            throw new IllegalStateException(
                    "Database could not be initialized.", e);
        }
    }

    private static void executeSchemaCreation(Connection conn)
            throws SQLException {

        executeSql(conn, createUsersSql());
        executeSql(conn, createReportsSql());
        executeSql(conn, createCommentsSql());
        executeSql(conn, createAttachmentsSql());
    }

    // Tabelle für Bürger und Mitarbeiter
    private static String createUsersSql() {
        return """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'CITIZEN'
                    CHECK (role IN ('CITIZEN', 'EMPLOYEE', 'ADMIN')),
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;
    }

    // Tabelle für Mängelmeldungen
    private static String createReportsSql() {
        return """
            CREATE TABLE IF NOT EXISTS reports (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                category TEXT NOT NULL
                    CHECK (category IN (
                        'POTHOLE',
                        'LITTER',
                        'STREETLIGHT',
                        'PARKING_VIOLATION',
                        'OTHER'
                    )),
                description TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                status TEXT NOT NULL DEFAULT 'NEW'
                    CHECK (status IN (
                        'NEW',
                        'IN_PROGRESS',
                        'COMPLETED'
                    )),
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                FOREIGN KEY (user_id) REFERENCES users(id)
                    ON DELETE SET NULL
            );
            """;
    }

    // Tabelle für Kommentare und Bearbeitungsnotizen
    private static String createCommentsSql() {
        return """
            CREATE TABLE IF NOT EXISTS comments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                report_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                text TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                FOREIGN KEY (report_id) REFERENCES reports(id)
                    ON DELETE CASCADE,

                FOREIGN KEY (user_id) REFERENCES users(id)
                    ON DELETE CASCADE
            );
            """;
    }

    // Tabelle für Fotos und weitere Anhänge
    private static String createAttachmentsSql() {
        return """
            CREATE TABLE IF NOT EXISTS attachments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                report_id INTEGER NOT NULL,
                file_path TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                FOREIGN KEY (report_id) REFERENCES reports(id)
                    ON DELETE CASCADE
            );
            """;
    }

    private static void executeSql(Connection conn, String sql)
            throws SQLException {

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        }
    }
}
