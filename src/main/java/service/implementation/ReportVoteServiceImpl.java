package service.implementation;

import model.User;
import model.enums.Role;
import repository.interfaces.ReportRepository;
import repository.interfaces.ReportVoteRepository;
import repository.interfaces.UserRepository;
import service.interfaces.ReportVoteService;
import java.util.Set;

public final class ReportVoteServiceImpl implements ReportVoteService {
    private final ReportVoteRepository voteRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public ReportVoteServiceImpl(ReportVoteRepository voteRepository,
                                 ReportRepository reportRepository,
                                 UserRepository userRepository) {
        this.voteRepository = voteRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean vote(long reportId, long userId) {
        validateCitizenVote(reportId, userId);
        return voteRepository.addVote(reportId, userId);
    }

    @Override
    public boolean removeVote(long reportId, long userId) {
        validateCitizenVote(reportId, userId);
        return voteRepository.removeVote(reportId, userId);
    }

    @Override
    public int count(long reportId) {
        if (reportId <= 0 || reportRepository.findById(reportId) == null) {
            throw new IllegalArgumentException("Report not found");
        }
        return voteRepository.countByReportId(reportId);
    }

    @Override
    public boolean hasVoted(long reportId, long userId) {
        validateIds(reportId, userId);
        return voteRepository.existsByReportIdAndUserId(reportId, userId);
    }

    @Override
    public Set<Long> findReportIdsByUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than zero");
        }
        return voteRepository.findReportIdsByUserId(userId);
    }

    private static void validateIds(long reportId, long userId) {
        if (reportId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("IDs must be greater than zero");
        }
    }

    private void validateCitizenVote(long reportId, long userId) {
        validateIds(reportId, userId);
        if (reportRepository.findById(reportId) == null) {
            throw new IllegalArgumentException("Report not found");
        }
        User user = userRepository.findById(userId);
        if (user == null || user.getRole() != Role.CITIZEN) {
            throw new SecurityException("Only citizens can support reports");
        }
    }
}
