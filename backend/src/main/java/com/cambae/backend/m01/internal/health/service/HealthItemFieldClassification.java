package com.cambae.backend.m01.internal.health.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường nhật ký của mục sức khỏe (001 T011; data-model mục 10; FR-049, BR-M15-11): mọi trường là nhóm sức
 * khỏe, nên Hành chính xem lịch sử thấy "đã thay đổi" mà không thấy giá trị. Trường chưa liệt kê sẽ mặc định công khai
 * nội bộ, vì vậy thêm trường vào {@code snapshot} thì thêm vào đây ({@code AuditLogHealthMaskingIT} kiểm).
 */
@Component
class HealthItemFieldClassification implements FieldClassificationProvider {

    @Override
    public String entityType() {
        return HealthItemService.ENTITY_TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of(
                "type", InfoGroup.HEALTH,
                "allergenId", InfoGroup.HEALTH,
                "freeText", InfoGroup.HEALTH,
                "severityId", InfoGroup.HEALTH,
                "sourceId", InfoGroup.HEALTH,
                "status", InfoGroup.HEALTH);
    }
}
