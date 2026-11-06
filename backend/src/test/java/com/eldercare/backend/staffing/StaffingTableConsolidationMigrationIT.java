package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/** T050/R-15: dữ liệu đủ 25 bảng cũ, id trùng giữa nguồn và bảng chung, chạy trên schema tạm. */
class StaffingTableConsolidationMigrationIT extends MySqlIntegrationTestBase {
    private static final String SUFFIX = "m008_95";
    private static final long ID = 7001;
    private static final List<String> DROPPED = List.of(
            "department",
            "credential_type",
            "practice_scope",
            "shift_template",
            "department_lead_history",
            "shift_lead_history",
            "shift_publication",
            "handover_item",
            "handover_assessment",
            "handover_comment",
            "handover_reply",
            "staffing_ratio_calculation",
            "staff_import_batch",
            "floor_manager_assignment",
            "floor_scope_lock",
            "m09_job_run",
            "credential_practice_scope");
    private static final List<String> KEPT = List.of(
            "staff_profile",
            "staff_department",
            "credential",
            "shift_schedule",
            "work_shift",
            "shift_staff",
            "staff_assignment",
            "shift_handover");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JdbcTemplate db;
    private static Map<String, Long> catalogCounts;

    @BeforeAll
    static void migrateFixtures() {
        var old = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX).getConfiguration())
                .target("8.8")
                .load();
        old.migrate();
        db = new JdbcTemplate(old.getConfiguration().getDataSource());
        db.update(
                "INSERT INTO staff_import_batch(id,file_name,row_count,created_by,created_at) VALUES (7001,'nhan-vien.xlsx',1,7001,'2026-01-01 01:00:00.123456')");
        db.update(
                "INSERT INTO staff_profile(id,full_name,position,source,import_batch_id) VALUES (7001,'[Nhân viên migration]','Điều dưỡng','IMPORT',7001)");
        db.update("INSERT INTO department(id,name,lead_staff_id) VALUES (7001,'[Bộ phận migration]',7001)");
        db.update(
                "INSERT INTO staff_department(id,staff_id,department_id,starts_at,actor_id,reason) VALUES (7001,7001,7001,'2026-01-01',7001,'Lập hồ sơ')");
        db.update(
                "INSERT INTO department_lead_history(id,department_id,staff_id,starts_at,actor_id,reason) VALUES (7001,7001,7001,'2026-01-01 00:00:00.123456',7001,'Giao phụ trách')");
        db.update(
                "INSERT INTO credential_type(id,code,name,credential_group,has_expiry,required_for_roles,has_practice_scopes) VALUES (7001,'MIGRATION_LICENSE','[Chứng chỉ migration]','PROFESSIONAL_LICENSE',1,JSON_ARRAY('NURSING_STAFF'),1)");
        db.update("INSERT INTO practice_scope(id,code,name) VALUES (7001,'MIGRATION_SCOPE','[Phạm vi migration]')");
        db.update(
                "INSERT INTO credential(id,staff_id,credential_type_id,issue_date,expiry_date,created_by) VALUES (7001,7001,7001,'2026-01-01','2027-01-01',7001)");
        db.update("INSERT INTO credential_practice_scope VALUES (7001,7001)");
        db.update(
                "INSERT INTO shift_template(id,name,start_time,end_time,scope_type) VALUES (7001,'[Mẫu ca migration]','06:00','14:00','FLOOR')");
        db.update(
                "INSERT INTO shift_schedule(id,month,scope_type,scope_id,created_by) VALUES (7001,'2026-01','FLOOR',7001,7001)");
        db.update(
                "INSERT INTO work_shift(id,schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,lead_staff_id) VALUES (7001,7001,7001,'2026-01-01 00:00','2026-01-01 08:00','FLOOR',7001,1,7001)");
        db.update("INSERT INTO shift_staff(id,shift_id,staff_id) VALUES (7001,7001,7001)");
        db.update(
                "INSERT INTO shift_lead_history(id,shift_id,staff_id,starts_at,basis,actor_id,reason) VALUES (7001,7001,7001,'2026-01-01 00:00:00.123456','SCHEDULED',7001,'Lập lịch')");
        db.update(
                "INSERT INTO shift_publication(id,schedule_id,published_at,content) VALUES (7001,7001,'2026-01-01',JSON_OBJECT('snapshot',7001))");
        db.update("INSERT INTO floor_scope_lock VALUES (7001)");
        db.update(
                "INSERT INTO floor_manager_assignment(id,staff_id,floor_area_id,starts_at,assigned_by,reason) VALUES (7001,7001,7001,'2026-01-01',7001,'Giao tầng')");
        // Dòng cũ hợp lệ không có actor/lý do: migration không được tự bịa hai trường này.
        db.update(
                "INSERT INTO floor_manager_assignment(id,staff_id,floor_area_id,starts_at) VALUES (7002,7001,7002,'2026-01-01')");
        db.update(
                "INSERT INTO staff_assignment(id,shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at) VALUES (7001,7001,7001,'FLOOR',7001,'NURSING_STAFF','PRIMARY','2026-01-01','2026-01-01 08:00')");
        db.update(
                "INSERT INTO shift_handover(id,shift_id,handover_staff_id,created_by,created_at) VALUES (7001,7001,7001,7001,'2026-01-01')");
        db.update(
                "INSERT INTO handover_item(id,handover_id,item_group,resident_id,description,note,note_author,created_at) VALUES (7001,7001,'FOLLOW_UP_ISSUE',7001,'Cần theo dõi','Ghi chú','[Điều dưỡng]','2026-01-01 00:00:00.123456')");
        db.update(
                "INSERT INTO handover_assessment(id,handover_id,assessment_kind,content) VALUES (7001,7001,'GENERAL','Nhận định chung')");
        db.update(
                "UPDATE shift_handover SET status='PREPARED',finalized_at='2026-01-01 07:50',finalized_by=7001 WHERE id=7001");
        db.update(
                "INSERT INTO handover_comment(id,handover_id,content,sender,created_at) VALUES (7001,7001,'Ý kiến','[Người nhận]','2026-01-01 07:51')");
        db.update(
                "INSERT INTO handover_reply(id,comment_id,content,sender,created_at) VALUES (7001,7001,'Phản hồi','[Người giao]','2026-01-01 07:52')");
        db.update(
                "UPDATE shift_handover SET status='CONFIRMED',confirmed_at='2026-01-01 08:00',confirmed_by=7001 WHERE id=7001");
        db.update(
                "INSERT INTO staffing_ratio_calculation(id,shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES (7001,7001,'2026-01-01','PUBLISHED',0,0,NULL,1,0)");
        db.update(
                "INSERT INTO m09_job_run VALUES ('M09_MINUTE','2026-01-01 00:00','2026-01-01 00:00:20','2026-01-01 00:00:21','SUCCEEDED'),('M09_MINUTE','2026-01-01 00:01','2026-01-01 00:01:20','2026-01-01 00:01:21','FAILED')");
        // Cố tình trùng id của cả bốn nguồn: không được đọc nhầm sang loại khác trong bảng chung.
        db.update(
                "INSERT INTO catalog_item(id,catalog_type,module,code,name,status,attributes) VALUES (7001,'M15.PROFESSIONAL_ACTIVITY','M15','MIGRATION_ACTIVITY','[Nghiệp vụ migration]','ACTIVE',JSON_OBJECT('practiceScopeIds',JSON_ARRAY(7001)))");
        db.update(
                "INSERT INTO record_event(id,entity_type,entity_id,module,kind,by_system,occurred_at,confirmed_at) VALUES (7001,'M15.TEST',7001,'M15','M15.TEST',0,'2026-01-01','2026-01-01')");
        db.update(
                "INSERT INTO simple_record(id,kind,module,status,created_at,created_by) VALUES (7001,'M15.TEST','M15','RECORDED','2026-01-01',7001)");
        catalogCounts = Map.of(
                "department",
                count("department"),
                "credential_type",
                count("credential_type"),
                "practice_scope",
                count("practice_scope"),
                "shift_template",
                count("shift_template"));
        Flyway.configure()
                .configuration(old.getConfiguration())
                .target("latest")
                .load()
                .migrate();
    }

    @AfterAll
    static void cleanup() {
        dropScratchSchema(SUFFIX);
    }

    @Test
    void R_15_replacesSeventeenLegacyTables_andKeepsEightPrivateTables() {
        var tables = db.queryForList(
                "SELECT LOWER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()", String.class);
        assertThat(tables).doesNotContainAnyElementsOf(DROPPED).containsAll(KEPT);
        for (String table : KEPT) assertThat(count(table)).as(table).isPositive();
    }

    @Test
    void R_15_catalogsRetainEveryRowAndLogicalIdsDespiteSharedIdCollisions() {
        var kinds = Map.of(
                "department",
                "M09.DEPARTMENT",
                "credential_type",
                "M09.CREDENTIAL_TYPE",
                "practice_scope",
                "M09.PRACTICE_SCOPE",
                "shift_template",
                "M09.SHIFT_TEMPLATE");
        for (var entry : kinds.entrySet()) {
            assertThat(db.queryForObject(
                            "SELECT COUNT(*) FROM catalog_item WHERE catalog_type=? AND JSON_UNQUOTE(attributes->'$.legacy.table')=?",
                            Long.class,
                            entry.getValue(),
                            entry.getKey()))
                    .isEqualTo(catalogCounts.get(entry.getKey()));
            var item = catalog(entry.getValue());
            assertThat(((Number) item.get("id")).longValue()).isNotEqualTo(ID);
            assertThat(((Number) ((Map<?, ?>) attributes(item).get("legacy")).get("id")).longValue())
                    .isEqualTo(ID);
        }
    }

    @Test
    void FR_005_preservesPracticeScopeIdentityForExistingProfessionalActivity() {
        column("credential", "practice_scope_ids");
        var scope = catalog("M09.PRACTICE_SCOPE");
        long physical = ((Number) scope.get("id")).longValue();
        assertThat(JSON.readValue(
                        db.queryForObject("SELECT practice_scope_ids FROM credential WHERE id=7001", String.class),
                        List.class))
                .containsExactly((int) physical);
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(attributes->'$.practiceScopeIds[0]') FROM catalog_item WHERE id=7001",
                        String.class))
                .isEqualTo("7001");
        assertThat(((Number) ((Map<?, ?>) attributes(scope).get("legacy")).get("id")).longValue())
                .isEqualTo(ID);
    }

    @Test
    void FR_024_preservesDistinctCareAndFloorManagerLogicalIds() {
        column("staff_assignment", "legacy_floor_manager_id");
        assertThat(db.queryForObject("SELECT kind FROM staff_assignment WHERE id=7001", String.class))
                .isEqualTo("CARE");
        var manager = db.queryForMap(
                "SELECT id,staff_id,assigned_by,reason FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND legacy_floor_manager_id=7001");
        assertThat(((Number) manager.get("id")).longValue()).isNotEqualTo(ID);
        assertThat(manager)
                .containsEntry("staff_id", ID)
                .containsEntry("assigned_by", ID)
                .containsEntry("reason", "Giao tầng");
        assertThat(db.queryForMap("SELECT assigned_by,reason FROM staff_assignment WHERE legacy_floor_manager_id=7002"))
                .containsEntry("assigned_by", null)
                .containsEntry("reason", null);
    }

    @Test
    void FR_046_keepsHandoverParentAndEmbeddedItemIdsAndMicroseconds() {
        column("shift_handover", "items");
        var items = JSON.readValue(
                db.queryForObject("SELECT items FROM shift_handover WHERE id=7001", String.class), List.class);
        assertThat(items).hasSize(1);
        assertThat((Map) items.getFirst()).containsEntry("description", "Cần theo dõi");
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(items->'$[0].id') FROM shift_handover WHERE id=7001", String.class))
                .isEqualTo("7001");
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(items->'$[0].createdAt') FROM shift_handover WHERE id=7001", String.class))
                .contains("123456");
        assertThat(JSON.readValue(
                        db.queryForObject("SELECT assessments FROM shift_handover WHERE id=7001", String.class),
                        List.class))
                .hasSize(1);
    }

    @Test
    void FR_042_keepsReplyOnCorrectCommentAndHandoverWithSameLegacyIds() {
        var comments = db.queryForList(
                "SELECT id FROM record_event WHERE kind='M09.HANDOVER_COMMENT' AND entity_type='M09.HANDOVER' AND entity_id=7001",
                Long.class);
        assertThat(comments).hasSize(1);
        assertThat(db.queryForObject(
                        "SELECT reply_to_id FROM record_event WHERE kind='M09.HANDOVER_REPLY' AND entity_id=7001",
                        Long.class))
                .isEqualTo(comments.getFirst());
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(content->'$.legacy.id') FROM record_event WHERE kind='M09.HANDOVER_REPLY' AND entity_id=7001",
                        String.class))
                .isEqualTo("7001");
    }

    @Test
    void FR_055_copiesPublicationAndLeadHistoryAndLocksOriginals() {
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M09.SHIFT_PUBLICATION' AND entity_type='M09.SCHEDULE' AND entity_id=7001",
                        Long.class))
                .isEqualTo(1L);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type IN ('M09.DEPARTMENT','M09.SHIFT') AND entity_id=7001 AND attribute='LEAD_STAFF'",
                        Long.class))
                .isEqualTo(2L);
        assertThatThrownBy(
                        () -> db.update(
                                "UPDATE record_event SET content=JSON_OBJECT('changed',1) WHERE kind='M09.SHIFT_PUBLICATION' AND entity_id=7001"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_037_retainsZeroStaffRatioAndCorrectRetentionParent() {
        var rows = db.queryForList(
                "SELECT content FROM record_event WHERE kind='M09.STAFFING_RATIO_CALCULATION' AND entity_type='M09.SHIFT' AND entity_id=7001",
                String.class);
        assertThat(rows).hasSize(1);
        var content = JSON.readValue(rows.getFirst(), Map.class);
        assertThat(content).containsEntry("staffCount", 0).containsEntry("ratio", null);
    }

    @Test
    void FR_008_copiesSuccessfulTickWithoutPreventingFailedTickRetry() {
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M09.JOB.M09_MINUTE' AND marker='2026-01-01T00:00:00Z'",
                        Long.class))
                .isEqualTo(1L);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M09.JOB.M09_MINUTE' AND marker='2026-01-01T00:01:00Z'",
                        Long.class))
                .isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM technical_lock WHERE lock_key='M09.TANG:7001'", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void R_15_declaresDataGroupsAndProtectsConfirmedHandoverJson() {
        for (String table : KEPT)
            assertThat(db.queryForObject(
                            "SELECT TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?",
                            String.class,
                            table))
                    .as(table)
                    .contains("DataGroup=");
        column("shift_handover", "locked_at");
        assertThat(db.queryForObject("SELECT locked_at IS NOT NULL FROM shift_handover WHERE id=7001", Boolean.class))
                .isTrue();
        assertThatThrownBy(() -> db.update("UPDATE shift_handover SET items=JSON_ARRAY() WHERE id=7001"))
                .isInstanceOf(DataAccessException.class);
    }

    private static long count(String table) {
        return db.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private static void column(String table, String column) {
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?",
                        Long.class,
                        table,
                        column))
                .as(table + "." + column)
                .isEqualTo(1L);
    }

    private static Map<String, Object> catalog(String kind) {
        var rows = db.queryForList(
                "SELECT id,attributes FROM catalog_item WHERE catalog_type=? AND JSON_UNQUOTE(attributes->'$.legacy.id')='7001'",
                kind);
        assertThat(rows).as(kind).hasSize(1);
        return rows.getFirst();
    }

    private static Map<String, Object> attributes(Map<String, Object> row) {
        return JSON.readValue((String) row.get("attributes"), Map.class);
    }
}
