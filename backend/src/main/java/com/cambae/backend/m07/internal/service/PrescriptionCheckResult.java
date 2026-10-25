package com.cambae.backend.m07.internal.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of checking active ingredient duplication and allergy conflicts (FR-007, FR-008).
 */
public record PrescriptionCheckResult(List<PrescriptionIssueView> issues, List<PrescriptionIssueView> reviewItems) {

    public PrescriptionCheckResult {
        issues = issues != null ? Collections.unmodifiableList(new ArrayList<>(issues)) : List.of();
        reviewItems = reviewItems != null ? Collections.unmodifiableList(new ArrayList<>(reviewItems)) : List.of();
    }

    /**
     * Finds list of blocking issues without valid confirmation reasons.
     */
    public List<PrescriptionIssueView> findUnconfirmedIssues(Map<String, String> confirmationReasons) {
        List<PrescriptionIssueView> unconfirmed = new ArrayList<>();
        for (PrescriptionIssueView issue : issues) {
            String reason = confirmationReasons != null ? confirmationReasons.get(issue.issueCode()) : null;
            if (reason == null || reason.trim().isEmpty()) {
                unconfirmed.add(issue);
            }
        }
        return unconfirmed;
    }

    public boolean hasUnconfirmedIssues(Map<String, String> confirmationReasons) {
        return !findUnconfirmedIssues(confirmationReasons).isEmpty();
    }
}
