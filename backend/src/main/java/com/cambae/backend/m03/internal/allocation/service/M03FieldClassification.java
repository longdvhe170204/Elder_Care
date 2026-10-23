package com.cambae.backend.m03.internal.allocation.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;

/** T010: tên trường nhạy cảm ở before/after; lý do thường vẫn công khai nội bộ theo G-15. */
public record M03FieldClassification(String entityType, Map<String, InfoGroup> fieldGroups)
        implements FieldClassificationProvider {
    public M03FieldClassification {
        fieldGroups = Map.copyOf(fieldGroups);
    }
}
