package repository.implementation.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import model.Comment;

public class CommentResultSetMapper {

    private static final DateTimeFormatter SQLITE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Comment map(ResultSet rs) throws SQLException {

        long id = rs.getLong("id");

        long reportId = rs.getLong("report_id");

        long userId = rs.getLong("user_id");

        String text = rs.getString("text");

        LocalDateTime createdAt =
                parseDateTime(rs.getString("created_at"));

        return new Comment(
                id,
                reportId,
                userId,
                text,
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
