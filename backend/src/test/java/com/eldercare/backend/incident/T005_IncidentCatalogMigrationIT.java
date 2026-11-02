package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** T005, R-15: dữ liệu khởi tạo đã chốt nằm ở bảng chung, không sinh bảng nghiệp vụ cũ. */
class T005_IncidentCatalogMigrationIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_042_fallAndMissingResidentKeepTheEmergencyDefaultsFromBusinessRules() {
        var types = jdbc.queryForList("""
                SELECT code, JSON_UNQUOTE(attributes->'$.defaultLevel') AS level,
                  attributes->'$.nonHealth' AS non_health,
                  attributes->'$.allowsNoResident' AS allows_no_resident
                FROM catalog_item WHERE catalog_type='M05.INCIDENT_TYPE' AND module='M05'
                  AND code IN ('FALL', 'MISSING_OR_NOT_RETURNED') AND status='ACTIVE'
                ORDER BY sort_order
                """);
        assertThat(types).extracting(row -> row.get("code")).containsExactly("FALL", "MISSING_OR_NOT_RETURNED");
        assertThat(types).extracting(row -> row.get("level")).containsOnly("CRITICAL");
        assertThat(types).extracting(row -> row.get("non_health")).containsOnly("false");
        assertThat(types).extracting(row -> row.get("allows_no_resident")).containsOnly("false");
    }

    @Test
    void FR_039_trendRulesRetainConfiguredLevelsAndParameterReferences() {
        var rules = jdbc.queryForList("""
                SELECT code, JSON_UNQUOTE(attributes->'$.level') AS level,
                  JSON_UNQUOTE(attributes->'$.parameterCode') AS parameter_code
                FROM catalog_item WHERE catalog_type='M05.TREND_RULE' AND module='M05'
                  AND code IN ('WEIGHT_LOSS', 'MISSED_MEASUREMENT', 'PROLONGED_INSOMNIA', 'MEDICATION_REFUSAL')
                ORDER BY sort_order
                """);
        assertThat(rules).hasSize(4);
        assertThat(rules)
                .extracting(row -> row.get("code"))
                .containsExactly("WEIGHT_LOSS", "MISSED_MEASUREMENT", "PROLONGED_INSOMNIA", "MEDICATION_REFUSAL");
        assertThat(rules).extracting(row -> row.get("level")).containsExactly("MEDIUM", "LOW", "LOW", "MEDIUM");
        assertThat(rules.get(0)).containsEntry("parameter_code", "CFG-M05-03");
        assertThat(rules.get(2)).containsEntry("parameter_code", "CFG-M05-05");
        assertThat(rules.get(3)).containsEntry("parameter_code", "CFG-M05-04");
    }

    @Test
    void BR_M05_20_initialResuscitationActionsAndSystemProgressionsUseSharedCatalog() {
        var actions = jdbc.queryForList("""
                SELECT code FROM catalog_item WHERE catalog_type='M05.PROGRESSION_TYPE'
                  AND attributes->'$.resuscitation' = CAST('true' AS JSON) ORDER BY sort_order
                """, String.class);
        assertThat(actions)
                .containsExactly(
                        "CHEST_COMPRESSION",
                        "VENTILATION_SUPPORT",
                        "DEFIBRILLATION",
                        "INTUBATION",
                        "EMERGENCY_MEDICATION");
        var groups = jdbc.queryForList("""
                SELECT code FROM catalog_item WHERE catalog_type='M05.PROGRESSION_TYPE'
                  AND JSON_UNQUOTE(attributes->'$.group')='SYSTEM' ORDER BY sort_order
                """, String.class);
        assertThat(groups).containsExactly("SOURCE_RESOLVED", "TRANSFER_REFUSED", "FOUND", "CANCELLATION_REQUESTED");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M05.PROGRESSION_TYPE'
                  AND module='M05' AND status='ACTIVE'
                """, Integer.class)).isEqualTo(12);
    }

    @Test
    void R15_technicalStateAndCatalogsDoNotCreateLegacyTables() {
        var legacyTables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()
                  AND table_name IN ('resident_vital_signs','resident_monitoring','m05_job_run','m06_job_run',
                  'received_alert_event','vital_sign_indicator','vital_sign_component','trend_rule',
                  'incident_type','progression_type')
                """, String.class);
        assertThat(legacyTables).isEmpty();
        assertThat(jdbc.queryForMap("""
                SELECT table_comment FROM information_schema.tables
                WHERE table_schema=DATABASE() AND table_name='catalog_item'
                """)).containsAllEntriesOf(Map.of("TABLE_COMMENT", "DataGroup=CATALOG"));
    }
}
