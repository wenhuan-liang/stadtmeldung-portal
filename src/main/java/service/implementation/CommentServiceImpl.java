package service.implementation;

import java.util.List;
import java.util.Set;

import model.Comment;
import model.User;
import model.enums.Role;
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
        return createComment(reportId, userId, text, "");
    }

    @Override
    public Comment createComment(long reportId, long userId,
                                 String text, String photoPath) {

        validateId(reportId);
        validateId(userId);
        validateText(text, photoPath);

        if (reportRepository.findById(reportId) == null) {
            throw new IllegalArgumentException(
                    "Report not found: " + reportId);
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException(
                    "User not found: " + userId);
        }
        if (user.getRole() != Role.CITIZEN) {
            throw new SecurityException(
                    "Only citizens can add public report information");
        }

        Comment comment = new Comment(reportId, userId,
                text == null ? "" : text.trim(),
                photoPath == null ? "" : photoPath);

        commentRepository.save(comment);

        return comment;
    }

    @Override
    public Comment createInternalComment(long reportId, long userId,
                                         String text) {
        validateId(reportId);
        validateId(userId);
        validateText(text, "");

        if (reportRepository.findById(reportId) == null) {
            throw new IllegalArgumentException(
                    "Report not found: " + reportId);
        }

        User user = userRepository.findById(userId);
        if (user == null || (user.getRole() != Role.EMPLOYEE &&
                user.getRole() != Role.ADMIN)) {
            throw new SecurityException(
                    "Only employees and admins can add internal comments");
        }

        Comment comment = new Comment(reportId, userId, text.trim(), "",
                true);
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
    public Set<Long> findReportIdsByUserId(long userId) {
        validateId(userId);
        return commentRepository.findReportIdsByUserId(userId);
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

        validateText(text, "");

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

    private void validateText(String text, String photoPath) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasPhoto = photoPath != null && !photoPath.isBlank();
        if ((!hasText && !hasPhoto) || (text != null && text.length() > 1000)) {
            throw new IllegalArgumentException(
                    "Information must include text or a photo, with at most 1000 characters");
        }
    }

    private void validateId(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID must be greater than zero");
        }
    }
}
