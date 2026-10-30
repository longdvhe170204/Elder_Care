package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
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
