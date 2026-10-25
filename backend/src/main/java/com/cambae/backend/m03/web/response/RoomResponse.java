package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.structure.domain.Room;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

/** T017: phản hồi chỉ gồm trường hợp đồng, không serialize trực tiếp entity JPA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomResponse(
        long id,
        long floorId,
        String code,
        String name,
        long roomTypeId,
        Set<CareLevelCode> allowedCareLevels,
        GenderPolicy genderPolicy,
        boolean isolated,
        Boolean isolationBlocked,
        String status) {
    public static RoomResponse of(Room e, Boolean blocked) {
        return new RoomResponse(
                e.getId(),
                e.floorId(),
                e.code(),
                e.getName(),
                e.roomTypeId(),
                e.careLevels(),
                e.genderPolicy(),
                e.isolated(),
                blocked,
                e.getStatus().name());
    }
}
