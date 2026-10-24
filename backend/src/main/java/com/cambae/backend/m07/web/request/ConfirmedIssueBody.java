package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;

/**
 * Confirmed reason for a prescription issue (schema DonThuocMoi.xacNhanVanDe in api-006.yaml).
 * Standardized English field names with backward-compatible Vietnamese aliases.
 */
public class ConfirmedIssueBody {

    @NotEmpty
    @JsonAlias({"maVanDe"})
    private String issueCode;

    @NotEmpty
    @JsonAlias({"lyDo"})
    private String reason;

    public ConfirmedIssueBody() {}

    public ConfirmedIssueBody(String issueCode, String reason) {
        this.issueCode = issueCode;
        this.reason = reason;
    }

    // --- English Getters & Setters ---

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

    // --- Backward-Compatible Vietnamese Getters & Setters ---

    public String getMaVanDe() {
        return issueCode;
    }

    public void setMaVanDe(String maVanDe) {
        this.issueCode = maVanDe;
    }

    public String getLyDo() {
        return reason;
    }

    public void setLyDo(String lyDo) {
        this.reason = lyDo;
    }
}
