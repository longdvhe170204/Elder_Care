package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateHandoverBody {

    @JsonAlias({"chuyenId", "tripId"})
    private Long tripId;

    @NotNull
    @JsonAlias({"nguoiNhanLoai", "recipientType"})
    private String recipientType; // STAFF | TRIP_LEADER | RELATIVE | NHAN_VIEN | TRUONG_DOAN | NGUOI_THAN

    @JsonAlias({"nguoiNhanId", "recipientId"})
    private Long recipientId;

    @JsonAlias({"nguoiNhanTen", "recipientName"})
    private String recipientName;

    @JsonAlias({"quanHe", "relationship"})
    private String relationship;

    @NotEmpty
    @Valid
    @JsonAlias({"dong", "items"})
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
