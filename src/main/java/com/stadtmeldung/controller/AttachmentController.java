package com.stadtmeldung.controller;

import com.stadtmeldung.service.ImageDataService;
import model.Attachment;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import service.interfaces.AttachmentService;
import service.interfaces.ReportService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class AttachmentController {
    private final AttachmentService attachmentService;
    private final ReportService reportService;
    private final ImageDataService imageDataService;
    private final AuthenticatedUserResolver userResolver;

    public AttachmentController(AttachmentService attachmentService,
                                ReportService reportService,
                                ImageDataService imageDataService,
                                AuthenticatedUserResolver userResolver) {
        this.attachmentService = attachmentService;
        this.reportService = reportService;
        this.imageDataService = imageDataService;
        this.userResolver = userResolver;
    }

    @GetMapping("/api/reports/{reportId}/attachments")
    public List<Map<String, Object>> getAttachments(
            @PathVariable long reportId) {
        if (reportService.findById(reportId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Meldung nicht gefunden.");
        }
        return attachmentService.findByReportId(reportId).stream()
                .map(AttachmentController::attachmentResponse)
                .toList();
    }

    @PostMapping("/api/reports/{reportId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addAttachment(
            @PathVariable long reportId,
            @RequestBody CreateAttachmentRequest request,
            Authentication authentication) throws IOException {
        User citizen = userResolver.requireCitizen(authentication);
        if (request == null || request.photoData() == null ||
                request.photoData().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bitte wählen Sie ein Foto aus.");
        }

        String filePath = imageDataService.saveDataUrl(request.photoData());
        try {
            Attachment attachment = attachmentService.addAttachment(
                    reportId, filePath, citizen.getId());
            return Map.of("attachment", attachmentResponse(attachment));
        } catch (RuntimeException exception) {
            imageDataService.deleteSavedFile(filePath);
            throw exception;
        }
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(
            @PathVariable long attachmentId,
            Authentication authentication) throws IOException {
        User citizen = userResolver.requireCitizen(authentication);
        Attachment attachment = attachmentService.findById(attachmentId);
        if (attachment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Anhang nicht gefunden.");
        }

        attachmentService.deleteAttachment(attachmentId, citizen.getId());
        imageDataService.deleteSavedFile(attachment.getFilePath());
    }

    private static Map<String, Object> attachmentResponse(
            Attachment attachment) {
        return Map.of(
                "id", attachment.getId(),
                "reportId", attachment.getReportId(),
                "filePath", attachment.getFilePath(),
                "createdAt", attachment.getCreatedAt() == null
                        ? "" : attachment.getCreatedAt().toString());
    }

    public record CreateAttachmentRequest(String photoData) {}
}
