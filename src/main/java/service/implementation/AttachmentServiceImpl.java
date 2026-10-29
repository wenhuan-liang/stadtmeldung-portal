package service.implementation;

import model.Attachment;
import model.Report;

import repository.interfaces.AttachmentRepository;
import repository.interfaces.ReportRepository;

import service.interfaces.AttachmentService;

import java.util.List;

public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ReportRepository reportRepository;

    public AttachmentServiceImpl(
            AttachmentRepository attachmentRepository,
            ReportRepository reportRepository) {

        this.attachmentRepository = attachmentRepository;
        this.reportRepository = reportRepository;
    }

    @Override
    public Attachment addAttachment(long reportId,
                                    String filePath,
                                    long currentUserId) {

        validateId(reportId);
        validateId(currentUserId);
        validateFilePath(filePath);

        // Meldung laden und Eigentümer prüfen
        Report report = findReportAndCheckOwner(
                reportId,
                currentUserId
        );

        Attachment attachment = new Attachment(
                report.getId(),
                filePath.trim()
        );

        attachmentRepository.save(attachment);

        return attachment;
    }

    @Override
    public Attachment findById(long id) {

        validateId(id);

        return attachmentRepository.findById(id);
    }

    @Override
    public List<Attachment> findByReportId(long reportId) {

        validateId(reportId);

        return attachmentRepository.findByReportId(reportId);
    }

    @Override
    public void deleteAttachment(long attachmentId,
                                 long currentUserId) {

        validateId(attachmentId);
        validateId(currentUserId);

        Attachment attachment =
                attachmentRepository.findById(attachmentId);

        if (attachment == null) {
            throw new IllegalArgumentException(
                    "Attachment not found: " + attachmentId);
        }

        // Eigentümer der zugehörigen Meldung prüfen
        findReportAndCheckOwner(
                attachment.getReportId(),
                currentUserId
        );

        attachmentRepository.deleteById(attachmentId);
    }

    private Report findReportAndCheckOwner(
            long reportId,
            long currentUserId) {

        Report report = reportRepository.findById(reportId);

        if (report == null) {
            throw new IllegalArgumentException(
                    "Report not found: " + reportId);
        }

        if (report.getUserId() == null ||
                !report.getUserId().equals(currentUserId)) {

            throw new SecurityException(
                    "You can only manage attachments of your own reports");
        }

        return report;
    }

    private void validateFilePath(String filePath) {

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException(
                    "File path must not be empty");
        }
    }

    private void validateId(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID must be greater than zero");
        }
    }
}
