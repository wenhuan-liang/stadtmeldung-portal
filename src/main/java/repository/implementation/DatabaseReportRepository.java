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
import model.Report;
import model.enums.Status;
import repository.RepositoryException;
import repository.implementation.mapper.ReportResultSetMapper;
import repository.interfaces.ReportRepository;

public class DatabaseReportRepository implements ReportRepository {

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseReportRepository.class);

    private final ConnectionProvider connectionProvider;
    private final ReportResultSetMapper reportMapper;

    public DatabaseReportRepository(
            ConnectionProvider connectionProvider,
            ReportResultSetMapper reportMapper) {

        this.connectionProvider = connectionProvider;
        this.reportMapper = reportMapper;
    }

    @Override
    public void save(Report report) {

        String sql = """
            INSERT INTO reports
            (user_id, category, description, latitude, longitude, status)
            VALUES (?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            if (report.getUserId() == null) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setLong(1, report.getUserId());
            }

            pstmt.setString(2, report.getCategory().name());
            pstmt.setString(3, report.getDescription());
            pstmt.setDouble(4, report.getLatitude());
            pstmt.setDouble(5, report.getLongitude());
            pstmt.setString(6, report.getStatus().name());

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    report.setId(keys.getLong(1));
                } else {
                    throw new SQLException(
                            "No generated ID returned for report");
                }
            }

            logger.info("Report saved with ID {}", report.getId());

        } catch (SQLException e) {
            logger.error("Error while saving report", e);
            throw new RepositoryException(
                    "Failed to save report", e);
        }
    }

    @Override
    public Report findById(long id) {

        String sql = "SELECT * FROM reports WHERE id = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return reportMapper.map(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Error while finding report {}", id, e);
            throw new RepositoryException(
                    "Failed to find report " + id, e);
        }

        return null;
    }

    @Override
    public List<Report> findAll() {

        List<Report> reports = new ArrayList<>();

        String sql = """
            SELECT * FROM reports
            ORDER BY created_at DESC, id DESC;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                reports.add(reportMapper.map(rs));
            }

        } catch (SQLException e) {
            logger.error("Error while loading reports", e);
            throw new RepositoryException(
                    "Failed to load reports", e);
        }

        return reports;
    }

    @Override
    public List<Report> findByStatus(Status status) {

        List<Report> reports = new ArrayList<>();

        String sql = """
            SELECT * FROM reports
            WHERE status = ?
            ORDER BY created_at DESC, id DESC;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(reportMapper.map(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Error while loading reports by status", e);
            throw new RepositoryException(
                    "Failed to load reports by status", e);
        }

        return reports;
    }

    @Override
    public void update(Report report) {

        String sql = """
            UPDATE reports
            SET category = ?,
                description = ?,
                latitude = ?,
                longitude = ?,
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?;
            """;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, report.getCategory().name());
            pstmt.setString(2, report.getDescription());
            pstmt.setDouble(3, report.getLatitude());
            pstmt.setDouble(4, report.getLongitude());
            pstmt.setString(5, report.getStatus().name());
            pstmt.setLong(6, report.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Report not found: " + report.getId());
            }

            logger.info("Report {} updated", report.getId());

        } catch (SQLException e) {
            logger.error("Error while updating report", e);
            throw new RepositoryException(
                    "Failed to update report", e);
        }
    }

    @Override
    public void deleteById(long id) {

        String sql = "DELETE FROM reports WHERE id = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            int affectedRows = pstmt.executeUpdate();

            logger.info("Deleted {} report(s) for ID {}",
                    affectedRows, id);

        } catch (SQLException e) {
            logger.error("Error while deleting report {}", id, e);
            throw new RepositoryException(
                    "Failed to delete report " + id, e);
        }
    }
}

