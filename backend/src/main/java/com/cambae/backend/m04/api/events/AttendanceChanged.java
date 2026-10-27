package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m04.api.dto.*;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record AttendanceChanged(
        long residentId, LocalDate date, AttendanceStatus status, boolean adHocSession, CareEventContext context)
        implements CareEvent {}
