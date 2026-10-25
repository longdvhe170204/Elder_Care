package com.cambae.backend.m03.internal.structure.domain;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import java.util.Set;

/** Dữ liệu lệnh nội bộ T015, không phải schema HTTP. parentId là area của tầng hoặc floor của phòng. */
public record StructureChange(
        String code,
        String name,
        Long parentId,
        Long roomTypeId,
        String buildingName,
        Integer standardBeds,
        GenderPolicy genderPolicy,
        Set<CareLevelCode> careLevels,
        Boolean servesLongTerm,
        Boolean servesShortTerm,
        Boolean medicalSafety,
        Reason reason) {
    public static StructureChange area(String code, String name) {
        return new StructureChange(code, name, null, null, null, null, null, null, null, null, null, null);
    }

    public static StructureChange floor(long areaId, String code, String name, String buildingName) {
        return new StructureChange(code, name, areaId, null, buildingName, null, null, null, null, null, null, null);
    }

    public static StructureChange roomType(
            String name, int beds, Set<CareLevelCode> levels, boolean longTerm, boolean shortTerm) {
        return new StructureChange(null, name, null, null, null, beds, null, levels, longTerm, shortTerm, null, null);
    }

    public static StructureChange room(
            long floorId, long typeId, String code, String name, GenderPolicy gender, Set<CareLevelCode> levels) {
        return new StructureChange(code, name, floorId, typeId, null, null, gender, levels, null, null, null, null);
    }

    public static StructureChange transferReason(String code, String name, boolean medical) {
        return new StructureChange(code, name, null, null, null, null, null, null, null, null, medical, null);
    }

    public StructureChange withReason(Reason reason) {
        return new StructureChange(
                code,
                name,
                parentId,
                roomTypeId,
                buildingName,
                standardBeds,
                genderPolicy,
                careLevels,
                servesLongTerm,
                servesShortTerm,
                medicalSafety,
                reason);
    }
}
