package com.cambae.backend.m02.internal.transition.config;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phân loại trường nhật ký của M02 theo nhóm thông tin ({@code PhanLoaiTruongProvider}; data-model mục 13, FR-028,
 * FR-069, BR-M02-21): giá, khoản cọc, dòng ghi đè chính sách vắng của hợp đồng, phụ lục và yêu cầu thay đổi là tài chính;
 * nguyên nhân qua đời là sức khỏe. Trường không khai báo là công khai nội bộ. Tên trường theo thuộc tính JSON của
 * api-004; tên loại đối tượng của phụ lục ({@code M02.ADDENDUM}) do plan chưa đặt nên là tên tạm của T011.
 */
@Configuration
public class M02FieldClassificationConfig {

    @Bean
    FieldClassificationProvider contractClassification() {
        return classification(
                "M02.CONTRACT",
                Map.of(
                        "depositAmount", InfoGroup.FINANCE,
                        "noDepositRequired", InfoGroup.FINANCE,
                        "absencePolicyOverrides", InfoGroup.FINANCE,
                        "services", InfoGroup.FINANCE,
                        "recordedUnitPrices", InfoGroup.FINANCE));
    }

    @Bean
    FieldClassificationProvider addendumClassification() {
        return classification(
                "M02.ADDENDUM", Map.of("contentBefore", InfoGroup.FINANCE, "contentAfter", InfoGroup.FINANCE));
    }

    @Bean
    FieldClassificationProvider residencyChangeClassification() {
        return classification(
                "M02.RESIDENCY_CHANGE",
                Map.of(
                        "previousValue", InfoGroup.FINANCE,
                        "newValue", InfoGroup.FINANCE,
                        "costImpact", InfoGroup.FINANCE));
    }

    @Bean
    FieldClassificationProvider deathRecordClassification() {
        return classification("M02.DEATH_RECORD", Map.of("cause", InfoGroup.HEALTH));
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
