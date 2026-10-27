package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m04.api.dto.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record BillableCareResultRecorded(
        long resultId,
        long residentId,
        long taskTypeId,
        long serviceId,
        BigDecimal quantity,
        Instant performedAt,
        CareEventContext context)
        implements CareEvent {}
