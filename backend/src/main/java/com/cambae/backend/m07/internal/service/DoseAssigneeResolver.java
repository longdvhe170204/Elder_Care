package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.api.spi.ShiftNursePort;
import com.cambae.backend.m07.internal.domain.DoseAuthorityBasis;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the nurse responsible for a medication dose at read and write time (research D-09, FR-026, FR-026a).
 * Dose responsibility is dynamically computed, not stored as a static column on the dose.
 */
@Component
public class DoseAssigneeResolver {

    private final ShiftNursePort shiftNursePort;

    public DoseAssigneeResolver(ShiftNursePort shiftNursePort) {
        this.shiftNursePort = shiftNursePort;
    }

    public record DoseAssignee(
            Long assignedNurseId,
            boolean floorShared,
            DoseAuthorityBasis authorityBasis,
            List<Long> eligibleNurseIds) {}

    public DoseAssignee resolve(Long residentId, Long floorAreaId, Instant scheduledTime, Instant now) {
        Optional<Long> assigned =
                residentId != null ? shiftNursePort.assignedNurse(residentId, scheduledTime) : Optional.empty();

        if (assigned.isPresent()) {
            return new DoseAssignee(assigned.get(), false, DoseAuthorityBasis.ASSIGNED, List.of(assigned.get()));
        }

        List<Long> nursesOnShift = floorAreaId != null ? shiftNursePort.nursesOnShift(floorAreaId, now) : List.of();

        return new DoseAssignee(null, true, DoseAuthorityBasis.FLOOR_SHARED, nursesOnShift);
    }
}
