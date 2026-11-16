package com.stadtmeldung.service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import model.Attachment;
import model.Comment;
import model.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import repository.interfaces.AttachmentRepository;
import repository.interfaces.CommentRepository;
import repository.interfaces.ReportRepository;

@Component
public class CompletedReportCleanupJob {
    private static final Logger logger =
            LoggerFactory.getLogger(CompletedReportCleanupJob.class);

    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final ImageDataService imageDataService;

    public CompletedReportCleanupJob(
            ReportRepository reportRepository,
            CommentRepository commentRepository,
            AttachmentRepository attachmentRepository,
            ImageDataService imageDataService) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.imageDataService = imageDataService;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 0L)
    public void deleteExpiredCompletedReports() {
        for (Report report : reportRepository.findExpiredCompletedReports()) {
            long reportId = report.getId();
            Set<String> filePaths = collectFilePaths(reportId, report);

            if (!reportRepository.deleteExpiredCompletedReport(reportId)) {
                continue;
            }

            logger.info("Automatically deleted completed report {}",
                    reportId);
            deleteFiles(reportId, filePaths);
        }
    }

    private Set<String> collectFilePaths(long reportId, Report report) {
        Set<String> filePaths = new LinkedHashSet<>();
        addPath(filePaths, report.getPhotoPath());
        for (Comment comment : commentRepository.findByReportId(reportId)) {
            addPath(filePaths, comment.getPhotoPath());
        }
        for (Attachment attachment :
                attachmentRepository.findByReportId(reportId)) {
            addPath(filePaths, attachment.getFilePath());
        }
        return filePaths;
    }

    private void deleteFiles(long reportId, Set<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                imageDataService.deleteSavedFile(filePath);
            } catch (IOException exception) {
                logger.error(
                        "Report {} was deleted, but file {} could not be removed",
                        reportId, filePath, exception);
            }
        }
    }

    private static void addPath(Set<String> filePaths, String filePath) {
        if (filePath != null && !filePath.isBlank()) {
            filePaths.add(filePath);
        }
    }
}
