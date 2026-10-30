package com.cambae.backend.m07.internal.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrnHistoryResponse {

    private Long prescriptionId;
    private List<PrnAdministrationResponse> administrations = new ArrayList<>();
    private Instant earliestNextAllowedTime;
    private boolean canAdministerNow;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public List<PrnAdministrationResponse> getAdministrations() {
        return administrations;
    }

    public void setAdministrations(List<PrnAdministrationResponse> administrations) {
        this.administrations = administrations != null ? administrations : new ArrayList<>();
    }

    public Instant getEarliestNextAllowedTime() {
        return earliestNextAllowedTime;
    }

    public void setEarliestNextAllowedTime(Instant earliestNextAllowedTime) {
        this.earliestNextAllowedTime = earliestNextAllowedTime;
    }

    public boolean isCanAdministerNow() {
        return canAdministerNow;
    }

    public boolean canAdministerNow() {
        return canAdministerNow;
    }

    public void setCanAdministerNow(boolean canAdministerNow) {
        this.canAdministerNow = canAdministerNow;
    }
}
