package com.cambae.backend.m07.internal.service.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ReturnHandoverBody {

    @NotEmpty
    @Valid
    private List<ReturnItemBody> items;

    private String reason;

    public List<ReturnItemBody> getItems() {
        return items;
    }

    public void setItems(List<ReturnItemBody> items) {
        this.items = items;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
