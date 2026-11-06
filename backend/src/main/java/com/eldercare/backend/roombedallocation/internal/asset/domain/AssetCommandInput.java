package com.eldercare.backend.roombedallocation.internal.asset.domain;

import java.time.LocalDate;

/** Thân lệnh trên tài sản (api-019 {@code AssetCommand}); trường dùng theo từng lệnh. */
public record AssetCommandInput(
        String reason,
        String locationType,
        Long locationId,
        String description,
        String impactLevel,
        String result,
        LocalDate date,
        String content,
        String performedBy,
        Boolean outsourced,
        Long cost,
        Long procurementRequestId,
        Long newDepartmentId) {}
