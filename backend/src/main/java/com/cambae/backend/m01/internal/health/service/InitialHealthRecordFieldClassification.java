package com.cambae.backend.m01.internal.health.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường nhật ký của hồ sơ sức khỏe ban đầu (001 T011; data-model mục 10; FR-017, FR-049): mọi trường là
 * nhóm sức khỏe. Thêm trường vào {@code snapshot} thì thêm vào đây ({@code AuditLogHealthMaskingIT} kiểm).
 */
@Component
class InitialHealthRecordFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return InitialHealthRecordService.RECORD_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.ofEntries(
                Map.entry("bloodType", InfoGroup.HEALTH),
                Map.entry("mobility", InfoGroup.HEALTH),
                Map.entry("selfCareAbility", InfoGroup.HEALTH),
                Map.entry("cognition", InfoGroup.HEALTH),
                Map.entry("initialNeedsNote", InfoGroup.HEALTH),
                Map.entry("nutritionNeeds", InfoGroup.HEALTH),
                Map.entry("currentMedications", InfoGroup.HEALTH),
                Map.entry("rehabilitationNeeds", InfoGroup.HEALTH),
                Map.entry("confirmedAt", InfoGroup.HEALTH));
    }
}
