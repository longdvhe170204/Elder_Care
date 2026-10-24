package com.cambae.backend.m07.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HandoverResponse {

    private Long id;
    private Long residentId;
    private Long tripId;
    private Long absenceIntervalId;
    private String recipientType;
    private Long recipientId;
    private String recipientName;
    private String relationship;
    private Long dispensedById;
    private Instant handoverTime;
    private String status;
    private List<HandoverItemResponse> items = new ArrayList<>();

    public static class HandoverItemResponse {
        private Long id;
        private Long prescriptionId;
        private BigDecimal quantity;
        private Long familyMedicationBatchId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getPrescriptionId() {
            return prescriptionId;
        }

        public void setPrescriptionId(Long prescriptionId) {
            this.prescriptionId = prescriptionId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public Long getFamilyMedicationBatchId() {
            return familyMedicationBatchId;
        }

        public void setFamilyMedicationBatchId(Long familyMedicationBatchId) {
            this.familyMedicationBatchId = familyMedicationBatchId;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getAbsenceIntervalId() {
        return absenceIntervalId;
    }

    public void setAbsenceIntervalId(Long absenceIntervalId) {
        this.absenceIntervalId = absenceIntervalId;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Long getDispensedById() {
        return dispensedById;
    }

    public void setDispensedById(Long dispensedById) {
        this.dispensedById = dispensedById;
    }

    public Instant getHandoverTime() {
        return handoverTime;
    }

    public void setHandoverTime(Instant handoverTime) {
        this.handoverTime = handoverTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<HandoverItemResponse> getItems() {
        return items;
    }

    public void setItems(List<HandoverItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
