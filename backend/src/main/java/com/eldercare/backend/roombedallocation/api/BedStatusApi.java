package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.roombedallocation.api.dto.BedFlagView;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.dto.BedView;
import com.eldercare.backend.roombedallocation.api.dto.ResidentGender;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Hợp đồng 003 BedStatusApi (T003); bản cài thuộc user story tương ứng. */
public interface BedStatusApi {
    Map<BedStatus, Integer> countByState(ScopeFilter scope, Instant at);

    int countAvailable(
            ScopeFilter scope, CareLevelCode level, ResidentGender gender, ResidencyType residencyType, Instant at);

    List<BedFlagView> flaggedBeds(ScopeFilter scope);

    Optional<Instant> emptyPeriodStarted(long bedId);

    /** 016 `BED.EMPTY_AWAITING_TRANSFER` (019 FR-015, Q-235): số giường Trống mang dấu "chờ chuyển người" tại {@code at}. */
    int countEmptyAwaitingTransfer(ScopeFilter scope, Instant at);

    List<BedView> overdueAwaitingReady(long floorId, Instant at);
}
