package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import java.time.Instant;
import java.util.List;

/** Ảnh đọc hiện hành đã áp đính chính, gồm định danh actor để tầng trình bày lấy nhãn qua API. */
public record AllocationDetails(
        long id,
        long residentId,
        LocationView location,
        AllocationStatus status,
        Instant startAt,
        Instant plannedStartAt,
        Instant endAt,
        Instant expectedEnd,
        String startReason,
        String endReason,
        String cancellationReason,
        Long openedById,
        Long closedById,
        String basisType,
        Long basisId,
        Long previousAllocationId,
        CareLevelCode checkedCareLevel,
        CareLevelSource careLevelSource,
        Long prescribingDoctorId,
        boolean backfilled,
        Instant actualRecordedAt,
        Instant plannedAt,
        boolean executedLate,
        boolean waitingForBedReady,
        List<String> flags,
        Long confirmingDoctorId,
        Instant doctorConfirmedAt,
        String doctorConfirmationReason,
        boolean corrected) {}
