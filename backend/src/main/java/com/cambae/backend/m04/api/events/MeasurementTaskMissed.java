package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m04.api.dto.*;
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
