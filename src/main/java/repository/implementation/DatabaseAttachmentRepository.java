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
import model.Attachment;
import repository.RepositoryException;
import repository.implementation.mapper.AttachmentResultSetMapper;
import repository.interfaces.AttachmentRepository;

public class DatabaseAttachmentRepository
        implements AttachmentRepository {

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseAttachmentRepository.class);

    private final ConnectionProvider connectionProvider;
    private final AttachmentResultSetMapper attachmentMapper;

    public DatabaseAttachmentRepository(
            ConnectionProvider connectionProvider,
            AttachmentResultSetMapper attachmentMapper) {

        this.connectionProvider = connectionProvider;
        this.attachmentMapper = attachmentMapper;
    }

    @Override
    public void save(Attachment attachment) {

        String sql = """
            INSERT INTO attachments (report_id, file_path)
            VALUES (?, ?);
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, attachment.getReportId());
            pstmt.setString(2, attachment.getFilePath());

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    attachment.setId(keys.getLong(1));
                } else {
                    throw new SQLException(
                            "No generated ID returned for attachment");
                }
            }

            logger.info("Attachment saved with ID {}",
                    attachment.getId());

        } catch (SQLException e) {
            logger.error("Error while saving attachment", e);
            throw new RepositoryException(
                    "Failed to save attachment", e);
        }
    }

    @Override
    public Attachment findById(long id) {

        String sql = """
            SELECT * FROM attachments
            WHERE id = ?;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return attachmentMapper.map(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Error while finding attachment {}", id, e);
            throw new RepositoryException(
                    "Failed to find attachment " + id, e);
        }

        return null;
    }

    @Override
    public List<Attachment> findByReportId(long reportId) {

        List<Attachment> attachments = new ArrayList<>();

        String sql = """
            SELECT * FROM attachments
            WHERE report_id = ?
            ORDER BY created_at ASC, id ASC;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, reportId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    attachments.add(attachmentMapper.map(rs));
                }
            }

        } catch (SQLException e) {
            logger.error(
                    "Error while loading attachments for report {}",
                    reportId, e);
            throw new RepositoryException(
                    "Failed to load attachments for report "
                            + reportId, e);
        }

        return attachments;
    }

    @Override
    public void deleteById(long id) {

        String sql = """
            DELETE FROM attachments
            WHERE id = ?;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            int affectedRows = pstmt.executeUpdate();

            logger.info("Deleted {} attachment(s) for ID {}",
                    affectedRows, id);

        } catch (SQLException e) {
            logger.error("Error while deleting attachment {}", id, e);
            throw new RepositoryException(
                    "Failed to delete attachment " + id, e);
        }
    }

    @Override
    public void deleteByReportId(long reportId) {

        String sql = """
            DELETE FROM attachments
            WHERE report_id = ?;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, reportId);

            int affectedRows = pstmt.executeUpdate();

            logger.info(
                    "Deleted {} attachment(s) for report {}",
                    affectedRows, reportId);

        } catch (SQLException e) {
            logger.error(
                    "Error while deleting attachments for report {}",
                    reportId, e);
            throw new RepositoryException(
                    "Failed to delete attachments for report "
                            + reportId, e);
        }
    }
}