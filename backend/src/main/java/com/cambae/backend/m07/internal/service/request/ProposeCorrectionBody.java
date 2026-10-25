package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request body for proposing a correction to a reconciliation form (FR-039, BR-M07-17).
 */
public class ProposeCorrectionBody {

    @NotNull
    private Map<String, Object> correctedContent;

    @NotEmpty
    private String reason;

    public Map<String, Object> getCorrectedContent() {
        return correctedContent;
    }

    public void setCorrectedContent(Map<String, Object> correctedContent) {
        this.correctedContent = correctedContent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
