
package com.stadtmeldung.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import model.Comment;
import model.User;
import repository.interfaces.UserRepository;
import service.interfaces.CommentService;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    public CommentController(
            CommentService commentService,
            UserRepository userRepository) {

        this.commentService = commentService;
        this.userRepository = userRepository;
    }

    // Kommentare zu einer Meldung anzeigen
    // GET /api/reports/5/comments
    @GetMapping("/reports/{reportId}/comments")
    public List<Comment> getCommentsByReport(
            @PathVariable long reportId) {

        try {
            return commentService.findByReportId(reportId);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Kommentar erstellen
    // POST /api/reports/5/comments
    @PostMapping("/reports/{reportId}/comments")
    public ResponseEntity<Comment> createComment(
            @PathVariable long reportId,
            @RequestBody CreateCommentRequest request,
            Authentication authentication) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing comment data"
            );
        }

        long currentUserId = getCurrentUserId(authentication);

        try {
            Comment comment = commentService.createComment(
                    reportId,
                    currentUserId,
                    request.text()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(comment);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Eigenen Kommentar bearbeiten
    // PUT /api/comments/3
    @PutMapping("/comments/{commentId}")
    public Comment updateComment(
            @PathVariable long commentId,
            @RequestBody UpdateCommentRequest request,
            Authentication authentication) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing comment data"
            );
        }

        long currentUserId = getCurrentUserId(authentication);

        try {
            commentService.updateComment(
                    commentId,
                    request.text(),
                    currentUserId
            );

            return commentService.findById(commentId);

        } catch (SecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only edit your own comments"
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Eigenen Kommentar löschen
    // DELETE /api/comments/3
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable long commentId,
            Authentication authentication) {

        long currentUserId = getCurrentUserId(authentication);

        try {
            commentService.deleteComment(
                    commentId,
                    currentUserId
            );

            return ResponseEntity.noContent().build();

        } catch (SecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only delete your own comments"
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Angemeldeten Benutzer sicher ermitteln
    private long getCurrentUserId(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal() instanceof String) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Login required"
            );
        }

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user not found"
            );
        }

        return user.getId();
    }

    // Daten, die das Frontend sendet
    public record CreateCommentRequest(String text) {}

    public record UpdateCommentRequest(String text) {}
}
