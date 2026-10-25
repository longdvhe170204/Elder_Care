package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.BedFlagView;
import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.api.dto.BedView;
import com.cambae.backend.m03.api.dto.ResidentGender;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.api.spi.ResidencyType;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
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

    List<BedView> overdueAwaitingReady(long floorId, Instant at);
}
