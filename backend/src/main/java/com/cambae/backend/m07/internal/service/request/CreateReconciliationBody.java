package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.constraints.NotEmpty;

/**
 * Request body for creating a medication reconciliation form (FR-034, FR-034b).
 */
public class CreateReconciliationBody {

    @NotEmpty
    private String reason; // ADMISSION | HOSPITAL_RETURN | PROFESSIONAL_REQUEST

    private String reasonDetail;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }
}
