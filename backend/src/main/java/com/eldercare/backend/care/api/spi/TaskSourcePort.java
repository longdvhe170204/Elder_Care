package com.eldercare.backend.care.api.spi;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Mỗi nguồn 007, 011, 014 cài một bean; không gọi ngược API của nguồn. */
public interface TaskSourcePort {
    Set<SourceType> managedTypes();

    SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt);

    /** FR-017: dữ liệu nguồn đã có trong khoảng ca [from,to), không đề xuất công việc. */
    List<SourceView> sources(long residentId, Instant from, Instant to);

    record SourceView(SourceType type, long id, Instant at, String description) {}

    default boolean confirmServing(long mealId, long residentId) {
        return true;
    }

    enum SourceType {
        CARE_PLAN_ITEM,
        MEASUREMENT,
        MEAL,
        ACTIVITY_SESSION,
        POST_FALL_MONITORING,
        CONTACT_MONITORING,
        WATER_INTAKE_ALERT,
        SHIFT_START_CHECK,
        SUPERVISION,
        AD_HOC,
        AD_HOC_RECORDING,
        REWORK
    }

    record SourceValidation(boolean valid, boolean residentMatches, boolean timeMatches, boolean prescribed) {}
}
