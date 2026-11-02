package com.eldercare.backend.notification.internal.urgent.domain;

import com.eldercare.backend.notification.internal.urgent.domain.CallReminderTarget.Tier;
import java.time.Instant;

/** Một lần chuyển nhắc của lượt (FR-030a). */
public record CallReminderHandoff(
        Long previousStaffId, long remindedStaffId, Tier tier, String reason, Instant dueAt, Instant at) {}
