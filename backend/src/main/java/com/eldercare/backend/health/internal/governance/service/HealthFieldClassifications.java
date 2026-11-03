package com.eldercare.backend.health.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phân loại trường nhạy cảm sức khỏe cho module M06 (FR-080; data-model mục 13).
 */
@Configuration
public class HealthFieldClassifications {

    public static final String VITAL_SIGN_RECORD = "M06.VITAL_SIGN_RECORD";
    public static final String EXAMINATION_RECORD = "M06.EXAMINATION_RECORD";

    @Bean
    public FieldClassificationProvider vitalSignFieldClassification() {
        return vitalSignRecord();
    }

    @Bean
    public FieldClassificationProvider examinationFieldClassification() {
        return examinationRecord();
    }

    public static FieldClassificationProvider vitalSignRecord() {
        return new FieldClassification(
                VITAL_SIGN_RECORD,
                Map.of(
                        "value", InfoGroup.HEALTH,
                        "values", InfoGroup.HEALTH,
                        "giaTri", InfoGroup.HEALTH,
                        "level", InfoGroup.HEALTH,
                        "muc", InfoGroup.HEALTH));
    }

    public static FieldClassificationProvider examinationRecord() {
        return new FieldClassification(
                EXAMINATION_RECORD,
                Map.of(
                        "content", InfoGroup.HEALTH,
                        "noiDung", InfoGroup.HEALTH,
                        "clinicalAssessment", InfoGroup.HEALTH,
                        "nhanDinh", InfoGroup.HEALTH));
    }

    record FieldClassification(String entityType, Map<String, InfoGroup> fieldGroups)
            implements FieldClassificationProvider {}
}
