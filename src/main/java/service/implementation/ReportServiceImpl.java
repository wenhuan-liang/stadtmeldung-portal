package service.implementation;

import java.util.List;

import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Role;
import model.enums.Status;
import repository.interfaces.ReportRepository;
import repository.interfaces.UserRepository;
import service.interfaces.ReportService;

public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(
            ReportRepository reportRepository,
            UserRepository userRepository) {

        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Report createReport(Long userId, Category category,
                               String description, double latitude,
                               double longitude) {

        String defectType = category == Category.POTHOLE
                ? "OTHER_DEFECT" : "";
        String title = category == null ? "" : category.name();
        return createReport(userId, title, category, defectType,
                description, "", latitude, longitude);
    }

    @Override
    public Report createReport(Long userId, String title, Category category,
                               String defectType, String description,
                               String photoPath, double latitude,
                               double longitude) {

        validateReport(title, category, defectType, description,
                latitude, longitude);

        // Prüfen, ob der Benutzer existiert
        if (userId != null) {
            validateId(userId);

            if (userRepository.findById(userId) == null) {
                throw new IllegalArgumentException(
                        "User not found: " + userId);
            }
        }

        Report report = new Report(
                userId,
                title.trim(),
                category,
                defectType == null ? "" : defectType.trim(),
                description.trim(),
                photoPath == null ? "" : photoPath,
                latitude,
                longitude
        );

        reportRepository.save(report);

        return report;
    }

    @Override
    public Report findById(long id) {

        validateId(id);

        return reportRepository.findById(id);
    }

    @Override
    public List<Report> findAll() {

        return reportRepository.findAll();
    }

    @Override
    public List<Report> findByStatus(Status status) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Status must not be null");
        }

        return reportRepository.findByStatus(status);
    }

    // Bürger dürfen nur ihre eigenen Meldungen bearbeiten
    @Override
    public void updateReport(Report report, long currentUserId) {

        if (report == null || report.getId() == null) {
            throw new IllegalArgumentException(
                    "Report and report ID must not be null");
        }

        validateId(report.getId());
        validateId(currentUserId);

        Report existingReport =
                reportRepository.findById(report.getId());

        if (existingReport == null) {
            throw new IllegalArgumentException(
                    "Report not found: " + report.getId());
        }

        // Eigentümer überprüfen
        if (existingReport.getUserId() == null ||
                !existingReport.getUserId().equals(currentUserId)) {

            throw new SecurityException(
                    "You can only edit your own reports");
        }

        validateReport(
                report.getTitle(),
                report.getCategory(),
                report.getDefectType(),
                report.getDescription(),
                report.getLatitude(),
                report.getLongitude()
        );

        // Eigentümer darf nicht verändert werden
        report.setUserId(existingReport.getUserId());

        // Bürger dürfen den Status nicht ändern
        report.setStatus(existingReport.getStatus());

        report.setDescription(report.getDescription().trim());

        reportRepository.update(report);
    }

    // Nur Mitarbeiter und Admins dürfen den Status ändern
    @Override
    public void updateStatus(long reportId, Status status,
                             long currentUserId) {

        validateId(reportId);
        validateId(currentUserId);

        if (status == null) {
            throw new IllegalArgumentException(
                    "Status must not be null");
        }

        // Angemeldeten Benutzer laden
        User currentUser =
                userRepository.findById(currentUserId);

        // Benutzerrolle überprüfen
        if (currentUser == null ||
                (currentUser.getRole() != Role.EMPLOYEE &&
                 currentUser.getRole() != Role.ADMIN)) {

            throw new SecurityException(
                    "Only employees and admins can update report status");
        }

        Report report = reportRepository.findById(reportId);

        if (report == null) {
            throw new IllegalArgumentException(
                    "Report not found: " + reportId);
        }

        report.setStatus(status);

        reportRepository.update(report);
    }

    // Bürger dürfen nur ihre eigenen Meldungen löschen
    @Override
    public void deleteReport(long id, long currentUserId) {

        validateId(id);
        validateId(currentUserId);

        Report report = reportRepository.findById(id);

        if (report == null) {
            throw new IllegalArgumentException(
                    "Report not found: " + id);
        }

        // Eigentümer überprüfen
        if (report.getUserId() == null ||
                !report.getUserId().equals(currentUserId)) {

            throw new SecurityException(
                    "You can only delete your own reports");
        }

        reportRepository.deleteById(id);
    }

    // Validierung der Meldungsdaten
    private void validateReport(String title, Category category,
                                String defectType,
                                String description,
                                double latitude,
                                double longitude) {

        if (title == null || title.isBlank() || title.length() > 120) {
            throw new IllegalArgumentException(
                    "Title must contain between 1 and 120 characters");
        }

        if (category == null) {
            throw new IllegalArgumentException(
                    "Category must not be null");
        }

        if (category == Category.POTHOLE &&
                (defectType == null ||
                 !java.util.Set.of(
                         "POTHOLE",
                         "ROAD_SURFACE",
                         "SIDEWALK",
                         "STREETLIGHT",
                         "TRAFFIC_SIGN",
                         "BENCH_OR_PLAYGROUND",
                         "OTHER_DEFECT"
                 ).contains(defectType))) {
            throw new IllegalArgumentException(
                    "A concrete defect type must be selected");
        }

        if (category != Category.POTHOLE &&
                defectType != null && !defectType.isBlank()) {
            throw new IllegalArgumentException(
                    "Defect type is only valid for defect reports");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Description must not be empty");
        }
        if (description.length() > 2000) {
            throw new IllegalArgumentException(
                    "Description must not exceed 2000 characters");
        }

        if (!Double.isFinite(latitude) ||
                latitude < -90 || latitude > 90) {

            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90");
        }

        if (!Double.isFinite(longitude) ||
                longitude < -180 || longitude > 180) {

            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180");
        }
    }

    private void validateId(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID must be greater than zero");
        }
    }
}
