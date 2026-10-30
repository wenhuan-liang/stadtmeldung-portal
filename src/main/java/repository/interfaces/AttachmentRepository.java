package repository.interfaces;

import java.util.List;

import model.Attachment;

public interface AttachmentRepository {

    void save(Attachment attachment);

    Attachment findById(long id);

    List<Attachment> findByReportId(long reportId);

    void deleteById(long id);

    void deleteByReportId(long reportId);
}