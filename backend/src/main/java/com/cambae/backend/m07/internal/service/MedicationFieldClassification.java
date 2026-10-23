package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Phân loại trường của Danh mục thuốc (FR-001, FR-002, data-model mục 12).
 * Các trường tên, hàm lượng, dạng bào chế, đường dùng, đơn vị dùng, hoạt chất mang nhóm {@link InfoGroup#HEALTH}.
 */
@Component
public class MedicationFieldClassification implements FieldClassificationProvider {

    public static final String TYPE = "M07.MEDICATION";

    @Override
    public String entityType() {
        return TYPE;
    }

    @Override
    public Map<String, InfoGroup> fieldGroups() {
        return Map.of(
                "name", InfoGroup.HEALTH,
                "concentration", InfoGroup.HEALTH,
                "dosageForm", InfoGroup.HEALTH,
                "defaultRoute", InfoGroup.HEALTH,
                "usageUnit", InfoGroup.HEALTH,
                "allergenIds", InfoGroup.HEALTH);
    }
}
