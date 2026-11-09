package com.eldercare.backend.billing.web.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * T012: dữ liệu chọn nguồn theo hợp đồng 010 đã được duyệt ({@code CreateCostsFromEvents}): chọn đúng một kiểu
 * {@code sourceIds}, {@code sourceUnits} hoặc {@code dateRange}; mảng ít nhất một phần tử, không trùng, không phần tử
 * rỗng; {@code dateRange} đủ hai đầu, không ngược, chỉ cho phí lưu trú. Các hàm kiểm không vào thân JSON (FR-013).
 */
public record CreateCostsFromEventsRequest(
        @NotNull Long residentId,
        @NotNull String sourceType,
        List<String> sourceIds,
        List<SourceUnit> sourceUnits,
        DateRange dateRange) {

    /** {@code BillingSourceType} của api-010. */
    static final Set<String> SOURCE_TYPES = Set.of(
            "RESIDENCY_DAY",
            "DAY_CARE_SESSION",
            "CARE_TASK_RESULT",
            "DOSE",
            "PRN_ADMINISTRATION",
            "DISPENSE_RECORD",
            "FAMILY_MEAL",
            "OVERNIGHT_STAY",
            "ACTIVITY_ATTENDANCE");

    public record SourceUnit(String sourceId, String billingUnit) {}

    public record DateRange(LocalDate from, LocalDate to) {}

    @JsonIgnore
    @AssertTrue(message = "Loại nguồn không thuộc danh sách")
    public boolean isKnownSourceType() {
        return sourceType == null || SOURCE_TYPES.contains(sourceType);
    }

    @JsonIgnore
    @AssertTrue(message = "Chọn đúng một kiểu sourceIds, sourceUnits hoặc dateRange")
    public boolean isExactlyOneSelection() {
        int chosen = (sourceIds != null ? 1 : 0) + (sourceUnits != null ? 1 : 0) + (dateRange != null ? 1 : 0);
        return chosen == 1;
    }

    @JsonIgnore
    @AssertTrue(message = "sourceIds cần ít nhất một id, không trùng, không rỗng")
    public boolean isSourceIdsValid() {
        return sourceIds == null
                || distinctNonEmpty(sourceIds) && sourceIds.stream().allMatch(Objects::nonNull);
    }

    @JsonIgnore
    @AssertTrue(message = "sourceUnits cần ít nhất một đơn vị, không trùng, đủ sourceId và billingUnit")
    public boolean isSourceUnitsValid() {
        return sourceUnits == null
                || distinctNonEmpty(sourceUnits)
                        && sourceUnits.stream()
                                .allMatch(u -> u != null && u.sourceId() != null && u.billingUnit() != null);
    }

    @JsonIgnore
    @AssertTrue(message = "dateRange cần from, to, không ngược và chỉ dùng cho phí lưu trú")
    public boolean isDateRangeValid() {
        return dateRange == null
                || dateRange.from() != null
                        && dateRange.to() != null
                        && !dateRange.to().isBefore(dateRange.from())
                        && "RESIDENCY_DAY".equals(sourceType);
    }

    private static boolean distinctNonEmpty(List<?> values) {
        return !values.isEmpty() && new HashSet<>(values).size() == values.size();
    }
}
