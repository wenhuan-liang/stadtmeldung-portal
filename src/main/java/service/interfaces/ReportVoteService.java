package service.interfaces;

import java.util.Set;

public interface ReportVoteService {
    boolean vote(long reportId, long userId);

    boolean removeVote(long reportId, long userId);

    int count(long reportId);

    boolean hasVoted(long reportId, long userId);

    Set<Long> findReportIdsByUserId(long userId);
}
