package repository.implementation;

import database.connection.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.RepositoryException;
import repository.interfaces.ReportVoteRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DatabaseReportVoteRepository
        implements ReportVoteRepository {
    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseReportVoteRepository.class);
    private final ConnectionProvider connectionProvider;

    public DatabaseReportVoteRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public int countByReportId(long reportId) {
        String sql = "SELECT COUNT(*) FROM report_votes WHERE report_id = ?;";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.error("Could not count votes for report {}", reportId, e);
            throw new RepositoryException("Failed to count report votes", e);
        }
    }

    @Override
    public boolean existsByReportIdAndUserId(long reportId, long userId) {
        String sql = """
                SELECT 1 FROM report_votes
                WHERE report_id = ? AND user_id = ?;
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            statement.setLong(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            logger.error("Could not check report vote", e);
            throw new RepositoryException("Failed to check report vote", e);
        }
    }

    @Override
    public boolean addVote(long reportId, long userId) {
        String sql = """
                INSERT INTO report_votes (report_id, user_id)
                VALUES (?, ?)
                ON CONFLICT(report_id, user_id) DO NOTHING;
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.error("Could not add vote for report {}", reportId, e);
            throw new RepositoryException("Failed to add report vote", e);
        }
    }

    @Override
    public boolean removeVote(long reportId, long userId) {
        String sql = """
                DELETE FROM report_votes
                WHERE report_id = ? AND user_id = ?;
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.error("Could not remove vote for report {}", reportId, e);
            throw new RepositoryException("Failed to remove report vote", e);
        }
    }

    @Override
    public Set<Long> findReportIdsByUserId(long userId) {
        String sql = "SELECT report_id FROM report_votes WHERE user_id = ?;";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                Set<Long> reportIds = new LinkedHashSet<>();
                while (result.next()) {
                    reportIds.add(result.getLong("report_id"));
                }
                return reportIds;
            }
        } catch (SQLException e) {
            logger.error("Could not find voted reports for user {}", userId, e);
            throw new RepositoryException(
                    "Failed to find reports supported by user", e);
        }
    }
}
