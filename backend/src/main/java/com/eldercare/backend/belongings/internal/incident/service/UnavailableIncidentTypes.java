package com.eldercare.backend.belongings.internal.incident.service;

import com.eldercare.backend.common.error.BusinessException;
import org.springframework.context.annotation.Fallback;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Bản mặc định của {@link IncidentTypeLookup} cho tới khi 007 cho module khác tra mã loại sự cố (plan 013 điểm 14, dòng
 * team-plan nhờ D): 503 {@code SOURCE_UNAVAILABLE}; test khai bản giả {@code @Primary}.
 */
@Component
@Fallback
class UnavailableIncidentTypes implements IncidentTypeLookup {

    @Override
    public long idOf(String typeCode) {
        throw new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SOURCE_UNAVAILABLE",
                "Chưa có cách tra loại sự cố " + typeCode + " của feature 007");
    }
}
