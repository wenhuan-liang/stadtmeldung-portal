package repository.implementation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.connection.ConnectionProvider;
import model.User;
import repository.RepositoryException;
import repository.implementation.mapper.UserResultSetMapper;
import repository.interfaces.UserRepository;

public class DatabaseUserRepository implements UserRepository {

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseUserRepository.class);

    private final ConnectionProvider connectionProvider;
    private final UserResultSetMapper userMapper;

    public DatabaseUserRepository(
            ConnectionProvider connectionProvider,
            UserResultSetMapper userMapper) {

        this.connectionProvider = connectionProvider;
        this.userMapper = userMapper;
    }

    @Override
    public void save(User user) {

        String sql = """
            INSERT INTO users
            (name, username, email, password_hash, role)
            VALUES (?, ?, ?, ?, ?);
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPasswordHash());
            pstmt.setString(5, user.getRole().name());

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                } else {
                    throw new SQLException(
                            "No generated ID returned for user");
                }
            }

            logger.info("User saved with ID {}", user.getId());

        } catch (SQLException e) {
            logger.error("Error while saving user", e);
            throw new RepositoryException(
                    "Failed to save user", e);
        }
    }

    @Override
    public User findById(long id) {

        String sql = "SELECT * FROM users WHERE id = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return userMapper.map(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Error while finding user {}", id, e);
            throw new RepositoryException(
                    "Failed to find user " + id, e);
        }

        return null;
    }

    @Override
    public User findByEmail(String email) {

        String sql = "SELECT * FROM users WHERE email = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return userMapper.map(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Error while finding user by email", e);
            throw new RepositoryException(
                    "Failed to find user by email", e);
        }

        return null;
    }

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?;";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return userMapper.map(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error while finding user by username", e);
            throw new RepositoryException(
                    "Failed to find user by username", e);
        }
        return null;
    }

    @Override
    public List<User> findAll() {

        List<User> users = new ArrayList<>();

        String sql = """
            SELECT * FROM users
            ORDER BY id ASC;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(userMapper.map(rs));
            }

        } catch (SQLException e) {
            logger.error("Error while loading users", e);
            throw new RepositoryException(
                    "Failed to load users", e);
        }

        return users;
    }

    @Override
    public void update(User user) {

        String sql = """
            UPDATE users
            SET name = ?,
                username = ?,
                email = ?,
                password_hash = ?,
                role = ?
            WHERE id = ?;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPasswordHash());
            pstmt.setString(5, user.getRole().name());
            pstmt.setLong(6, user.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "User not found: " + user.getId());
            }

            logger.info("User {} updated", user.getId());

        } catch (SQLException e) {
            logger.error("Error while updating user", e);
            throw new RepositoryException(
                    "Failed to update user", e);
        }
    }

    @Override
    public void deleteById(long id) {

        String sql = "DELETE FROM users WHERE id = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            int affectedRows = pstmt.executeUpdate();

            logger.info("Deleted {} user(s) for ID {}",
                    affectedRows, id);

        } catch (SQLException e) {
            logger.error("Error while deleting user {}", id, e);
            throw new RepositoryException(
                    "Failed to delete user " + id, e);
        }
    }
}
