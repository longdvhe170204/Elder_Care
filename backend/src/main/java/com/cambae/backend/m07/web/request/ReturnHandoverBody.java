package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ReturnHandoverBody {

    @NotEmpty
    @Valid
    @JsonAlias({"dong", "items"})
    private List<ReturnItemBody> items;

    @JsonAlias({"lyDo", "reason"})
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
