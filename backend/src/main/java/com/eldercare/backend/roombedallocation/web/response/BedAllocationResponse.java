package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Chỉ các trường BedAllocation của hợp đồng 003 đã duyệt. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedAllocationResponse(
        long id,
        long residentId,
        LocationResponse location,
        AllocationStatus status,
        Instant startAt,
        Instant plannedStartAt,
        Instant endAt,
        Instant expectedEnd,
        String startReason,
        String endReason,
        String cancellationReason,
        String openedBy,
        String closedBy,
        AllocationBasis basis,
        Long previousAllocationId,
        CareLevelCode checkedCareLevel,
        CareLevelSource careLevelSource,
        String prescribingDoctor,
        boolean backfilled,
        Instant actualRecordedAt,
        Instant plannedAt,
        boolean executedLate,
        boolean waitingForBedReady,
        List<AllocationFlag> flags,
        DoctorConfirmation doctorConfirmation,
        boolean corrected) {
    public record AllocationBasis(String type, Long id) {}

    public record DoctorConfirmation(String fullName, Instant occurredAt, String reason) {}

    public enum AllocationFlag {
        ROOM_UNSUITABLE,
        AWAITING_DOCTOR_CONFIRMATION,
        AWAITING_REQUEST,
        AWAITING_SAME_PRICE
    }

    private static String name(AccountApi accounts, Long id) {
        return id == null ? null : accounts.fullName(id).orElse(null);
    }

    public static BedAllocationResponse of(AllocationDetails row, AccountApi accounts) {
        return new BedAllocationResponse(
                row.id(),
                row.residentId(),
                LocationResponse.of(row.location()),
                row.status(),
                row.startAt(),
                row.plannedStartAt(),
                row.endAt(),
                row.expectedEnd(),
                row.startReason(),
                row.endReason(),
                row.cancellationReason(),
                name(accounts, row.openedById()),
                name(accounts, row.closedById()),
                row.basisType() == null ? null : new AllocationBasis(row.basisType(), row.basisId()),
                row.previousAllocationId(),
                row.checkedCareLevel(),
                row.careLevelSource(),
                name(accounts, row.prescribingDoctorId()),
                row.backfilled(),
                row.actualRecordedAt(),
                row.plannedAt(),
                row.executedLate(),
                row.waitingForBedReady(),
                row.flags().stream().map(AllocationFlag::valueOf).toList(),
                row.confirmingDoctorId() == null
                        ? null
                        : new DoctorConfirmation(
                                name(accounts, row.confirmingDoctorId()),
                                row.doctorConfirmedAt(),
                                row.doctorConfirmationReason()),
                row.corrected());
    }
}
