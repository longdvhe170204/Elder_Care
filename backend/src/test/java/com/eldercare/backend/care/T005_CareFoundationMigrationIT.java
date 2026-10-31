package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** R-15: kiểm dữ liệu thật trước/sau gộp, có id trùng với danh mục của module khác. */
class T005_CareFoundationMigrationIT extends MySqlIntegrationTestBase {
    private static final String SUFFIX = "care005_foundation";
    private static JdbcTemplate db;

    @BeforeAll
    static void migrate() {
        var before = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX).getConfiguration())
                .target("5.2")
                .load();
        before.migrate();
        db = new JdbcTemplate(before.getConfiguration().getDataSource());
        db.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status) VALUES(2000000001,'M15.TEST_COLLISION','M15','Giữ dòng module khác','ACTIVE')");
        db.update(
                "INSERT INTO care_task_type(id,name,task_group,professional,result_type,default_role,default_importance,billable,version) VALUES(2000000001,'Loại cũ','OTHER',FALSE,'PERFORMED','CAREGIVER','NORMAL',FALSE,7)");
        db.update("INSERT INTO care_task_type_certificate VALUES(2000000001,'CERT_TEST')");
        db.update(
                "INSERT INTO care_result_value(id,value_type,code,name,rule_group,status,version) VALUES(2000000002,'SLEEP_QUALITY',?,'Giấc ngủ cũ','INSOMNIA','INACTIVE',3)",
                "X".repeat(100));
        db.update("INSERT INTO care_resident_lock VALUES(2000000003,9)");
        db.update("INSERT INTO m04_job_milestone VALUES('TEST_CATCHUP','2026-10-30')");
        Flyway.configure()
                .configuration(before.getConfiguration())
                .target("5.95")
                .load()
                .migrate();
    }

    @AfterAll
    static void cleanup() {
        dropScratchSchema(SUFFIX);
    }

    @Test
    void FR_038_catalogCopyKeepsMetadataAndDoesNotOverwriteOtherModules() {
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M04.TASK_TYPE' AND JSON_EXTRACT(attributes,'$.legacy.id')=2000000001 AND JSON_EXTRACT(attributes,'$.certificates')=JSON_ARRAY('CERT_TEST') AND version=7",
                        Integer.class))
                .isEqualTo(1);
        assertThat(db.queryForObject("SELECT name FROM catalog_item WHERE id=2000000001", String.class))
                .isEqualTo("Giữ dòng module khác");
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M04.RESULT_VALUE' AND status='INACTIVE' AND version=3 AND CHAR_LENGTH(JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.code')))=100",
                        Integer.class))
                .isEqualTo(1);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND JSON_EXTRACT(attributes,'$.fixed')=TRUE AND name='Người cao tuổi từ chối'",
                        Integer.class))
                .isEqualTo(1);
    }

    @Test
    void NFR_04_locksAndMarkersAreCopiedToSharedTables() {
        assertThat(db.queryForObject(
                        "SELECT JSON_EXTRACT(data,'$.version') FROM resident_module_state WHERE module='M04' AND resident_id=2000000003",
                        Integer.class))
                .isEqualTo(9);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M04.DAILY_WORK' AND entity_type='M04.DAILY_WORK' AND marker=SHA2('TEST_CATCHUP:2026-10-30',256)",
                        Integer.class))
                .isEqualTo(1);
    }

    @Test
    void DBR_23_legacyTablesAreRemovedAfterTheirDataIsCopied() {
        for (String table : List.of(
                "care_resident_lock",
                "m04_job_milestone",
                "care_task_type",
                "care_task_type_certificate",
                "care_result_value",
                "care_non_performance_reason_type")) {
            assertThat(db.queryForObject(
                            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                            Integer.class,
                            table))
                    .as(table)
                    .isZero();
        }
    }
}
