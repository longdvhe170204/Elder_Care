package com.eldercare.backend.medication.internal.service.request;

import jakarta.validation.constraints.NotEmpty;

/**
 * Confirmed reason for a prescription issue (schema DonThuocMoi.xacNhanVanDe in api-006.yaml).
 */
public class ConfirmedIssueBody {

    @NotEmpty
    private String issueCode;

    @NotEmpty
    private String reason;

    public ConfirmedIssueBody() {}

    public ConfirmedIssueBody(String issueCode, String reason) {
        this.issueCode = issueCode;
        this.reason = reason;
    }

    public String getIssueCode() {
        return issueCode;
    }

    public void setIssueCode(String issueCode) {
        this.issueCode = issueCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
