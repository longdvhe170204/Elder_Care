package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.internal.structure.domain.Area;
import com.fasterxml.jackson.annotation.JsonInclude;

/** T017: phản hồi chỉ gồm trường hợp đồng, không serialize trực tiếp entity JPA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AreaResponse(long id, String code, String name, String status, int version) {
    public static AreaResponse of(Area e) {
        return new AreaResponse(e.getId(), e.code(), e.getName(), e.getStatus().name(), e.getVersion());
    }
}
