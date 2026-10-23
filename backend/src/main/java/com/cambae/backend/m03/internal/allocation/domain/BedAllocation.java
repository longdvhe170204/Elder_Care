package com.cambae.backend.m03.internal.allocation.domain;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import jakarta.persistence.*;
import java.time.Instant;

/** T024: dữ liệu phân bổ nhóm 2; không cung cấp setter đổi trạng thái. */
@Entity
@Table(name = "bed_allocation")
@DataGroup(DataGroup.Group.BUSINESS)
public class BedAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long residentId;
    private long bedId;

    @Enumerated(EnumType.STRING)
    private AllocationStatus status;

    private Instant startAt;
    private Instant plannedStartAt;
    private Instant endAt;
    private Instant expectedEnd;
    private String startReason;
    private String endReason;
    private String cancellationReason;
    private Long transferReasonId;
    private Long openedById;
    private Long closedById;
    private String basisType;
    private Long basisId;
    private Long previousAllocationId;

    @Enumerated(EnumType.STRING)
    private CareLevelCode checkedCareLevel;

    @Enumerated(EnumType.STRING)
    private CareLevelSource careLevelSource;

    private Long prescribingDoctorId;
    private boolean backfilled;
    private Instant actualRecordedAt;
    private Instant plannedAt;
    private boolean executedLate;
    private boolean waitingForBedReady;
    private boolean roomUnsuitable;
    private boolean awaitingRequest;
    private boolean awaitingSamePrice;
    private boolean awaitingDoctorConfirmation;
    private Long confirmingDoctorId;
    private Instant doctorConfirmedAt;
    private String doctorConfirmationReason;
    private Instant notifiedPlannedStartAt;

    protected BedAllocation() {}
}
