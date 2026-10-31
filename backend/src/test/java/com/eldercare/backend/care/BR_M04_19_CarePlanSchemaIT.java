package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T011 trước T012: MySQL phải giữ DBR-11 và khóa mục ở ngoàiNháp. */
@Transactional
class BR_M04_19_CarePlanSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    long resident;
    int revision;

    @BeforeEach
    void resident() {
        resident = TestUsers.nextId();
        revision = 0;
    }

    private long plan(String status) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,created_by,created_at) VALUES(?,?,?,?,1,'2026-10-31 08:00:00')",
                id,
                resident,
                ++revision,
                status);
        return id;
    }

    private long task(long plan) {
        long type = TestUsers.nextId(), id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE','{}')",
                type,
                "Loại kiểm " + type);
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,task_type_id,frequency_type,data) VALUES(?,?,'TASK',?,'ONCE_PER_SHIFT','{}')",
                id,
                plan,
                type);
        return id;
    }

    @Test
    void DBR_11_onlyOneOpenRevisionPerResident() {
        plan("DRAFT");
        assertThatThrownBy(() -> plan("PENDING_APPROVAL")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_11_onlyOneEffectiveRevisionPerResident() {
        plan("EFFECTIVE");
        assertThatThrownBy(() -> plan("EFFECTIVE")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_003_approvalDateRequiresTheFollowingBusinessDay() {
        long id = plan("DRAFT");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE care_plan SET status='PENDING_EFFECT',approved_at='2026-10-31 23:59:59',effective_from='2026-10-31' WHERE id=?",
                        id))
                .isInstanceOf(DataAccessException.class);
        jdbc.update(
                "UPDATE care_plan SET status='PENDING_EFFECT',approved_at='2026-10-31 23:59:59',effective_from='2026-11-01' WHERE id=?",
                id);
    }

    @Test
    void BR_M04_19_itemsCannotBeAddedChangedOrDeletedOutsideDraft() {
        long id = plan("DRAFT"), item = task(id);
        jdbc.update("UPDATE care_plan SET status='PENDING_APPROVAL',effective_from='2026-11-01' WHERE id=?", id);
        assertThatThrownBy(() -> task(id)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE care_plan_item SET data='{}' WHERE id=?", item))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM care_plan_item WHERE id=?", item))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M04_25_everyNHoursRequiresAnIntervalAndStartTime() {
        long id = plan("DRAFT"), item = task(id);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE care_plan_item SET frequency_type='EVERY_N_HOURS',interval_hours=0,starts_at='01:00' WHERE id=?",
                        item))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE care_plan_item SET frequency_type='EVERY_N_HOURS',interval_hours=2,starts_at=NULL WHERE id=?",
                        item))
                .isInstanceOf(DataAccessException.class);
        jdbc.update(
                "UPDATE care_plan_item SET frequency_type='EVERY_N_HOURS',interval_hours=2,starts_at='01:00' WHERE id=?",
                item);
    }

    @Test
    void DBR_23_newTablesDeclareTheirBusinessDataGroup() {
        for (String table : java.util.List.of("care_plan", "care_plan_item"))
            assertThat(jdbc.queryForObject(
                            "SELECT table_comment FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                            String.class,
                            table))
                    .isEqualTo("DataGroup=BUSINESS");
    }

    private long agreement(long plan) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,data,choice,discussed_risks,resident_participates,assessing_doctor_id,can_self_determine,support_method,assessment_at,assessment_review_date,decision_maker_type,review_date) VALUES(?,?,'RISK_AGREEMENT','{}','Lựa chọn','Đã trao đổi',TRUE,1,TRUE,'Đã hỗ trợ','2026-10-31','2026-11-30','RESIDENT','2026-11-30')",
                id,
                plan);
        return id;
    }

    @Test
    void FR_002a_riskAgreementHasNoTaskFrequencyAndRequiresConsistentDecisionMaker() {
        long id = plan("DRAFT"), agreement = agreement(id);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE care_plan_item SET frequency_type='ONCE_PER_SHIFT' WHERE id=?", agreement))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE care_plan_item SET can_self_determine=FALSE WHERE id=?", agreement))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE care_plan_item SET support_method=NULL WHERE id=?", agreement))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M04_19_sharedRiskOpinionCannotBeInsertedOrChangedAfterSubmission() {
        long id = plan("DRAFT"), agreement = agreement(id), event = TestUsers.nextId();
        String insert =
                "INSERT INTO record_event(id,entity_type,entity_id,module,kind,content,by_system,occurred_at) VALUES(?,'M04.RISK_AGREEMENT',?,'M04','M04.RISK_OPINION','{}',FALSE,'2026-10-31')";
        jdbc.update(insert, event, agreement);
        jdbc.update("UPDATE care_plan SET status='PENDING_APPROVAL',effective_from='2026-11-01' WHERE id=?", id);
        assertThatThrownBy(() -> jdbc.update(insert, TestUsers.nextId(), agreement))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE record_event SET content='{}' WHERE id=?", event))
                .isInstanceOf(DataAccessException.class);
    }
}
