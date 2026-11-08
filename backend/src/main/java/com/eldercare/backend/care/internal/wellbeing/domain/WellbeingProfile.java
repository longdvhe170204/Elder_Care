package com.eldercare.backend.care.internal.wellbeing.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;

/** FR-053: số liệu đọc, không lưu thành bản ghi nghiệp vụ mới. */
public record WellbeingProfile(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate from,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate to,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Week> weeklyParticipation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Session> sessions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Day> dailyObservations,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PreferencesView preferences,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Alert> alerts) {
    public record Week(LocalDate weekStart, int presentCount) {}

    public record Session(
            long sessionId, Instant startsAt, String result, String participationLevel, String communicationLevel) {}

    public record Observation(
            long id, String resultType, Instant performedAt, Map<String, Object> values, String note) {}

    public record Day(LocalDate date, List<Observation> records) {}

    public record Alert(long id, String typeCode, String status) {}
}
