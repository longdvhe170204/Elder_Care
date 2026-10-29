package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Hợp đồng 003 ResidentLocationApi (T003); bản cài thuộc user story tương ứng. */
public interface ResidentLocationApi {
    Optional<LocationView> current(long residentId);

    Map<Long, LocationView> current(Collection<Long> residentIds);

    Optional<LocationView> at(long residentId, Instant at);

    Optional<Long> floorOn(long residentId, LocalDate date);

    boolean hasNoBed(long residentId);

    Optional<BedAllocationView> currentOrFutureAllocation(long residentId);
}
