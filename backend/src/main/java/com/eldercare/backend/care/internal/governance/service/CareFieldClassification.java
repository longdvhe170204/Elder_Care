package com.eldercare.backend.care.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;

public record CareFieldClassification(String entityType, Map<String, InfoGroup> fieldGroups)
        implements FieldClassificationProvider {
    public CareFieldClassification {
        fieldGroups = Map.copyOf(fieldGroups);
    }
}
