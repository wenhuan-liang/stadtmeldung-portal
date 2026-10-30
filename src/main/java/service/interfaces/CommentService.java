package service.interfaces;

import java.util.List;
import java.util.Set;

import model.Comment;

public interface CommentService {

    Comment createComment(long reportId, long userId, String text);

    Comment createComment(long reportId, long userId, String text,
                          String photoPath);

    Comment createInternalComment(long reportId, long userId, String text);

    Comment findById(long id);

    List<Comment> findByReportId(long reportId);

    List<Comment> findByUserId(long userId);

    Set<Long> findReportIdsByUserId(long userId);

    void updateComment(long commentId, String text, long currentUserId);

    void deleteComment(long commentId, long currentUserId);
}
