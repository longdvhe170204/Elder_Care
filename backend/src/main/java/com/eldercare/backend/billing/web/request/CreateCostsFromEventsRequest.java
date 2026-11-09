package com.eldercare.backend.billing.web.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/** T012: dữ liệu chọn nguồn theo hợp đồng 010 đã được duyệt. */
@Schema(name = "CreateCostsFromEvents")
public record CreateCostsFromEventsRequest(
        @NotNull @Positive Long residentId,

        @NotNull
        @Schema(
                allowableValues = {
                    "RESIDENCY_DAY",
                    "DAY_CARE_SESSION",
                    "CARE_TASK_RESULT",
                    "DOSE",
                    "PRN_ADMINISTRATION",
                    "DISPENSE_RECORD",
                    "FAMILY_MEAL",
                    "OVERNIGHT_STAY",
                    "ACTIVITY_ATTENDANCE"
                })
        @Pattern(
                regexp =
                        "RESIDENCY_DAY|DAY_CARE_SESSION|CARE_TASK_RESULT|DOSE|PRN_ADMINISTRATION|DISPENSE_RECORD|FAMILY_MEAL|OVERNIGHT_STAY|ACTIVITY_ATTENDANCE")
        String sourceType,

        @Size(min = 1) List<@NotBlank String> sourceIds,
        @Size(min = 1) List<@NotNull @Valid SourceUnit> sourceUnits,
        @Valid DateRange dateRange) {
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Chọn đúng một kiểu nguồn, không lặp đơn vị; khoảng ngày chỉ dùng cho phí lưu trú")
    public boolean isSelectionValid() {
        int choices = (sourceIds == null ? 0 : 1) + (sourceUnits == null ? 0 : 1) + (dateRange == null ? 0 : 1);
        return choices == 1
                && (sourceIds == null || new HashSet<>(sourceIds).size() == sourceIds.size())
                && (sourceUnits == null || new HashSet<>(sourceUnits).size() == sourceUnits.size())
                && (dateRange == null || "RESIDENCY_DAY".equals(sourceType));
    }

    @Schema(name = "CostSourceUnit")
    public record SourceUnit(
            @NotBlank String sourceId, @NotBlank String billingUnit) {}

    @Schema(name = "CostSourceDateRange")
    public record DateRange(
            @NotNull LocalDate from, @NotNull LocalDate to) {
        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = "Ngày bắt đầu phải không sau ngày kết thúc")
        public boolean isOrdered() {
            return from == null || to == null || !from.isAfter(to);
        }
    }
}
