package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request body for recording a reported dose for an outing/trip (FR-027).
 */
public class ReportedDoseBody {

    @NotEmpty(message = "Reporter information is required (FR-027)")
    @JsonAlias({"nguoiBao", "reportedBy"})
    private String reportedBy;

    @JsonAlias({"ghiChu", "notes"})
    private String notes;

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
