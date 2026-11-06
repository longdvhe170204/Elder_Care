package com.eldercare.backend.billing.web.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/** T012: dữ liệu chọn nguồn theo hợp đồng 010 đã được duyệt. */
public record CreateCostsFromEventsRequest(
        @NotNull Long residentId,
        @NotNull
        @Pattern(regexp = "RESIDENCY_DAY|DAY_CARE_SESSION|CARE_TASK_RESULT|DOSE|PRN_ADMINISTRATION|DISPENSE_RECORD|FAMILY_MEAL|OVERNIGHT_STAY|ACTIVITY_ATTENDANCE")
        String sourceType,
        @Size(min = 1) List<@NotNull String> sourceIds,
        @Size(min = 1) List<@NotNull @Valid SourceUnit> sourceUnits,
        @Valid DateRange dateRange) {
    @AssertTrue(message = "Chọn đúng một kiểu: id nguồn, đơn vị nguồn hoặc khoảng ngày.")
    @JsonIgnore
    public boolean isSelectionModeValid() {
        return (sourceIds == null ? 0 : 1) + (sourceUnits == null ? 0 : 1) + (dateRange == null ? 0 : 1) == 1;
    }

    @AssertTrue(message = "Danh sách nguồn không được lặp lại cùng một lựa chọn.")
    @JsonIgnore
    public boolean isSelectionUnique() {
        return (sourceIds == null || new HashSet<>(sourceIds).size() == sourceIds.size())
                && (sourceUnits == null || new HashSet<>(sourceUnits).size() == sourceUnits.size());
    }

    public record SourceUnit(@NotNull String sourceId, @NotNull String billingUnit) {}

    public record DateRange(@NotNull LocalDate from, @NotNull LocalDate to) {
        @AssertTrue(message = "Ngày bắt đầu không được sau ngày kết thúc.")
        @JsonIgnore
        public boolean isDateOrderValid() {
            return from == null || to == null || !from.isAfter(to);
        }
    }
}
