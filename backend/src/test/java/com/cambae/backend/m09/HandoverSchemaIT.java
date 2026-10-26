package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.m09.handover.internal.service.HandoverQueries;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T025/T026: ràng buộc và trigger kiểm trực tiếp trên MySQL, không chỉ qua service. */
@Transactional
class HandoverSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    HandoverQueries handovers;

    private long id() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long handover() {
        jdbc.update("INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên bàn giao]','Điều dưỡng')");
        long staff = id();
        jdbc.update(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type) VALUES ('[Mẫu bàn giao]','07:00','15:00','FLOOR')");
        long template = id();
        jdbc.update("INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2033-01','FLOOR',?)", template);
        long schedule = id();
        jdbc.update(
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover) VALUES (?,?,'2033-01-01','2033-01-01 08:00','FLOOR',?,TRUE)",
                schedule,
                template,
                template);
        long shift = id();
        jdbc.update(
                "INSERT INTO shift_handover(shift_id,handover_staff_id,created_at) VALUES (?,?,'2033-01-01 07:30')",
                shift,
                staff);
        return id();
    }

    private long item(long parent, String group) {
        jdbc.update(
                "INSERT INTO handover_item(handover_id,item_group,resident_id,description,created_at) VALUES (?,?,1,'Nội dung gốc','2033-01-01 07:31')",
                parent,
                group);
        return id();
    }

    @Test
    void DBR_20_oneHandoverPerShift() {
        long h = handover();
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO shift_handover(shift_id,handover_staff_id,created_at) SELECT shift_id,handover_staff_id,created_at FROM shift_handover WHERE id=?",
                        h))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M09_07_onlyPreparedOrConfirmedHandoverLetsShiftClose() {
        long h = handover();
        long shift = jdbc.queryForObject("SELECT shift_id FROM shift_handover WHERE id=?", Long.class, h);
        assertThat(handovers.prepared(shift)).isFalse();
        assertThat(handovers.confirmed(shift)).isFalse();
        jdbc.update("UPDATE shift_handover SET status='PREPARED',finalized_at='2033-01-01 07:50' WHERE id=?", h);
        assertThat(handovers.prepared(shift)).isTrue();
        jdbc.update("UPDATE shift_handover SET status='HAS_COMMENTS' WHERE id=?", h);
        assertThat(handovers.prepared(shift)).isFalse();
        jdbc.update("UPDATE shift_handover SET status='PREPARED' WHERE id=?", h);
        jdbc.update("UPDATE shift_handover SET status='CONFIRMED',confirmed_at='2033-01-01 07:59' WHERE id=?", h);
        assertThat(handovers.prepared(shift)).isTrue();
        assertThat(handovers.confirmed(shift)).isTrue();
        assertThat(handovers.prepared(-1)).isFalse();
        assertThat(handovers.confirmed(-1)).isFalse();
    }

    @Test
    void FR_040_vitalSignsAndResidentChangesCannotBeDiscardedInDraft() {
        long h = handover();
        for (String group : new String[] {"VITAL_SIGN", "RESIDENT_CHANGE"}) {
            long i = item(h, group);
            assertThatThrownBy(() -> jdbc.update("DELETE FROM handover_item WHERE id=?", i))
                    .isInstanceOf(DataAccessException.class);
        }
        long ordinary = item(h, "TASK");
        assertThat(jdbc.update("DELETE FROM handover_item WHERE id=?", ordinary))
                .isOne();
    }

    @Test
    void FR_040_protectedSourceCannotBeRenamedToBypassDeletionGuard() {
        long h = handover(), i = item(h, "VITAL_SIGN");
        assertThatThrownBy(() -> jdbc.update("UPDATE handover_item SET item_group='FOLLOW_UP_ISSUE' WHERE id=?", i))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_041_finalizationLocksOriginalItemsAssessmentsAndTimestamp() {
        long h = handover(), i = item(h, "TASK");
        jdbc.update(
                "INSERT INTO handover_assessment(handover_id,assessment_kind,content) VALUES (?,'GENERAL','Nhận định')",
                h);
        jdbc.update("UPDATE shift_handover SET finalized_at='2033-01-01 07:50',status='PREPARED' WHERE id=?", h);
        assertThatThrownBy(() -> jdbc.update("UPDATE shift_handover SET finalized_at='2033-01-01 07:51' WHERE id=?", h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE handover_item SET note='Thay đổi' WHERE id=?", i))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM handover_item WHERE id=?", i))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE handover_assessment SET content='Thay đổi' WHERE handover_id=?", h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> item(h, "TASK")).isInstanceOf(DataAccessException.class);
        jdbc.update(
                "INSERT INTO handover_item(handover_id,item_group,resident_id,description,section,created_at) VALUES (?,'TASK',1,'Bổ sung','AFTER_PREPARATION','2033-01-01 07:55')",
                h);
        long appended = id();
        assertThatThrownBy(() -> jdbc.update("UPDATE handover_item SET description='Sửa bổ sung' WHERE id=?", appended))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM handover_item WHERE id=?", appended))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_044_confirmedParentAndChildrenAreImmutableAndRepliesAppendOnly() {
        long h = handover();
        jdbc.update(
                "INSERT INTO handover_comment(handover_id,content,sender,created_at) VALUES (?,'Thiếu ghi chú','[Người nhận]','2033-01-01 07:50')",
                h);
        long comment = id();
        jdbc.update(
                "INSERT INTO handover_reply(comment_id,content,sender,created_at) VALUES (?,'Đã bổ sung','[Người giao]','2033-01-01 07:55')",
                comment);
        assertThatThrownBy(() -> jdbc.update("UPDATE handover_reply SET content='Sửa' WHERE comment_id=?", comment))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM handover_comment WHERE id=?", comment))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("UPDATE shift_handover SET status='PREPARED',finalized_at='2033-01-01 07:56' WHERE id=?", h);
        jdbc.update("UPDATE shift_handover SET status='CONFIRMED',confirmed_at='2033-01-01 07:58' WHERE id=?", h);
        assertThatThrownBy(() -> jdbc.update("UPDATE shift_handover SET status='PREPARED' WHERE id=?", h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO handover_item(handover_id,item_group,resident_id,description,section,created_at) VALUES (?,'TASK',1,'Muộn','AFTER_PREPARATION','2033-01-01 07:59')",
                        h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO handover_comment(handover_id,content,sender,created_at) VALUES (?,'Muộn','[Người nhận]','2033-01-01 07:59')",
                        h))
                .isInstanceOf(DataAccessException.class);
    }
}
