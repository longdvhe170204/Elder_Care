package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.staffing.internal.handover.domain.HandoverItemInput;
import com.eldercare.backend.staffing.internal.handover.persistence.HandoverStore;
import com.eldercare.backend.staffing.internal.handover.service.HandoverQueries;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T050: giữ kịch bản trigger trên JSON; điều kiện thêm ý kiến sau gộp kiểm ở backend theo data-model 0.1. */
@Transactional
class HandoverSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    HandoverQueries handovers;

    @Autowired
    HandoverStore store;

    private static final Instant AT = Instant.parse("2033-01-01T07:31:00Z");

    private long id() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long handover() {
        jdbc.update("INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên bàn giao]','Điều dưỡng')");
        long staff = id();
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES ('M09.SHIFT_TEMPLATE','M09','[Mẫu schema bàn giao]','ACTIVE',JSON_OBJECT('startTime','07:00:00','endTime','15:00:00','scopeType','FLOOR','requiresHandover',CAST('true' AS JSON)))");
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

    private HandoverItemInput input(String group, String description) {
        return new HandoverItemInput(group, null, null, 1L, null, description, null, false);
    }

    private long item(long parent, String group) {
        return store.add(parent, input(group, "Nội dung gốc"), "FINALIZED", null, AT);
    }

    private String path(long item) {
        return "$[" + (item - 1) + "]";
    }

    private int remove(long parent, long item) {
        return jdbc.update("UPDATE shift_handover SET items=JSON_REMOVE(items,?) WHERE id=?", path(item), parent);
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
            assertThatThrownBy(() -> remove(h, i)).isInstanceOf(DataAccessException.class);
        }
        long ordinary = item(h, "TASK");
        assertThat(remove(h, ordinary)).isOne();
    }

    @Test
    void FR_040_protectedSourceCannotBeRenamedToBypassDeletionGuard() {
        long h = handover(), i = item(h, "VITAL_SIGN");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE shift_handover SET items=JSON_SET(items,?,'FOLLOW_UP_ISSUE') WHERE id=?",
                        path(i) + ".itemGroup",
                        h))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_041_finalizationLocksOriginalItemsAssessmentsAndTimestamp() {
        long h = handover(), i = item(h, "TASK");
        store.assess(h, "Nhận định", Map.of());
        jdbc.update("UPDATE shift_handover SET finalized_at='2033-01-01 07:50',status='PREPARED' WHERE id=?", h);
        assertThatThrownBy(() -> jdbc.update("UPDATE shift_handover SET finalized_at='2033-01-01 07:51' WHERE id=?", h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE shift_handover SET items=JSON_SET(items,?,'Thay đổi') WHERE id=?",
                        path(i) + ".note",
                        h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> remove(h, i)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE shift_handover SET assessments=JSON_SET(assessments,'$[0].content','Thay đổi') WHERE id=?",
                        h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> item(h, "TASK")).isInstanceOf(DataAccessException.class);
        long appended = store.add(h, input("TASK", "Bổ sung"), "AFTER_PREPARATION", null, AT);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE shift_handover SET items=JSON_SET(items,?,'Sửa bổ sung') WHERE id=?",
                        path(appended) + ".description",
                        h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> remove(h, appended)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_044_confirmedParentAndChildrenAreImmutableAndRepliesAppendOnly() {
        long h = handover();
        jdbc.update("UPDATE shift_handover SET status='PREPARED',finalized_at='2033-01-01 07:50' WHERE id=?", h);
        long comment = store.comment(h, "Thiếu ghi chú", "[Người nhận]", AT);
        store.state(h, "HAS_COMMENTS");
        store.reply(comment, "Đã bổ sung", "[Người giao]", AT);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE record_event SET content=JSON_SET(content,'$.content','Sửa') WHERE kind='M09.HANDOVER_REPLY' AND reply_to_id=?",
                        comment))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() ->
                        jdbc.update("DELETE FROM record_event WHERE kind='M09.HANDOVER_COMMENT' AND id=?", comment))
                .isInstanceOf(DataAccessException.class);
        store.state(h, "PREPARED");
        jdbc.update("UPDATE shift_handover SET status='CONFIRMED',confirmed_at='2033-01-01 07:58' WHERE id=?", h);
        assertThatThrownBy(() -> jdbc.update("UPDATE shift_handover SET status='PREPARED' WHERE id=?", h))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> store.add(h, input("TASK", "Muộn"), "AFTER_PREPARATION", null, AT))
                .isInstanceOf(DataAccessException.class);
        // Kiểm backend thay trigger thêm của bảng con cũ; common API không biết trạng thái nghiệp vụ của cha.
        assertThatThrownBy(() -> store.comment(h, "Muộn", "[Người nhận]", AT)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> store.reply(comment, "Muộn", "[Người giao]", AT))
                .isInstanceOf(BusinessException.class);
    }
}
