package service.interfaces;

import java.util.List;

import model.Report;
import model.enums.Category;
import model.enums.Status;

public interface ReportService {

    Report createReport(Long userId, Category category,
                        String description, double latitude,
                        double longitude);

    Report findById(long id);

    List<Report> findAll();

    List<Report> findByStatus(Status status);

    void updateReport(Report report, long currentUserId);

    void updateStatus(long reportId, Status status,long currentUserId);

    void deleteReport(long id, long currentUserId);
}

