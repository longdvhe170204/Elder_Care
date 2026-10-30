package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** T052: giờ phút trong cùng ngày theo hợp đồng. */
public record DayCareSessionWrite(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$") @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String startTime,

        @NotBlank @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$") @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String endTime) {
    public LocalTime start() {
        return parse(startTime);
    }

    public LocalTime end() {
        return parse(endTime);
    }

    private LocalTime parse(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("Giờ buổi không hợp lệ");
        }
    }
}
