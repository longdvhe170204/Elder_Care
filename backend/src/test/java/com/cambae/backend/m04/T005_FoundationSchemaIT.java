package com.cambae.backend.m04;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class T005_FoundationSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_038_activeNameIsUniqueAndInactiveNameCanBeReused() {
        insert("TEST_SCHEMA_DUPLICATE", "OTHER");
        assertThatThrownBy(() -> insert("TEST_SCHEMA_DUPLICATE", "OTHER"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("UPDATE care_task_type SET status='INACTIVE' WHERE name='TEST_SCHEMA_DUPLICATE'");
        insert("TEST_SCHEMA_DUPLICATE", "OTHER");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM care_task_type WHERE name='TEST_SCHEMA_DUPLICATE'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void FR_038_allEighteenGroupsAndNullableCfgWindowsAreStored() {
        List<String> groups = List.of(
                "PERSONAL_HYGIENE",
                "BATHING",
                "DRESSING",
                "EATING_ASSISTANCE",
                "DRINKING_ASSISTANCE",
                "TOILETING_ASSISTANCE",
                "MOVEMENT",
                "REHABILITATION",
                "VITAL_SIGN_MEASUREMENT",
                "ACTIVITY",
                "SLEEP",
                "MOOD",
                "BEHAVIOR",
                "DIAPER_ITEM_CHANGE",
                "SHIFT_START_CHECK",
                "SUPERVISION",
                "MEDICATION",
                "OTHER");
        for (String group : groups) insert("TEST_SCHEMA_" + group, group);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM care_task_type WHERE name LIKE 'TEST_SCHEMA_%' AND window_before_minutes IS NULL AND window_after_minutes IS NULL",
                        Integer.class))
                .isEqualTo(18);
        assertThatThrownBy(() -> insert("TEST_SCHEMA_BAD", "INVALID"))
                .isInstanceOf(DataIntegrityViolationException.class);
        long id = jdbc.queryForObject("SELECT id FROM care_task_type WHERE name='TEST_SCHEMA_OTHER'", Long.class);
        jdbc.update("INSERT INTO care_task_type_certificate(task_type_id,certificate_code) VALUES (?,?)", id, "TEST");
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO care_task_type_certificate(task_type_id,certificate_code) VALUES (?,?)",
                        id,
                        "TEST"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO care_task_type_certificate(task_type_id,certificate_code) VALUES (?,?)",
                        Long.MAX_VALUE,
                        "TEST"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void NFR_04_technicalKeysAndResultValueCatalogArePersisted() {
        jdbc.update("INSERT INTO care_resident_lock(resident_id) VALUES (99001)");
        assertThat(jdbc.queryForObject("SELECT version FROM care_resident_lock WHERE resident_id=99001", Integer.class))
                .isZero();
        jdbc.update("INSERT INTO m04_job_milestone(job,milestone) VALUES ('TEST','2026-10-27')");
        assertThatThrownBy(
                        () -> jdbc.update("INSERT INTO m04_job_milestone(job,milestone) VALUES ('TEST','2026-10-27')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(
                "INSERT INTO care_result_value(value_type,code,name,rule_group) VALUES ('SLEEP_QUALITY','TEST','TEST_SCHEMA_SLEEP','INSOMNIA')");
        assertThat(jdbc.queryForObject("SELECT rule_group FROM care_result_value WHERE code='TEST'", String.class))
                .isEqualTo("INSOMNIA");
    }

    private void insert(String name, String group) {
        jdbc.update(
                "INSERT INTO care_task_type(name,task_group,professional,result_type,default_role,default_importance,billable) VALUES (?,?,false,'PERFORMED','CAREGIVER','NORMAL',false)",
                name,
                group);
    }
}
