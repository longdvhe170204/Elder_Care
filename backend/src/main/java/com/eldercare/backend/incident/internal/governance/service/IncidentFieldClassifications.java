package com.eldercare.backend.incident.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phân loại trường nhạy cảm cho module M05 (FR-080; data-model mục 13).
 */
@Configuration
public class IncidentFieldClassifications {

    public static final String INCIDENT = "M05.INCIDENT";
    public static final String INCIDENT_PROGRESSION = "M05.INCIDENT_PROGRESSION";
    public static final String ALERT = "M05.ALERT";
    public static final String CONTACT_LIST = "M05.CONTACT_LIST";

    @Bean
    public FieldClassificationProvider incidentFieldClassification() {
        return incident();
    }

    @Bean
    public FieldClassificationProvider progressionFieldClassification() {
        return incidentProgression();
    }

    @Bean
    public FieldClassificationProvider alertFieldClassification() {
        return alert();
    }

    @Bean
    public FieldClassificationProvider contactListFieldClassification() {
        return contactList();
    }

    public static FieldClassificationProvider incident() {
        return new FieldClassification(
                INCIDENT,
                Map.of(
                        "description", InfoGroup.HEALTH,
                        "moTa", InfoGroup.HEALTH,
                        "emergencyCard", InfoGroup.HEALTH,
                        "theKhanCap", InfoGroup.HEALTH,
                        "wishes", InfoGroup.HEALTH,
                        "nguyenVong", InfoGroup.HEALTH,
                        "clinicalAssessment", InfoGroup.HEALTH,
                        "nhanDinh", InfoGroup.HEALTH));
    }

    public static FieldClassificationProvider incidentProgression() {
        return new FieldClassification(
                INCIDENT_PROGRESSION,
                Map.of(
                        "progression", InfoGroup.HEALTH,
                        "dienBien", InfoGroup.HEALTH,
                        "content", InfoGroup.HEALTH,
                        "noiDung", InfoGroup.HEALTH));
    }

    public static FieldClassificationProvider alert() {
        return new FieldClassification(
                ALERT,
                Map.of(
                        "level", InfoGroup.HEALTH,
                        "muc", InfoGroup.HEALTH));
    }

    public static FieldClassificationProvider contactList() {
        return new FieldClassification(
                CONTACT_LIST,
                Map.of(
                        "phoneNumber", InfoGroup.CONTACT,
                        "soDienThoai", InfoGroup.CONTACT));
    }

    record FieldClassification(String entityType, Map<String, InfoGroup> fieldGroups)
            implements FieldClassificationProvider {}
}
