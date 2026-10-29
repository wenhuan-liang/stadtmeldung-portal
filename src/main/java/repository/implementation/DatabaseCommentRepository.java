package repository.implementation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.connection.ConnectionProvider;
import model.Comment;
import repository.RepositoryException;
import repository.implementation.mapper.CommentResultSetMapper;
import repository.interfaces.CommentRepository;

public class DatabaseCommentRepository implements CommentRepository {

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseCommentRepository.class);

    private final ConnectionProvider connectionProvider;
    private final CommentResultSetMapper commentMapper;

    public DatabaseCommentRepository(
            ConnectionProvider connectionProvider,
            CommentResultSetMapper commentMapper) {

        this.connectionProvider = connectionProvider;
        this.commentMapper = commentMapper;
    }

    @Override
    public void save(Comment comment) {

        String sql = """
            INSERT INTO comments (report_id, user_id, text, photo_path)
            VALUES (?, ?, ?, ?);
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, comment.getReportId());
            pstmt.setLong(2, comment.getUserId());
            pstmt.setString(3, comment.getText());
            pstmt.setString(4, comment.getPhotoPath() == null
                    ? "" : comment.getPhotoPath());

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    comment.setId(keys.getLong(1));
                } else {
                    throw new SQLException(
                            "No generated ID returned for comment");
                }
            }

            logger.info("Comment saved with ID {}", comment.getId());

        } catch (SQLException e) {
            logger.error("Error while saving comment", e);
            throw new RepositoryException(
                    "Failed to save comment", e);
        }
    }

    @Override
    public Comment findById(long id) {

        String sql = "SELECT * FROM comments WHERE id = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return commentMapper.map(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Error while finding comment {}", id, e);
            throw new RepositoryException(
                    "Failed to find comment " + id, e);
        }

        return null;
    }

    @Override
    public List<Comment> findByReportId(long reportId) {

        List<Comment> comments = new ArrayList<>();

        String sql = """
            SELECT * FROM comments
            WHERE report_id = ?
            ORDER BY created_at ASC, id ASC;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, reportId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(commentMapper.map(rs));
                }
            }

        } catch (SQLException e) {
            logger.error(
                    "Error while loading comments for report {}",
                    reportId, e);
            throw new RepositoryException(
                    "Failed to load comments for report " + reportId, e);
        }

        return comments;
    }

    @Override
    public List<Comment> findByUserId(long userId) {

        List<Comment> comments = new ArrayList<>();

        String sql = """
            SELECT * FROM comments
            WHERE user_id = ?
            ORDER BY created_at ASC, id ASC;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(commentMapper.map(rs));
                }
            }

        } catch (SQLException e) {
            logger.error(
                    "Error while loading comments for user {}",
                    userId, e);
            throw new RepositoryException(
                    "Failed to load comments for user " + userId, e);
        }

        return comments;
    }

    @Override
    public Set<Long> findReportIdsByUserId(long userId) {
        Set<Long> reportIds = new LinkedHashSet<>();
        String sql = """
            SELECT DISTINCT report_id FROM comments
            WHERE user_id = ?;
            """;
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reportIds.add(rs.getLong("report_id"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reports updated by user {}", userId, e);
            throw new RepositoryException(
                    "Failed to find reports updated by user", e);
        }
        return reportIds;
    }

    @Override
    public void update(Comment comment) {

        String sql = """
            UPDATE comments
            SET text = ?
            WHERE id = ?;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, comment.getText());
            pstmt.setLong(2, comment.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Comment not found: " + comment.getId());
            }

            logger.info("Comment {} updated", comment.getId());

        } catch (SQLException e) {
            logger.error("Error while updating comment", e);
            throw new RepositoryException(
                    "Failed to update comment", e);
        }
    }

    @Override
    public void deleteById(long id) {

        String sql = "DELETE FROM comments WHERE id = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            int affectedRows = pstmt.executeUpdate();

            logger.info("Deleted {} comment(s) for ID {}",
                    affectedRows, id);

        } catch (SQLException e) {
            logger.error("Error while deleting comment {}", id, e);
            throw new RepositoryException(
                    "Failed to delete comment " + id, e);
        }
    }
}
