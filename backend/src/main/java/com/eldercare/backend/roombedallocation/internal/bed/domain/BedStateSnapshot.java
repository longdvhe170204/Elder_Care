package com.eldercare.backend.roombedallocation.internal.bed.domain;
/** T008: dữ liệu của máy trạng thái giường, research G-04. */
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;

public record BedStateSnapshot(
        long id,
        long roomId,
        long floorId,
        BedStatus state,
        HoldReason holdReason,
        boolean roomActive,
        Long holdResidentId) {}
