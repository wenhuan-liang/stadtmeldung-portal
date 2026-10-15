package service.implementation;

import java.util.List;

import model.Comment;
import repository.interfaces.CommentRepository;
import repository.interfaces.ReportRepository;
import repository.interfaces.UserRepository;
import service.interfaces.CommentService;

public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public CommentServiceImpl(
            CommentRepository commentRepository,
            ReportRepository reportRepository,
            UserRepository userRepository) {

        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Comment createComment(long reportId, long userId,
                                 String text) {

        validateId(reportId);
        validateId(userId);
        validateText(text);

        if (reportRepository.findById(reportId) == null) {
            throw new IllegalArgumentException(
                    "Report not found: " + reportId);
        }

        if (userRepository.findById(userId) == null) {
            throw new IllegalArgumentException(
                    "User not found: " + userId);
        }

        Comment comment = new Comment(
                reportId,
                userId,
                text.trim()
        );

        commentRepository.save(comment);

        return comment;
    }

    @Override
    public Comment findById(long id) {

        validateId(id);

        return commentRepository.findById(id);
    }

    @Override
    public List<Comment> findByReportId(long reportId) {

        validateId(reportId);

        return commentRepository.findByReportId(reportId);
    }

    @Override
    public List<Comment> findByUserId(long userId) {

        validateId(userId);

        return commentRepository.findByUserId(userId);
    }

    @Override
    public void updateComment(long commentId, String text,
                              long currentUserId) {

        validateId(commentId);
        validateId(currentUserId);

        Comment comment = commentRepository.findById(commentId);

        if (comment == null) {
            throw new IllegalArgumentException(
                    "Comment not found: " + commentId);
        }

        // Nur eigene Kommentare dürfen bearbeitet werden
        if (!comment.getUserId().equals(currentUserId)) {
            throw new SecurityException(
                    "You can only edit your own comments");
        }

        validateText(text);

        comment.setText(text.trim());

        commentRepository.update(comment);
    }

    @Override
    public void deleteComment(long commentId,
                              long currentUserId) {

        validateId(commentId);
        validateId(currentUserId);

        Comment comment = commentRepository.findById(commentId);

        if (comment == null) {
            throw new IllegalArgumentException(
                    "Comment not found: " + commentId);
        }

        // Nur eigene Kommentare dürfen gelöscht werden
        if (!comment.getUserId().equals(currentUserId)) {
            throw new SecurityException(
                    "You can only delete your own comments");
        }

        commentRepository.deleteById(commentId);
    }

    private void validateText(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Comment must not be empty");
        }
    }

    private void validateId(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID must be greater than zero");
        }
    }
}
