package com.cambae.backend.m03.internal.bed.service;

import com.cambae.backend.m03.api.*;
import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.*;
import com.cambae.backend.m03.internal.bed.domain.BedCapacitySummary;
import com.cambae.backend.m03.internal.bed.persistence.BedStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T027/Q-246: số giường dùng được không bao gồm Tạm ngừng, Bảo trì, Không sử dụng. */
@Service
@Transactional(readOnly = true)
public class BedCapacityQueries {
    private final BedStore beds;
    private final RoomPolicyApi policy;
    private final LocationStructureApi structure;
    private final PermissionApi permissions;
    private final CurrentUser user;

    public BedCapacityQueries(
            BedStore beds,
            RoomPolicyApi policy,
            LocationStructureApi structure,
            PermissionApi permissions,
            CurrentUser user) {
        this.beds = beds;
        this.policy = policy;
        this.structure = structure;
        this.permissions = permissions;
        this.user = user;
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.VIEW)
    public BedCapacitySummary summary(Long floorId, CareLevelCode level, ResidentGender gender, ResidencyType type) {
        Map<BedStatus, Integer> counts = new EnumMap<>(BedStatus.class);
        int usable = 0;
        int matching = 0;
        for (var bed : beds.list(null, floorId, null, null)) {
            if (!permissions
                    .hasPermission(
                            user,
                            new PermissionKey(Function.BED_ALLOCATION, Operation.VIEW),
                            new Target(
                                    "M03.BED",
                                    Long.toString(bed.id()),
                                    null,
                                    bed.location().floorId()))
                    .allowed()) continue;
            counts.merge(bed.state(), 1, Integer::sum);
            if (bed.state() != BedStatus.SUSPENDED
                    && bed.state() != BedStatus.UNDER_MAINTENANCE
                    && bed.state() != BedStatus.RETIRED
                    && structure.active(new LocationRef("ROOM", bed.roomId()))) usable++;
            if (bed.state() != BedStatus.EMPTY || !structure.active(new LocationRef("ROOM", bed.roomId()))) continue;
            var room = policy.allocationProperties(bed.id());
            if (room.isolated() || room.awaitingResidentTransfer() || room.futureAllocation()) continue;
            if (level != null && !room.careLevels().contains(level)) continue;
            if (gender != null
                    && room.genderPolicy() != GenderPolicy.UNRESTRICTED
                    && !room.genderPolicy().name().equals(gender.name())) continue;
            if (type != null
                    && !(type == ResidencyType.LONG_TERM && room.longTerm()
                            || type == ResidencyType.SHORT_TERM && room.shortTerm())) continue;
            matching++;
        }
        return new BedCapacitySummary(Map.copyOf(counts), usable, matching);
    }
}
