package com.stadtmeldung.controller;

import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import service.interfaces.ReportVoteService;

import java.util.Map;

@RestController
@RequestMapping("/api/reports/{reportId}/votes")
public class ReportVoteController {
    private final ReportVoteService reportVoteService;
    private final AuthenticatedUserResolver userResolver;

    public ReportVoteController(ReportVoteService reportVoteService,
                                AuthenticatedUserResolver userResolver) {
        this.reportVoteService = reportVoteService;
        this.userResolver = userResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> vote(
            @PathVariable long reportId,
            Authentication authentication) {
        User citizen = userResolver.requireCitizen(authentication);
        boolean added = reportVoteService.vote(reportId, citizen.getId());
        if (!added) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sie haben bereits für diese Meldung gestimmt.");
        }
        return voteResponse(reportId, true);
    }

    @DeleteMapping
    public Map<String, Object> removeVote(
            @PathVariable long reportId,
            Authentication authentication) {
        User citizen = userResolver.requireCitizen(authentication);
        reportVoteService.removeVote(reportId, citizen.getId());
        return voteResponse(reportId, false);
    }

    private Map<String, Object> voteResponse(long reportId,
                                             boolean hasVoted) {
        return Map.of(
                "voteCount", reportVoteService.count(reportId),
                "hasVoted", hasVoted);
    }
}
