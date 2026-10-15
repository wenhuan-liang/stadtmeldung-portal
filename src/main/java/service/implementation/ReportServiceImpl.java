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

        validateReport(category, description, latitude, longitude);

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
                category,
                description.trim(),
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
                report.getCategory(),
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
    private void validateReport(Category category,
                                String description,
                                double latitude,
                                double longitude) {

        if (category == null) {
            throw new IllegalArgumentException(
                    "Category must not be null");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Description must not be empty");
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
