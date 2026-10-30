package com.stadtmeldung.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Status;
import repository.interfaces.UserRepository;
import service.implementation.ReportPhotoService;
import service.interfaces.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
private final UserRepository userRepository;
private final ReportPhotoService reportPhotoService;

    
public ReportController(
        ReportService reportService,
        UserRepository userRepository,
        ReportPhotoService reportPhotoService) {

    this.reportService = reportService;
    this.userRepository = userRepository;
    this.reportPhotoService = reportPhotoService;
}


    // Alle Meldungen abrufen
    @GetMapping
    public List<Report> getAllReports(
            @RequestParam(required = false) Status status) {

        if (status != null) {
            return reportService.findByStatus(status);
        }

        return reportService.findAll();
    }

    // Einzelne Meldung abrufen
    @GetMapping("/{id}")
    public Report getReportById(@PathVariable long id) {

        Report report = reportService.findById(id);

        if (report == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Report not found"
            );
        }

        return report;
    }

    // Meldung erstellen, auch anonym
    @PostMapping
    public ResponseEntity<Report> createReport(
            @RequestBody CreateReportRequest request,
            Authentication authentication) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing report data"
            );
        }

        Long userId = getOptionalUserId(authentication);

        try {
            Report report = reportService.createReport(
                    userId,
                    request.category(),
                    request.description(),
                    request.latitude(),
                    request.longitude()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(report);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }



@PostMapping(
        value = "/with-photo",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public ResponseEntity<Report> createReportWithPhoto(
        @RequestParam Category category,
        @RequestParam String description,
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam("file") MultipartFile file,
        Authentication authentication) {

    Long userId = getOptionalUserId(authentication);

    if (file == null || file.isEmpty()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A photo is required"
        );
    }

    try {
        Report report = reportPhotoService.createReportWithPhoto(
                userId,
                category,
                description,
                latitude,
                longitude,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(report);

    } catch (IllegalArgumentException e) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                e
        );

    } catch (IOException e) {

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not save report photo",
                e
        );
    }
}




    // Eigene Meldung bearbeiten
    @PutMapping("/{id}")
    public Report updateReport(
            @PathVariable long id,
            @RequestBody UpdateReportRequest request,
            Authentication authentication) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing report data"
            );
        }

        long currentUserId = getRequiredUserId(authentication);

        try {
            Report report = reportService.findById(id);

            if (report == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Report not found"
                );
            }

            report.setCategory(request.category());
            report.setDescription(request.description());
            report.setLatitude(request.latitude());
            report.setLongitude(request.longitude());

            // Service prüft, ob die Meldung dem Benutzer gehört
            reportService.updateReport(report, currentUserId);

            // Aktualisierte Werte aus SQLite laden
            return reportService.findById(id);

        } catch (SecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only edit your own reports"
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }


    // Bearbeitungsstatus einer Meldung ändern
    @PatchMapping("/{id}/status")
    public Report updateReportStatus(
            @PathVariable long id,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication) {

        if (request == null || request.status() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status is required"
            );
        }

        // Benutzer-ID aus der angemeldeten Sitzung ermitteln
        long currentUserId = getRequiredUserId(authentication);

        try {
            reportService.updateStatus(
                    id,
                    request.status(),
                    currentUserId
            );

            // Aktualisierte Meldung aus der Datenbank laden
            return reportService.findById(id);

        } catch (SecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only employees and admins can change report status"
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }


    // Benutzer-ID aus der Anmeldung ermitteln
    private Long getOptionalUserId(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {

            return null;
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

    private long getRequiredUserId(Authentication authentication) {

        Long userId = getOptionalUserId(authentication);

        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Login required"
            );
        }

        return userId;
    }

    public record CreateReportRequest(
            Category category,
            String description,
            double latitude,
            double longitude
    ) {}

    public record UpdateReportRequest(
            Category category,
            String description,
            double latitude,
            double longitude
    ) {}

    public record UpdateStatusRequest(Status status) {}
}