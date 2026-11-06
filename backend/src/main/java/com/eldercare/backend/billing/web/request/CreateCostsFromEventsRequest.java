package com.eldercare.backend.billing.web.request;

import java.time.LocalDate;
import java.util.List;

/** T012: dữ liệu chọn nguồn theo hợp đồng 010 đã được duyệt. */
public record CreateCostsFromEventsRequest(
        Long residentId,
        String sourceType,
        List<String> sourceIds,
        List<SourceUnit> sourceUnits,
        DateRange dateRange) {
    public record SourceUnit(String sourceId, String billingUnit) {}
    public record DateRange(LocalDate from, LocalDate to) {}
}
