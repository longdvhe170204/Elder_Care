package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường nhật ký của phiếu nguyện vọng khi điều trị (001 T079; data-model mục 10; FR-023m): mọi trường là
 * nhóm sức khỏe (không dùng nhóm nguyện vọng vì nhóm đó cho Kế toán "chỉ có hay không", research H-18).
 */
@Component
class TreatmentWishFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return TreatmentWishService.ENTITY_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.ofEntries(
                Map.entry("preferredHospitals", InfoGroup.HEALTH),
                Map.entry("insuranceRegistrationPlace", InfoGroup.HEALTH),
                Map.entry("insuranceCardNumber", InfoGroup.HEALTH),
                Map.entry("insuranceCardExpiry", InfoGroup.HEALTH),
                Map.entry("askFamilyFirst", InfoGroup.HEALTH),
                Map.entry("notifyFamilyMemberIds", InfoGroup.HEALTH),
                Map.entry("accompaniedBy", InfoGroup.HEALTH),
                Map.entry("note", InfoGroup.HEALTH),
                Map.entry("respondentType", InfoGroup.HEALTH),
                Map.entry("respondentFamilyMemberId", InfoGroup.HEALTH),
                Map.entry("otherOpinionCount", InfoGroup.HEALTH),
                Map.entry("signedCopyFileId", InfoGroup.HEALTH));
    }
}
