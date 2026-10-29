package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import java.util.Set;

/** Bảng chuyển G-04/data-model mục 3; điều kiện phân bổ/tài sản bên ngoài do lệnh gọi kiểm trong cùng transaction. */
public final class BedTransitions {
    private BedTransitions() {}

    public static BedStatus next(
            BedStatus from, HoldReason hold, BedEvent event, BedStatus maintenanceSource, boolean startReserved) {
        if (event == BedEvent.CREATE_BED) {
            if (from == null) return BedStatus.EMPTY;
            throw invalid();
        }
        if (from == null) throw invalid();
        boolean absent = from == BedStatus.HELD && hold == HoldReason.ABSENT_RESIDENT;
        boolean waiting = from == BedStatus.HELD && hold == HoldReason.WAITING_RECORD;
        return switch (event) {
            case ALLOCATION_STARTED -> {
                if (from == BedStatus.EMPTY || waiting) yield BedStatus.OCCUPIED;
                throw invalid();
            }
            case HOLD_WAITING_RECORD -> {
                require(from == BedStatus.EMPTY);
                yield BedStatus.HELD;
            }
            case HOLD_EXPIRED, HOLD_CANCELLED -> {
                require(waiting);
                yield BedStatus.EMPTY;
            }
            case ABSENT_KEEP_BED -> {
                require(from == BedStatus.OCCUPIED);
                yield BedStatus.HELD;
            }
            case RETURNED -> {
                require(absent);
                yield BedStatus.OCCUPIED;
            }
            case RELEASE_WHEN_ABSENT -> {
                require(absent);
                yield BedStatus.AWAITING_READY;
            }
            case RESIDENCY_ENDED, DEATH, TRANSFERRED_OUT -> {
                require(from == BedStatus.OCCUPIED || absent);
                yield BedStatus.AWAITING_READY;
            }
            case ABSENT_RELEASE_BED -> {
                require(from == BedStatus.OCCUPIED);
                yield BedStatus.AWAITING_READY;
            }
            case CONFIRM_READY -> {
                require(from == BedStatus.AWAITING_READY);
                yield startReserved ? BedStatus.OCCUPIED : BedStatus.EMPTY;
            }
            case ASSET_MAINTENANCE -> {
                require(Set.of(BedStatus.EMPTY, BedStatus.AWAITING_READY, BedStatus.SUSPENDED)
                        .contains(from));
                yield BedStatus.UNDER_MAINTENANCE;
            }
            case ASSET_RETIRED -> {
                require(Set.of(
                                BedStatus.EMPTY,
                                BedStatus.AWAITING_READY,
                                BedStatus.SUSPENDED,
                                BedStatus.UNDER_MAINTENANCE)
                        .contains(from));
                yield BedStatus.RETIRED;
            }
            case ASSET_READY -> {
                require(from == BedStatus.UNDER_MAINTENANCE && maintenanceSource != null);
                require(Set.of(BedStatus.EMPTY, BedStatus.AWAITING_READY, BedStatus.SUSPENDED)
                        .contains(maintenanceSource));
                yield maintenanceSource == BedStatus.SUSPENDED ? BedStatus.SUSPENDED : BedStatus.EMPTY;
            }
            case SUSPEND -> {
                require(from == BedStatus.EMPTY);
                yield BedStatus.SUSPENDED;
            }
            case REUSE -> {
                require(from == BedStatus.SUSPENDED);
                yield BedStatus.AWAITING_READY;
            }
            case CREATE_BED -> throw invalid();
        };
    }

    private static void require(boolean valid) {
        if (!valid) throw invalid();
    }

    private static IllegalStateException invalid() {
        return new IllegalStateException("Chuyển trạng thái giường không hợp lệ");
    }
}
