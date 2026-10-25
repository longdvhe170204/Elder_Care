package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.internal.contract.domain.ContractContent;
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
 * Thân lập và sửa hợp đồng Nháp (contracts/api-004.yaml, schema {@code ContractInput}; FR-019, FR-025a, FR-050). Các kiểu
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
        @Valid List<AbsencePolicyLineBody> absencePolicyOverrides,
        @Valid List<ArrivalLineBody> arrivalSchedule,
        @Size(max = 2000) String terminationCondition) {

    /** Kỳ tính phí lưu trú của hợp đồng (schema: {@code MONTH}, {@code DAY}, {@code SESSION}). */
    public enum FeeBillingPeriod {
        MONTH,
        DAY,
        SESSION
    }

    /** Dòng chính sách vắng: {@code holdBed}. */
    public enum HoldBed {
        YES,
        NO,
        PER_CONTRACT,
        MANAGER_DECIDES
    }

    public record ServiceLineBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long serviceId,

            Boolean inPackage,
            BigDecimal quantity,
            Long customUnitPrice) {}

    public record AbsencePolicyLineBody(
            ResidenceType residenceType,
            String absenceType,
            Integer fromDay,
            Integer toDay,

            @Schema(description = "Số phần trăm, PER_CONTRACT hoặc MANAGER_DECIDES")
            String coefficient,

            HoldBed holdBed) {}

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
                nullToEmpty(absencePolicyOverrides).stream()
                        .map(line -> {
                            if (line.residenceType() == null
                                    || line.absenceType() == null
                                    || line.coefficient() == null) {
                                throw new InvalidValueException(
                                        "Dòng chính sách vắng cần residenceType, absenceType và coefficient");
                            }
                            return new ContractContent.AbsencePolicyLine(
                                    line.residenceType(),
                                    line.absenceType(),
                                    line.fromDay(),
                                    line.toDay(),
                                    line.coefficient(),
                                    line.holdBed() == null
                                            ? null
                                            : line.holdBed().name());
                        })
                        .toList(),
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
