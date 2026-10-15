
package service.interfaces;

import java.util.List;

import model.Attachment;

public interface AttachmentService {

    Attachment addAttachment(
            long reportId,
            String filePath,
            long currentUserId
    );

    Attachment findById(long id);

    List<Attachment> findByReportId(long reportId);

    void deleteAttachment(
            long attachmentId,
            long currentUserId
    );
}

