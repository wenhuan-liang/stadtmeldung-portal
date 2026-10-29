package database.connection;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionProvider implements ConnectionProvider {

    @Override
    public Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
    }
}
