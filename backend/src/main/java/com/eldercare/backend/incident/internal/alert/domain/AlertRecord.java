package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;

/**
 * Bản ghi cảnh báo trong cơ sở dữ liệu (FR-026 đến FR-034).
 */
public record AlertRecord(
        long id,
        Long residentId,
        Long floorAreaId,
        String type,
        String typeLock,
        IncidentLevel level,
        IncidentLevel levelAtReceived,
        int occurrenceCount,
        Instant firstAt,
        Instant latestAt,
        AlertStatus status,
        int escalationLevel,
        Instant acknowledgementDeadline,
        Instant maxEscalationAt,
        int maxReminderCount,
        Long assigneeId,
        boolean handoverAcknowledged,
        Long receiverId,
        Instant receivedAt,
        String resolution,
        String closeReason,
        Long closedById,
        boolean closedBySystem,
        Instant closedAt,
        Long incidentId,
        Long criticalFlagId,
        boolean noOnDutyDoctor,
        boolean escalationStopped,
        Long createdById,
        String description,
        Long requesterId,
        Instant arrivedAt,
        Long arrivedById,
        boolean skipArrivalTime,
        int version) {}
