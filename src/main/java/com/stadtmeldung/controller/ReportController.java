package com.stadtmeldung.controller;

import com.stadtmeldung.service.ImageDataService;
import model.Comment;
import model.Report;
import model.User;
import model.enums.Role;
import model.enums.Status;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import service.interfaces.CommentService;
import service.interfaces.ReportService;
import service.interfaces.ReportVoteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private final CommentService commentService;
    private final ReportVoteService reportVoteService;
    private final ApiResponseMapper responseMapper;
    private final ImageDataService imageDataService;
    private final AuthenticatedUserResolver userResolver;

    public ReportController(ReportService reportService,
                            CommentService commentService,
                            ReportVoteService reportVoteService,
                            ApiResponseMapper responseMapper,
                            ImageDataService imageDataService,
                            AuthenticatedUserResolver userResolver) {
        this.reportService = reportService;
        this.commentService = commentService;
        this.reportVoteService = reportVoteService;
        this.responseMapper = responseMapper;
        this.imageDataService = imageDataService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public List<Map<String, Object>> getReports(
            @RequestParam(defaultValue = "false") boolean mine,
            Authentication authentication) {
        User viewer = userResolver.currentUser(authentication);
        if (mine && (viewer == null || viewer.getRole() != Role.CITIZEN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ein Bürgerkonto ist erforderlich.");
        }

        Set<Long> votedIds = mine
                ? new HashSet<>(
                        reportVoteService.findReportIdsByUserId(viewer.getId()))
                : Set.of();
        Set<Long> updatedIds = mine
                ? commentService.findReportIdsByUserId(viewer.getId())
                : Set.of();
        Set<Long> relatedIds = new HashSet<>(votedIds);
        relatedIds.addAll(updatedIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Report report : reportService.findAll()) {
            boolean submittedByViewer = mine &&
                    viewer.getId().equals(report.getUserId());
            if (mine && !submittedByViewer &&
                    !relatedIds.contains(report.getId())) {
                continue;
            }
            Map<String, Object> item = responseMapper.report(report, viewer);
            if (mine) {
                item.put("submittedByMe", submittedByViewer);
                item.put("votedByMe", votedIds.contains(report.getId()));
                item.put("updatedByMe", updatedIds.contains(report.getId()));
            }
            result.add(item);
        }
        return result;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createReport(
            @RequestBody CreateReportRequest request,
            Authentication authentication) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Meldungsdaten fehlen.");
        }
        User creator = userResolver.currentUser(authentication);
        if (creator != null && creator.getRole() != Role.CITIZEN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Mitarbeitende können keine Meldungen einreichen.");
        }

        String photoPath = "";
        if (request.photoData() != null && !request.photoData().isBlank()) {
            photoPath = imageDataService.saveDataUrl(request.photoData());
        }
        try {
            Report report = reportService.createReport(
                    creator == null ? null : creator.getId(),
                    required(request.title(), "Titel"),
                    ApiResponseMapper.parseCategory(request.category()),
                    request.defectType() == null ? "" : request.defectType(),
                    required(request.description(), "Beschreibung"),
                    photoPath,
                    request.latitude(),
                    request.longitude());
            return Map.of("report", responseMapper.report(report, creator));
        } catch (RuntimeException exception) {
            imageDataService.deleteSavedFile(photoPath);
            throw exception;
        }
    }

    @PatchMapping("/{id}")
    public Map<String, Object> changeStatus(
            @PathVariable long id,
            @RequestBody StatusRequest request,
            Authentication authentication) {
        User employee = userResolver.requireStaff(authentication);
        Report existing = reportService.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Meldung nicht gefunden.");
        }
        reportService.updateStatus(id,
                ApiResponseMapper.parseStatus(request.status()),
                employee.getId());
        Report updated = reportService.findById(id);
        return Map.of("report", responseMapper.report(updated, employee));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompletedReport(
            @PathVariable long id,
            Authentication authentication) throws IOException {
        User employee = userResolver.requireStaff(authentication);
        Report report = reportService.findById(id);
        if (report == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Meldung nicht gefunden.");
        }
        if (report.getStatus() != Status.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nur erledigte Meldungen können gelöscht werden.");
        }
        List<Comment> comments = commentService.findByReportId(id);
        reportService.deleteCompletedReport(id, employee.getId());
        imageDataService.deleteSavedFile(report.getPhotoPath());
        for (Comment comment : comments) {
            imageDataService.deleteSavedFile(comment.getPhotoPath());
        }
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Bitte geben Sie " + label + " an.");
        }
        return value.trim();
    }

    public record CreateReportRequest(
            String title,
            String category,
            String defectType,
            String description,
            String photoData,
            double latitude,
            double longitude) {}

    public record StatusRequest(String status) {}
}
