package repository.implementation.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import model.Attachment;

public class AttachmentResultSetMapper {

    private static final DateTimeFormatter SQLITE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Attachment map(ResultSet rs) throws SQLException {

        long id = rs.getLong("id");

        long reportId = rs.getLong("report_id");

        String filePath = rs.getString("file_path");

        LocalDateTime createdAt =
                parseDateTime(rs.getString("created_at"));

        return new Attachment(
                id,
                reportId,
                filePath,
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