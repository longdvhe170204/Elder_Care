package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.internal.structure.domain.Floor;
import com.fasterxml.jackson.annotation.JsonInclude;

/** T017: phản hồi chỉ gồm trường hợp đồng, không serialize trực tiếp entity JPA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FloorResponse(long id, long areaId, String code, String name, String buildingName, String status) {
    public static FloorResponse of(Floor e) {
        return new FloorResponse(
                e.getId(),
                e.areaId(),
                e.code(),
                e.getName(),
                e.buildingName(),
                e.getStatus().name());
    }
}
