package repository.interfaces;

import java.util.List;

import model.Report;
import model.enums.Status;

public interface ReportRepository {

    void save(Report report);

    Report findById(long id);

    List<Report> findAll();

    List<Report> findByStatus(Status status);

    List<Report> findExpiredCompletedReports();

    void update(Report report);

    void updateStatus(long id, Status status);

    void deleteById(long id);

    boolean deleteExpiredCompletedReport(long id);
}
