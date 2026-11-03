package com.eldercare.backend.care.api.spi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/** 007 cài; record phải tham gia transaction đang có (MANDATORY), FR-034. */
public interface VitalSignPort {
    RecordingResult record(
            long residentId,
            String vitalSignType,
            BigDecimal value,
            Instant measuredAt,
            long measuredById,
            long taskId,
            Optional<Instant> deviceAt);

    enum RecordingStatus {
        SAVED,
        BLOCKED,
        AWAITING_REMEASUREMENT
    }

    record RecordingResult(
            RecordingStatus status,
            Optional<Long> vitalSignRecordId,
            Optional<String> reason,
            Optional<Long> remeasurementSessionId) {}
}
