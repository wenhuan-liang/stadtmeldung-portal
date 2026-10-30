package repository.interfaces;

import java.util.List;

import model.Comment;

public interface CommentRepository {

    void save(Comment comment);

    Comment findById(long id);

    List<Comment> findByReportId(long reportId);

    List<Comment> findByUserId(long userId);

    void update(Comment comment);

    void deleteById(long id);
}