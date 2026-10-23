package com.cambae.backend.m09.shift.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m09.shift.internal.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Các thân lệnh lịch ca, tách khỏi record nghiệp vụ. */
public final class ShiftRequests {
    private ShiftRequests() {}

    public record Template(
            @NotBlank String name,
            @NotBlank String startTime,
            @NotBlank String endTime,
            @NotBlank String scopeType,
            Boolean handoverRequired,
            Boolean active) {
        public TemplateInput input() {
            try {
                return new TemplateInput(
                        name,
                        LocalTime.parse(startTime),
                        LocalTime.parse(endTime),
                        scopeType,
                        handoverRequired,
                        !Boolean.FALSE.equals(active));
            } catch (DateTimeException e) {
                throw invalid("Giờ mẫu ca không hợp lệ");
            }
        }
    }

    public record Schedule(
            @NotBlank String month,

            @NotBlank @Schema(allowableValues = {"FLOOR", "AREA", "FACILITY"})
            String scopeType,

            Long scopeId) {
        public YearMonth monthValue() {
            return parseMonth(month);
        }
    }

    public record Member(long staffId, Boolean onDutyDoctor) {}

    @Schema(name = "NewShift")
    public record Shift(
            @NotNull Long shiftTemplateId,
            @NotNull LocalDate date,
            Instant startsAt,
            Instant endsAt,
            List<Member> staff,
            Long leadStaffId,
            Boolean confirmWarnings,
            Reason reason) {
        public ShiftInput input() {
            return new ShiftInput(
                    shiftTemplateId,
                    date,
                    startsAt,
                    endsAt,
                    staff == null
                            ? List.of()
                            : staff.stream()
                                    .map(m -> new ShiftInput.Member(m.staffId(), Boolean.TRUE.equals(m.onDutyDoctor())))
                                    .toList(),
                    leadStaffId,
                    Boolean.TRUE.equals(confirmWarnings),
                    reason);
        }
    }

    public record Publish(Reason reason, List<Long> assignmentIdsToRemove, Boolean confirmWarnings) {}

    public static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeException | NullPointerException e) {
            throw invalid("Tháng của lịch không hợp lệ");
        }
    }

    private static BusinessException invalid(String detail) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", detail);
    }
}
