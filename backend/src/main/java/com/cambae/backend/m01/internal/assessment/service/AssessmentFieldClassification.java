package com.cambae.backend.m01.internal.assessment.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường nhật ký của đánh giá (001 T011; data-model mục 10; FR-049): mọi trường là nhóm sức khỏe, riêng mức
 * chăm sóc thuộc nhóm mức chăm sóc. Thêm trường vào {@code snapshot} thì thêm vào đây
 * ({@code AuditLogHealthMaskingIT} kiểm).
 */
@Component
class AssessmentFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return AssessmentService.RECORD_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.ofEntries(
                Map.entry("type", InfoGroup.HEALTH),
                Map.entry("basis", InfoGroup.HEALTH),
                Map.entry("comment", InfoGroup.HEALTH),
                Map.entry("careNote", InfoGroup.HEALTH),
                Map.entry("careLevel", InfoGroup.CARE_LEVEL),
                Map.entry("careNeedIds", InfoGroup.HEALTH),
                Map.entry("proposedResidenceType", InfoGroup.HEALTH),
                Map.entry("confirmedAt", InfoGroup.HEALTH));
    }
}
