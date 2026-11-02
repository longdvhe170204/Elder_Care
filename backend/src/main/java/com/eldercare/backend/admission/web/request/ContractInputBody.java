package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Thân lập và sửa hợp đồng Nháp (contracts/api-004.yaml, schema {@code ContractInput}; FR-019, FR-025a). Các kiểu
 * dòng con dùng chung với phản hồi {@code Contract}.
 */
@Schema(name = "ContractInput")
public record ContractInputBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ResidenceType residenceType,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,

        @Schema(description = "Bắt buộc với nội trú ngắn ngày")
        LocalDate endDate,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        CareLevel careLevel,

        Long roomTypeId,
        Long bedId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FeeBillingPeriod residencyFeeBillingPeriod,

        @Valid List<ServiceLineBody> services,
        Boolean noDepositRequired,
        Long depositAmount,
        @Size(max = 2000) String bedHoldPolicy,
        @Valid List<ArrivalLineBody> arrivalSchedule,
        @Size(max = 2000) String terminationCondition) {

    /** Kỳ tính phí lưu trú của hợp đồng (schema: {@code MONTH}, {@code DAY}, {@code SESSION}). */
    public enum FeeBillingPeriod {
        MONTH,
        DAY,
        SESSION
    }

    public record ServiceLineBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long serviceId,

            Boolean inPackage,
            BigDecimal quantity,
            Long customUnitPrice) {}

    public record ArrivalLineBody(
            @Min(1) @Max(7) Integer dayOfWeek,
            LocalDate date,
            @Schema(example = "07:30") String arrivalTime,
            @Schema(example = "16:30") String departureTime) {}

    public ContractContent content() {
        return new ContractContent(
                residenceType,
                startDate,
                endDate,
                careLevel,
                roomTypeId,
                bedId,
                BillingPeriod.valueOf(residencyFeeBillingPeriod.name()),
                nullToEmpty(services).stream()
                        .map(line -> new ContractContent.ServiceLine(
                                line.serviceId(),
                                Boolean.TRUE.equals(line.inPackage()),
                                line.quantity(),
                                line.customUnitPrice() == null ? null : BigDecimal.valueOf(line.customUnitPrice())))
                        .toList(),
                Boolean.TRUE.equals(noDepositRequired),
                depositAmount == null ? null : BigDecimal.valueOf(depositAmount),
                bedHoldPolicy,
                nullToEmpty(arrivalSchedule).stream()
                        .map(line -> new ContractContent.ArrivalLine(
                                line.dayOfWeek(), line.date(), time(line.arrivalTime()), time(line.departureTime())))
                        .toList(),
                terminationCondition);
    }

    private static LocalTime time(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new InvalidValueException("Giờ đến, giờ về phải có dạng HH:mm");
        }
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
