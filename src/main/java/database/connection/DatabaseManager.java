package database.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseManager {
    private static final String DATABASE_PATH =
            System.getenv().getOrDefault("PORTAL_DB_PATH", "portal.db");

    public static Connection getConnection() throws SQLException {
        Path databasePath = Path.of(DATABASE_PATH).toAbsolutePath();
        try {
            Files.createDirectories(databasePath.getParent());
        } catch (java.io.IOException e) {
            throw new SQLException("Could not create database directory", e);
        }

        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }
        return connection;
    }
}
