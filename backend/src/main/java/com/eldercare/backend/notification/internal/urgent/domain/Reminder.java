package com.eldercare.backend.notification.internal.urgent.domain;

import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.urgent.domain.CallReminderTarget.Tier;
import java.time.Instant;

/** Một lượt nhắc gọi với trạng thái hiện hành và bậc của người đang được nhắc (FR-030). */
public record Reminder(
        long id,
        long chainId,
        RecipientType calleeType,
        long calleeId,
        Long residentId,
        Long floorAreaId,
        String sourceRecordType,
        String sourceRecordId,
        String status,
        String finalResult,
        String stopReason,
        Instant dueAt,
        long currentStaffId,
        Tier tier) {}
