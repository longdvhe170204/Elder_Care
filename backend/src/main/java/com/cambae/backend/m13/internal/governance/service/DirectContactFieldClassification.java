package com.cambae.backend.m13.internal.governance.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Phân loại trường của liên hệ trực tiếp khi xem nhật ký (FR-021; data-model mục 9): số điện thoại là liên hệ. */
@Component
class DirectContactFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return "M13.DIRECT_CONTACT";
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of("phoneNumber", InfoGroup.CONTACT);
    }
}
