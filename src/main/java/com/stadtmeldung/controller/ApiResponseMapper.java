package com.stadtmeldung.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import model.Comment;
import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Role;
import model.enums.Status;
import service.interfaces.ReportVoteService;
import service.interfaces.UserService;

@Component
public class ApiResponseMapper {
    private final ReportVoteService reportVoteService;
    private final UserService userService;

    public ApiResponseMapper(ReportVoteService reportVoteService,
                             UserService userService) {
        this.reportVoteService = reportVoteService;
        this.userService = userService;
    }

    public Map<String, Object> user(User user) {
        return Map.of(
                "username", user.getUsername(),
                "displayName", user.getName(),
                "role", roleName(user.getRole()));
    }

    public Map<String, Object> report(Report report, User viewer) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", report.getId());
        result.put("title", report.getTitle());
        result.put("category", categoryLabel(report.getCategory()));
        result.put("defectType", report.getDefectType() == null
                ? "" : report.getDefectType());
        result.put("description", report.getDescription());
        result.put("latitude", report.getLatitude());
        result.put("longitude", report.getLongitude());
        result.put("status", statusName(report.getStatus()));
        result.put("photo", report.getPhotoPath() == null
                ? "" : report.getPhotoPath());
        result.put("submittedBy", report.getUserId());
        result.put("voteCount", reportVoteService.count(report.getId()));
        result.put("hasVoted", viewer != null &&
                viewer.getRole() == Role.CITIZEN &&
                reportVoteService.hasVoted(report.getId(), viewer.getId()));
        result.put("createdAt", report.getCreatedAt() == null
                ? "" : report.getCreatedAt().toString());
        return result;
    }

    public Map<String, Object> comment(Comment comment) {
        User author = userService.findById(comment.getUserId());
        return Map.of(
                "id", comment.getId(),
                "username", author == null ? "Bürger" : author.getUsername(),
                "text", comment.getText(),
                "photo", comment.getPhotoPath() == null
                        ? "" : comment.getPhotoPath(),
                "internal", comment.isInternal(),
                "createdAt", comment.getCreatedAt() == null
                        ? "" : comment.getCreatedAt().toString());
    }

    public static String roleName(Role role) {
        return switch (role) {
            case CITIZEN -> "user";
            case EMPLOYEE -> "staff";
            case ADMIN -> "admin";
        };
    }

    public static Category parseCategory(String value) {
        return switch (value) {
            case "Defekt" -> Category.POTHOLE;
            case "Verunreinigung" -> Category.LITTER;
            case "Parkvergehen" -> Category.PARKING_VIOLATION;
            case "Sonstiges" -> Category.OTHER;
            default -> throw new IllegalArgumentException(
                    "Ungültige Kategorie.");
        };
    }

    private static String categoryLabel(Category category) {
        return switch (category) {
            case POTHOLE, STREETLIGHT -> "Defekt";
            case LITTER -> "Verunreinigung";
            case PARKING_VIOLATION -> "Parkvergehen";
            case OTHER -> "Sonstiges";
        };
    }

    public static Status parseStatus(String value) {
        return switch (value) {
            case "new" -> Status.NEW;
            case "in_progress" -> Status.IN_PROGRESS;
            case "resolved" -> Status.COMPLETED;
            default -> throw new IllegalArgumentException(
                    "Ungültiger Status.");
        };
    }

    private static String statusName(Status status) {
        return switch (status) {
            case NEW -> "new";
            case IN_PROGRESS -> "in_progress";
            case COMPLETED -> "resolved";
        };
    }
}
