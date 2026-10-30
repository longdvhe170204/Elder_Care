package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.notification.*;
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
