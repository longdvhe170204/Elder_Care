package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.family.internal.visit.service.VisitService.Registration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Thân đăng ký thăm (api-012 {@code VisitRegistration}; FR-029): khung giờ dạng "HH:mm"; {@code familyMemberId} khi Hành
 * chính đăng ký thay; người đi cùng có họ tên, quan hệ, số điện thoại tùy chọn.
 */
@Schema(name = "VisitRegistration")
public record VisitRegistrationRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        Long familyMemberId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String startTime,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String endTime,

        @Size(max = 20) List<CompanionBody> companions) {

    /** Người đi cùng ({@code Companion}). */
    @Schema(name = "Companion")
    public record CompanionBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Size(max = 200)
            String fullName,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Size(max = 100)
            String relationship,

            @Size(max = 20) String phoneNumber) {

        public Visit.Companion companion() {
            return new Visit.Companion(fullName, relationship, phoneNumber, false, false);
        }

        public static List<Visit.Companion> all(List<CompanionBody> bodies) {
            return bodies == null
                    ? List.of()
                    : bodies.stream().map(b -> b == null ? null : b.companion()).toList();
        }
    }

    public Registration registration() {
        return new Registration(
                residentId, familyMemberId, date, time(startTime), time(endTime), CompanionBody.all(companions));
    }

    static LocalTime time(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("Giờ phải có dạng HH:mm");
        }
    }
}
