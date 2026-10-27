package com.cambae.backend.m04.internal.governance.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;

public record CareFieldClassification(String entityType, Map<String, InfoGroup> fieldGroups)
        implements FieldClassificationProvider {
    public CareFieldClassification {
        fieldGroups = Map.copyOf(fieldGroups);
    }
}
