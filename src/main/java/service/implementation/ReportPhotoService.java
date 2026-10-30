package service.implementation;

import java.io.IOException;
import java.io.InputStream;

import model.Attachment;
import model.Report;
import model.enums.Category;
import repository.interfaces.AttachmentRepository;
import repository.interfaces.ReportRepository;
import service.interfaces.FileStorageService;
import service.interfaces.ReportService;

public class ReportPhotoService {

    private final ReportService reportService;
    private final ReportRepository reportRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    public ReportPhotoService(
            ReportService reportService,
            ReportRepository reportRepository,
            AttachmentRepository attachmentRepository,
            FileStorageService fileStorageService) {

        this.reportService = reportService;
        this.reportRepository = reportRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
    }

    public Report createReportWithPhoto(
            Long userId,
            Category category,
            String description,
            double latitude,
            double longitude,
            InputStream imageStream,
            String originalFilename,
            String contentType) throws IOException {

        String storedFilename = null;
        Report createdReport = null;

        try {
            // 1. Bild prüfen und auf der Festplatte speichern
            storedFilename = fileStorageService.saveFile(
                    imageStream,
                    originalFilename,
                    contentType
            );

            // 2. Meldung erstellen
            // userId darf null sein (anonyme Meldung)
            createdReport = reportService.createReport(
                    userId,
                    category,
                    description,
                    latitude,
                    longitude
            );

            // 3. Foto der neu erstellten Meldung zuordnen
            
            Attachment attachment = new Attachment(
                    createdReport.getId(),
                    storedFilename
            );

            attachmentRepository.save(attachment);


            return createdReport;

        } catch (IOException | RuntimeException error) {

            // Wenn die Meldung bereits erstellt wurde,
            // versuchen wir sie wieder zu entfernen.
            // Durch ON DELETE CASCADE werden auch
            // zugehörige Attachment-Einträge gelöscht.
            if (createdReport != null) {
                try {
                    reportRepository.deleteById(
                            createdReport.getId()
                    );
                } catch (RuntimeException cleanupError) {
                    error.addSuppressed(cleanupError);
                }
            }

            // Auch die Bilddatei wieder entfernen
            if (storedFilename != null) {
                try {
                    fileStorageService.deleteFile(
                            storedFilename
                    );
                } catch (IOException | RuntimeException cleanupError) {
                    error.addSuppressed(cleanupError);
                }
            }

            throw error;
        }
    }
}