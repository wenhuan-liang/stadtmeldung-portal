package repository.implementation.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import model.Report;
import model.enums.Category;
import model.enums.Status;

public class ReportResultSetMapper {

    private static final DateTimeFormatter SQLITE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Report map(ResultSet rs) throws SQLException {

        long id = rs.getLong("id");

        long userIdValue = rs.getLong("user_id");
        Long userId = rs.wasNull() ? null : userIdValue;

        String title = rs.getString("title");
        Category category =
                Category.valueOf(rs.getString("category"));

        String defectType = rs.getString("defect_type");
        String description = rs.getString("description");
        String photoPath = rs.getString("photo_path");

        double latitude = rs.getDouble("latitude");
        double longitude = rs.getDouble("longitude");

        Status status =
                Status.valueOf(rs.getString("status"));

        LocalDateTime createdAt =
                parseDateTime(rs.getString("created_at"));

        LocalDateTime updatedAt =
                parseDateTime(rs.getString("updated_at"));

        return new Report(
                id,
                userId,
                title,
                category,
                defectType,
                description,
                photoPath,
                latitude,
                longitude,
                status,
                createdAt,
                updatedAt
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value, SQLITE_DATE_FORMAT);
    }
}
