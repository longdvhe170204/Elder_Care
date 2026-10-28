package com.cambae.backend.m01.internal.consent.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường nhật ký của bản đồng ý (001 T011 phần bản đồng ý; data-model mục 10; FR-049): quyết định của Tòa án và
 * người ký thay là nhận dạng, nên Hành chính xem lịch sử thấy "đã thay đổi" mà không thấy giá trị.
 */
@Component
class ConsentFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return ConsentService.ENTITY_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of(
                "consenterFamilyMemberId", InfoGroup.IDENTITY,
                "courtDecisionNumber", InfoGroup.IDENTITY,
                "courtDecisionDate", InfoGroup.IDENTITY,
                "courtDecisionCourt", InfoGroup.IDENTITY,
                "courtDecisionFileId", InfoGroup.IDENTITY,
                // người thân được xem dữ liệu sức khỏe: cấu hình chia sẻ sức khỏe
                "familyMemberIdsAllowedHealthView", InfoGroup.HEALTH);
    }
}
