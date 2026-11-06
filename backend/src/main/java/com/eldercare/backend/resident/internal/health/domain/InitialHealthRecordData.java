package com.eldercare.backend.resident.internal.health.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Nội dung hồ sơ sức khỏe ban đầu ở {@code simple_record} loại {@link #KIND} (001 T084, data-model 001 mục 0.1; FR-017):
 * người cao tuổi ở {@code resident_id}, trạng thái {@code DRAFT} hoặc {@code CONFIRMED}; xác nhận thì khóa dòng
 * ({@code locked_at} là lúc xác nhận, thay {@code trg_initial_health_record_no_update}); khóa {@link #uniqueKey} giữ tối
 * đa một hồ sơ mỗi người.
 */
public record InitialHealthRecordData(
        String bloodType,
        String mobility,
        String selfCareAbility,
        String cognition,
        String initialNeedsNote,
        String nutritionNeeds,
        String currentMedications,
        String rehabilitationNeeds,
        long performedBy,
        Instant performedAt,
        Long confirmedBy) {

    public static final SharedKind<InitialHealthRecordData> KIND =
            SharedKind.of("M01.INITIAL_HEALTH_RECORD", InitialHealthRecordData.class);

    public static final String DRAFT = "DRAFT";
    public static final String CONFIRMED = "CONFIRMED";

    public static String uniqueKey(long residentId) {
        return "M01.IHR:" + residentId;
    }
}
