package com.eldercare.backend.medication.internal.service.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateHandoverBody {

    private Long tripId;

    @NotNull
    private String recipientType; // STAFF | TRIP_LEADER | RELATIVE

    private Long recipientId;

    private String recipientName;

    private String relationship;

    @NotEmpty
    @Valid
    private List<HandoverItemBody> items;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
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

    public List<HandoverItemBody> getItems() {
        return items;
    }

    public void setItems(List<HandoverItemBody> items) {
        this.items = items;
    }
}
