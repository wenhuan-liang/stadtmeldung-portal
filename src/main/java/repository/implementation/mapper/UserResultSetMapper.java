package repository.implementation.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import model.User;
import model.enums.Role;

public class UserResultSetMapper {

    private static final DateTimeFormatter SQLITE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public User map(ResultSet rs) throws SQLException {

        long id = rs.getLong("id");

        String name = rs.getString("name");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");

        Role role = Role.valueOf(rs.getString("role"));

        LocalDateTime createdAt =
                parseDateTime(rs.getString("created_at"));

        return new User(
                id,
                name,
                username,
                email,
                passwordHash,
                role,
                createdAt
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value, SQLITE_DATE_FORMAT);
    }
}
