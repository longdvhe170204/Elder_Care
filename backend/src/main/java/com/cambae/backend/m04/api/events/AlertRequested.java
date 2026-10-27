package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m04.api.dto.*;
import com.cambae.backend.m04.api.spi.TaskSourcePort.SourceType;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record AlertRequested(
        String type,
        NotificationLevel level,
        long residentId,
        String source,
        SourceType basisType,
        long basisId,
        CareEventContext context)
        implements CareEvent {}
