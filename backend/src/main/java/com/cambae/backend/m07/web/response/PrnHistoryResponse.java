package com.cambae.backend.m07.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrnHistoryResponse {

    @JsonProperty("donThuocId")
    private Long prescriptionId;

    @JsonProperty("cacLanDung")
    private List<PrnAdministrationResponse> administrations = new ArrayList<>();

    @JsonProperty("somNhatDuocDung")
    private Instant earliestNextAllowedTime;

    @JsonProperty("coTheDungNgay")
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

    public void setCanAdministerNow(boolean canAdministerNow) {
        this.canAdministerNow = canAdministerNow;
    }
}
