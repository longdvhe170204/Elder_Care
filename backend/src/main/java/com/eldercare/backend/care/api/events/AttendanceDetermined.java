package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record AttendanceDetermined(
        long residentId, LocalDate date, AttendanceStatus status, boolean adHocSession, CareEventContext context)
        implements CareEvent {}
