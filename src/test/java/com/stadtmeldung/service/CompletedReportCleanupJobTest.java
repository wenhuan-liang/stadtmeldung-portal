package com.stadtmeldung.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import model.Attachment;
import model.Comment;
import model.Report;
import model.enums.Category;
import model.enums.Status;
import org.junit.jupiter.api.Test;
import repository.interfaces.AttachmentRepository;
import repository.interfaces.CommentRepository;
import repository.interfaces.ReportRepository;

class CompletedReportCleanupJobTest {
    @Test
    void deletesReportAndAllAssociatedImagesWhenRepositoryConfirmsExpiry() {
        Report report = completedReport(1L, "/uploads/report.jpg");
        TestReports reports = new TestReports(report, true);
        TestComments comments = new TestComments(List.of(
                new Comment(1L, 2L, "Update", "/uploads/comment.png")));
        TestAttachments attachments = new TestAttachments(List.of(
                new Attachment(1L, "/uploads/attachment.webp")));
        TestImages images = new TestImages();

        CompletedReportCleanupJob job = new CompletedReportCleanupJob(
                reports, comments, attachments, images);

        job.deleteExpiredCompletedReports();

        assertTrue(reports.deleteAttempted);
        assertEquals(Set.of(
                "/uploads/report.jpg",
                "/uploads/comment.png",
                "/uploads/attachment.webp"), Set.copyOf(images.deletedPaths));
    }

    @Test
    void doesNotDeleteImagesIfReportIsNoLongerExpired() {
        Report report = completedReport(1L, "/uploads/report.jpg");
        TestImages images = new TestImages();
        CompletedReportCleanupJob job = new CompletedReportCleanupJob(
                new TestReports(report, false),
                new TestComments(List.of()),
                new TestAttachments(List.of()),
                images);

        job.deleteExpiredCompletedReports();

        assertTrue(images.deletedPaths.isEmpty());
    }

    private static Report completedReport(long id, String photoPath) {
        Report report = new Report(
                id,
                null,
                "Test report",
                Category.OTHER,
                "",
                "Description",
                photoPath,
                52.5,
                13.4,
                Status.COMPLETED,
                LocalDateTime.now(),
                LocalDateTime.now());
        return report;
    }

    private static final class TestReports implements ReportRepository {
        private final Report report;
        private final boolean deletionSucceeds;
        private boolean deleteAttempted;

        private TestReports(Report report, boolean deletionSucceeds) {
            this.report = report;
            this.deletionSucceeds = deletionSucceeds;
        }

        @Override
        public List<Report> findExpiredCompletedReports() {
            return List.of(report);
        }

        @Override
        public boolean deleteExpiredCompletedReport(long id) {
            deleteAttempted = true;
            return deletionSucceeds;
        }

        @Override
        public void save(Report value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Report findById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Report> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Report> findByStatus(Status status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(Report value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(long id, Status status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(long id) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestComments implements CommentRepository {
        private final List<Comment> comments;

        private TestComments(List<Comment> comments) {
            this.comments = comments;
        }

        @Override
        public List<Comment> findByReportId(long reportId) {
            return comments;
        }

        @Override
        public void save(Comment comment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Comment findById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Comment> findByUserId(long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Long> findReportIdsByUserId(long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(Comment comment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(long id) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestAttachments
            implements AttachmentRepository {
        private final List<Attachment> attachments;

        private TestAttachments(List<Attachment> attachments) {
            this.attachments = attachments;
        }

        @Override
        public List<Attachment> findByReportId(long reportId) {
            return attachments;
        }

        @Override
        public void save(Attachment attachment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Attachment findById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteByReportId(long reportId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestImages extends ImageDataService {
        private final List<String> deletedPaths = new ArrayList<>();

        @Override
        public void deleteSavedFile(String publicPath) throws IOException {
            deletedPaths.add(publicPath);
        }
    }
}
