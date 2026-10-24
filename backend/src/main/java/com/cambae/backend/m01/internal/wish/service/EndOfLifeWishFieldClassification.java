package com.cambae.backend.m01.internal.wish.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường nhật ký của phiếu nguyện vọng cuối đời (001 T069; data-model mục 10; FR-023e, FR-049): mọi trường là
 * nhóm nguyện vọng, nên Hành chính xem lịch sử thấy "đã thay đổi" mà không thấy giá trị.
 */
@Component
class EndOfLifeWishFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return EndOfLifeWishService.ENTITY_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.ofEntries(
                Map.entry("choice", InfoGroup.WISHES),
                Map.entry("firstContact", InfoGroup.WISHES),
                Map.entry("note", InfoGroup.WISHES),
                Map.entry("respondentType", InfoGroup.WISHES),
                Map.entry("respondentFamilyMemberId", InfoGroup.WISHES),
                Map.entry("relationship", InfoGroup.WISHES),
                Map.entry("assessingDoctorId", InfoGroup.WISHES),
                Map.entry("canSelfDetermine", InfoGroup.WISHES),
                Map.entry("supportMethod", InfoGroup.WISHES),
                Map.entry("reviewDate", InfoGroup.WISHES),
                Map.entry("signerType", InfoGroup.WISHES),
                Map.entry("signerFamilyMemberId", InfoGroup.WISHES),
                Map.entry("representativeSelection", InfoGroup.WISHES),
                Map.entry("courtDecisionNumber", InfoGroup.WISHES),
                Map.entry("courtDecisionDate", InfoGroup.WISHES),
                Map.entry("courtDecisionCourt", InfoGroup.WISHES),
                Map.entry("courtDecisionFileId", InfoGroup.WISHES),
                Map.entry("otherOpinionCount", InfoGroup.WISHES),
                Map.entry("signedAt", InfoGroup.WISHES),
                Map.entry("signedCopyFileId", InfoGroup.WISHES));
    }
}
