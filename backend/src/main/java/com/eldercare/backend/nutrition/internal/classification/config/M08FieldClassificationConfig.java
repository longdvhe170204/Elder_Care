package com.eldercare.backend.nutrition.internal.classification.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phân loại trường nhật ký của M08 theo nhóm thông tin ({@code FieldClassificationProvider}; FR-064, research M-15):
 * chế độ ăn, hạn chế, dị ứng, nội dung sự cố khi ăn là sức khỏe; trường khác không khai báo là công khai nội bộ. Tên
 * trường theo thuộc tính JSON của api-011 đã dịch (glossary mục 17); tên loại đối tượng theo mục 17.6.
 */
@Configuration
public class M08FieldClassificationConfig {

    @Bean
    public FieldClassificationProvider dietClassification() {
        return classification("M08.DIET", Map.of("restrictions", InfoGroup.HEALTH));
    }

    @Bean
    public FieldClassificationProvider dietAssignmentClassification() {
        return classification(
                "M08.DIET_ASSIGNMENT",
                Map.of(
                        "dietId", InfoGroup.HEALTH,
                        "texture", InfoGroup.HEALTH,
                        "personalRestrictions", InfoGroup.HEALTH,
                        "personalInstruction", InfoGroup.HEALTH,
                        "professionalReason", InfoGroup.HEALTH));
    }

    @Bean
    public FieldClassificationProvider familyFoodClassification() {
        return classification(
                "M08.FAMILY_FOOD", Map.of("ingredients", InfoGroup.HEALTH, "systemReasons", InfoGroup.HEALTH));
    }

    @Bean
    public FieldClassificationProvider servingConfirmationClassification() {
        return classification(
                "M08.SERVING_CONFIRMATION", Map.of("hasEaten", InfoGroup.HEALTH, "incident", InfoGroup.HEALTH));
    }

    private static FieldClassificationProvider classification(String entityType, Map<String, InfoGroup> groups) {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return entityType;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return groups;
            }
        };
    }
}
