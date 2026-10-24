package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class NoMedicationOrderBody {

    @NotNull
    @JsonAlias({"chuyenId", "tripId"})
    private Long tripId;

    @NotEmpty
    @JsonAlias({"lyDo", "reason"})
    private String reason;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
