package com.eldercare.backend.admission.internal.transition.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phân loại trường nhật ký của 004 (T011): giá, cọc là tài chính; nguyên nhân qua đời là sức khỏe; ý kiến vào ở và các trường khác là
 * công khai nội bộ. Loại đối tượng thiếu provider thì chỉ Quản lý viện xem được lịch sử, nên loại không có trường nhạy cảm vẫn khai.
 */
@Configuration(proxyBeanMethods = false)
class M02FieldClassifications {

    private static final Map<String, InfoGroup> CONTRACT_FIELDS = Map.of(
            "services", InfoGroup.FINANCE,
            "depositAmount", InfoGroup.FINANCE,
            "noDepositRequired", InfoGroup.FINANCE,
            "recordedUnitPrices", InfoGroup.FINANCE);

    private record Classification(String entityType, Map<String, InfoGroup> fieldGroups)
            implements FieldClassificationProvider {}

    @Bean
    FieldClassificationProvider contractFieldClassification() {
        return new Classification("M02.CONTRACT", CONTRACT_FIELDS);
    }

    @Bean
    FieldClassificationProvider addendumFieldClassification() {
        return new Classification("M02.ADDENDUM", CONTRACT_FIELDS);
    }

    @Bean
    FieldClassificationProvider priceVersionFieldClassification() {
        return new Classification("M02.PRICE_VERSION", Map.of("unitPrice", InfoGroup.FINANCE));
    }

    @Bean
    FieldClassificationProvider deathRecordFieldClassification() {
        return new Classification("M02.DEATH_RECORD", Map.of("cause", InfoGroup.HEALTH));
    }

    @Bean
    FieldClassificationProvider admissionApplicationFieldClassification() {
        return new Classification("M02.ADMISSION_APPLICATION", Map.of());
    }

    @Bean
    FieldClassificationProvider waitingListFieldClassification() {
        return new Classification("M02.WAITING_LIST_ENTRY", Map.of());
    }

    @Bean
    FieldClassificationProvider absenceFieldClassification() {
        return new Classification("M02.ABSENCE", Map.of());
    }

    @Bean
    FieldClassificationProvider residencyEndingFieldClassification() {
        return new Classification("M02.RESIDENCY_ENDING", Map.of());
    }

    @Bean
    FieldClassificationProvider serviceFieldClassification() {
        return new Classification("M02.SERVICE", Map.of());
    }
}
