package com.stadtmeldung.controller;

import com.stadtmeldung.service.ImageDataService;
import model.Comment;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import service.interfaces.CommentService;
import service.interfaces.ReportService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/reports/{reportId}/comments")
public class CommentController {
    private final CommentService commentService;
    private final ReportService reportService;
    private final ApiResponseMapper responseMapper;
    private final ImageDataService imageDataService;
    private final AuthenticatedUserResolver userResolver;

    public CommentController(CommentService commentService,
                             ReportService reportService,
                             ApiResponseMapper responseMapper,
                             ImageDataService imageDataService,
                             AuthenticatedUserResolver userResolver) {
        this.commentService = commentService;
        this.reportService = reportService;
        this.responseMapper = responseMapper;
        this.imageDataService = imageDataService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public List<Map<String, Object>> getComments(
            @PathVariable long reportId,
            Authentication authentication) {
        if (reportService.findById(reportId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Meldung nicht gefunden.");
        }
        User viewer = userResolver.currentUser(authentication);
        Stream<Comment> comments = commentService.findByReportId(reportId)
                .stream();
        if (viewer == null || (viewer.getRole() != model.enums.Role.EMPLOYEE &&
                viewer.getRole() != model.enums.Role.ADMIN)) {
            comments = comments.filter(comment -> !comment.isInternal());
        }
        return comments
                .map(responseMapper::comment)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createComment(
            @PathVariable long reportId,
            @RequestBody CreateCommentRequest request,
            Authentication authentication) throws IOException {
        User citizen = userResolver.requireCitizen(authentication);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Update-Daten fehlen.");
        }
        String text = request.text() == null ? "" : request.text().trim();
        String photoPath = "";
        if (request.photoData() != null && !request.photoData().isBlank()) {
            photoPath = imageDataService.saveDataUrl(request.photoData());
        }
        try {
            Comment comment = commentService.createComment(
                    reportId, citizen.getId(), text, photoPath);
            return Map.of("comment", responseMapper.comment(comment));
        } catch (RuntimeException exception) {
            imageDataService.deleteSavedFile(photoPath);
            throw exception;
        }
    }

    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createInternalComment(
            @PathVariable long reportId,
            @RequestBody CreateInternalCommentRequest request,
            Authentication authentication) {
        User employee = userResolver.requireStaff(authentication);
        if (request == null || request.text() == null ||
                request.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bitte geben Sie einen internen Kommentar ein.");
        }
        Comment comment = commentService.createInternalComment(
                reportId, employee.getId(), request.text());
        return Map.of("comment", responseMapper.comment(comment));
    }

    public record CreateCommentRequest(String text, String photoData) {}

    public record CreateInternalCommentRequest(String text) {}
}
