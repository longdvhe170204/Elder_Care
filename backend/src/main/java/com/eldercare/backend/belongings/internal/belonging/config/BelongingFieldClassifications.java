package com.eldercare.backend.belongings.internal.belonging.config;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phân loại trường nhật ký của đồ gửi và bản ghi bàn giao ({@code PhanLoaiTruongProvider}; 013 T007, FR-031a): giấy tờ
 * người giao là dữ liệu cá nhân, số tiền là tài chính.
 */
@Configuration
class BelongingFieldClassifications {

    @Bean
    FieldClassificationProvider belongingFieldClassification() {
        return provider("M12.BELONGING", Map.of("cashAmount", InfoGroup.FINANCE));
    }

    @Bean
    FieldClassificationProvider belongingHandoverFieldClassification() {
        return provider(
                "M12.BELONGING_HANDOVER",
                Map.of("giverIdDocument", InfoGroup.IDENTITY, "cashAmount", InfoGroup.FINANCE));
    }

    private static FieldClassificationProvider provider(String entityType, Map<String, InfoGroup> groups) {
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
