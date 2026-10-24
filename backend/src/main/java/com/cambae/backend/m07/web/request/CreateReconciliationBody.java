package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request body for creating a medication reconciliation form (FR-034, FR-034b).
 */
public class CreateReconciliationBody {

    @NotEmpty
    @JsonAlias({"lyDo", "reason"})
    private String reason; // TIEP_NHAN | TRO_VE_BENH_VIEN | THEO_YEU_CAU_CHUYEN_MON | ADMISSION | HOSPITAL_RETURN |
    // PROFESSIONAL_REQUEST

    @JsonAlias({"lyDoLap", "reasonDetail"})
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
