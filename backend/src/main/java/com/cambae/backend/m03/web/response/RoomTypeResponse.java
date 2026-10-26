package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.structure.domain.RoomType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

/** T017: phản hồi chỉ gồm trường hợp đồng, không serialize trực tiếp entity JPA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomTypeResponse(
        long id,
        String name,
        int standardBeds,
        Set<CareLevelCode> defaultCareLevels,
        boolean servesLongTerm,
        boolean servesShortTerm,
        String status) {
    public static RoomTypeResponse of(RoomType e) {
        return new RoomTypeResponse(
                e.getId(),
                e.getName(),
                e.standardBeds(),
                e.careLevels(),
                e.servesLongTerm(),
                e.servesShortTerm(),
                e.getStatus().name());
    }
}
