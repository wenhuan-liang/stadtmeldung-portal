package repository.interfaces;

import java.util.Set;

public interface ReportVoteRepository {
    int countByReportId(long reportId);

    boolean existsByReportIdAndUserId(long reportId, long userId);

    boolean addVote(long reportId, long userId);

    boolean removeVote(long reportId, long userId);

    Set<Long> findReportIdsByUserId(long userId);
}
