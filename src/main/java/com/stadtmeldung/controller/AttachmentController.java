package com.stadtmeldung.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import model.Attachment;
import model.Report;
import model.User;
import repository.interfaces.ReportRepository;
import repository.interfaces.UserRepository;
import service.interfaces.AttachmentService;
import service.interfaces.FileStorageService;

@RestController
@RequestMapping("/api")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final FileStorageService fileStorageService;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public AttachmentController(
            AttachmentService attachmentService,
            FileStorageService fileStorageService,
            ReportRepository reportRepository,
            UserRepository userRepository) {

        this.attachmentService = attachmentService;
        this.fileStorageService = fileStorageService;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    // Fotos einer Meldung auflisten
    @GetMapping("/reports/{reportId}/attachments")
    public List<Attachment> getAttachments(
            @PathVariable long reportId) {

        return attachmentService.findByReportId(reportId);
    }

    // Foto zur eigenen Meldung hochladen
    @PostMapping(
            value = "/reports/{reportId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Attachment> uploadAttachment(
            @PathVariable long reportId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        long currentUserId = getCurrentUserId(authentication);

        Report report = reportRepository.findById(reportId);

        if (report == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Report not found"
            );
        }

        // Vor dem Speichern Eigentümer überprüfen
        if (report.getUserId() == null ||
                !report.getUserId().equals(currentUserId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only upload photos to your own reports"
            );
        }

        String storedFilename = null;

        try {
            // Echte Bilddatei speichern
            storedFilename = fileStorageService.saveFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );

            // Dateinamen mit der Meldung in SQLite verknüpfen
            Attachment attachment =
                    attachmentService.addAttachment(
                            reportId,
                            storedFilename,
                            currentUserId
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(attachment);

        } catch (IllegalArgumentException e) {

            cleanupFile(storedFilename);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    e
            );

        } catch (SecurityException e) {

            cleanupFile(storedFilename);

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not allowed to upload attachment",
                    e
            );

        } catch (IOException e) {

            cleanupFile(storedFilename);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store image",
                    e
            );

        } catch (RuntimeException e) {

            cleanupFile(storedFilename);
            throw e;
        }
    }


@DeleteMapping("/attachments/{attachmentId}")
public ResponseEntity<Void> deleteAttachment(
        @PathVariable long attachmentId,
        Authentication authentication) {

    long currentUserId = getCurrentUserId(authentication);

    // 1. Anhang suchen
    Attachment attachment = attachmentService.findById(attachmentId);

    if (attachment == null) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Attachment not found"
        );
    }

    // 2. Zugehörige Meldung suchen
    Report report = reportRepository.findById(
            attachment.getReportId()
    );

    if (report == null) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Report not found"
        );
    }

    // 3. Eigentümer prüfen
    if (report.getUserId() == null ||
            !report.getUserId().equals(currentUserId)) {

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can only delete photos from your own reports"
        );
    }

    // 4. Erst Bilddatei löschen
    try {
        fileStorageService.deleteFile(
                attachment.getFilePath()
        );

    } catch (IOException e) {
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not delete image file",
                e
        );
    }

    // 5. Danach Datenbankeintrag löschen
    try {
        attachmentService.deleteAttachment(
                attachmentId,
                currentUserId
        );

    } catch (SecurityException e) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Not allowed to delete attachment",
                e
        );

    } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                e
        );
    }

    return ResponseEntity.noContent().build();
}


    // Bilddatei abrufen
    @GetMapping("/attachments/{attachmentId}/file")
    public ResponseEntity<Resource> getAttachmentFile(
            @PathVariable long attachmentId) {

        Attachment attachment =
                attachmentService.findById(attachmentId);

        if (attachment == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attachment not found"
            );
        }

        try {
            Path path = fileStorageService.loadFile(
                    attachment.getFilePath()
            );

            Resource resource = new FileSystemResource(path);

            MediaType mediaType =
                    attachment.getFilePath().endsWith(".png")
                            ? MediaType.IMAGE_PNG
                            : MediaType.IMAGE_JPEG;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Image file not found",
                    e
            );
        }
    }

    // Benutzer-ID aus der Anmeldung ermitteln
    private long getCurrentUserId(
            Authentication authentication) {

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

    // Gespeicherte Datei bei einem Fehler wieder entfernen
    private void cleanupFile(String storedFilename) {

        if (storedFilename == null) {
            return;
        }

        try {
            fileStorageService.deleteFile(storedFilename);
        } catch (IOException e) {
            // Fehler bei der Bereinigung wird später
            // über strukturiertes Logging behandelt.
        }
    }
}
