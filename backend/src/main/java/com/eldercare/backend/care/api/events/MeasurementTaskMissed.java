package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record MeasurementTaskMissed(
        long taskId,
        long residentId,
        long measurementScheduleId,
        Set<Long> vitalSignIds,
        String reason,
        Importance importance,
        CareEventContext context)
        implements CareEvent {
    public MeasurementTaskMissed {
        vitalSignIds = Set.copyOf(vitalSignIds);
    }
}
