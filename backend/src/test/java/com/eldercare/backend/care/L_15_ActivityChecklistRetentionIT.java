package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 014 T006, T034 (research L-15, R-15; chủ dự án chọn 07/11/2026): hoạt động và danh sách kiểm tra chất lượng không gắn
 * hồ sơ. Hoạt động tính hạn từ ngày ngừng hiệu lực, còn buổi trỏ tới thì cả chuỗi chờ; danh sách tính hạn từ lúc đóng
 * (khóa dòng), mục kiểm tra xóa cùng danh sách qua dòng con bảng chung. Chuỗi FR-051a nối mục kiểm tra với công việc gốc
 * và công việc làm lại.
 */
class L_15_ActivityChecklistRetentionIT extends MySqlIntegrationTestBase {
    private static final Instant CUTOFF = Instant.parse("2001-01-01T00:00:00Z");

    @Autowired
    List<RetentionProvider> providers;

    @Autowired
    JdbcTemplate jdbc;

    private final List<String> cleanup = new ArrayList<>();

    /** Tài khoản ứng dụng không có quyền xóa, trigger khóa dòng chặn quản trị (R-14, R-15): dọn bằng tài khoản loại bỏ. */
    @AfterEach
    void clean() {
        for (String sql : cleanup.reversed()) retention(sql);
    }

    static void retention(String sql) {
        try (var c = retentionConnection();
                var st = c.createStatement()) {
            st.executeUpdate(sql);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    RetentionProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu RetentionProvider " + type));
    }

    long simple(String kind, String status, String content, LocalDateTime lockedAt) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,locked_at,created_at,created_by)"
                        + " VALUES(?,?,'M04',?,?,?,NOW(6),1)",
                id,
                kind,
                status,
                content,
                lockedAt);
        cleanup.add("DELETE FROM simple_record WHERE id=" + id);
        return id;
    }

    long task(String basisType, Long basisId) {
        long id = TestUsers.nextId();
        var at = LocalDateTime.of(2000, 1, 5, 9, 0);
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,"
                        + "window_from,window_to,importance,performer_role,status,data,created_by,created_at)"
                        + " VALUES(?,?,1,?,?,1,1,?,?,?,'NORMAL','CAREGIVER','COMPLETED','{}',1,NOW(6))",
                id,
                TestUsers.nextId(),
                basisType,
                basisId,
                at,
                at,
                at);
        cleanup.add("DELETE FROM care_task WHERE id=" + id);
        return id;
    }

    @Test
    void L_15_activityExpiresFromItsDeactivationDateAndChainsItsSessions() {
        var activities = provider("M04.ACTIVITY");
        assertThat(activities.retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        long active = simple("M04.ACTIVITY", "ACTIVE", "{\"name\":\"Hát\"}", null);
        long inactive =
                simple("M04.ACTIVITY", "INACTIVE", "{\"name\":\"Hát\",\"deactivatedFrom\":\"2000-01-10\"}", null);
        long session = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,status,created_at,created_by)"
                        + " VALUES(?,?,'2000-01-03 09:00:00','2000-01-03 10:00:00','SINGLE','SCHEDULED',NOW(6),1)",
                session,
                inactive);
        cleanup.add("DELETE FROM activity_session WHERE id=" + session);

        assertThat(activities.expiredCandidates(CUTOFF, 1000).filter(c -> c.id() == active || c.id() == inactive))
                .containsExactly(new RetentionProvider.Candidate(
                        inactive,
                        LocalDate.of(2000, 1, 10)
                                .atStartOfDay(BusinessTime.ZONE)
                                .toInstant()));
        assertThat(activities.referencesTo(inactive)).containsExactly(new DataRef("M04.ACTIVITY_SESSION", session));

        // Job xóa bản trỏ tới (buổi) trước bản bị trỏ; ở đây chỉ kiểm lệnh xóa của provider.
        retention("DELETE FROM activity_session WHERE id=" + session);
        activities.delete(List.of(inactive));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE id IN (?,?)", Integer.class, active, inactive))
                .isEqualTo(1);
    }

    @Test
    void L_15_closedChecklistExpiresFromClosingAndChainsOriginalAndReworkTasks() {
        var checklists = provider("M04.QUALITY_CHECKLIST");
        assertThat(checklists.retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        long open = simple("M04.QUALITY_CHECKLIST", "OPEN", "{\"shiftId\":1,\"floorAreaId\":1}", null);
        long closed = simple(
                "M04.QUALITY_CHECKLIST",
                "CLOSED",
                "{\"shiftId\":1,\"floorAreaId\":1}",
                LocalDateTime.of(2000, 2, 1, 7, 0));
        long original = task("AD_HOC", null);
        long item = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO record_event(id,entity_type,entity_id,module,kind,content,by_system,occurred_at)"
                        + " VALUES(?,'M04.QUALITY_CHECKLIST',?,'M04','M04.QUALITY_CHECK_ITEM',?,FALSE,'2000-02-01 06:00:00')",
                item,
                closed,
                "{\"checklistId\":" + closed + ",\"taskId\":" + original + ",\"status\":\"FAIL\"}");
        cleanup.add("DELETE FROM record_event WHERE id=" + item);
        long rework = task("REWORK", item);

        assertThat(checklists.expiredCandidates(CUTOFF, 1000).filter(c -> c.id() == open || c.id() == closed))
                .containsExactly(new RetentionProvider.Candidate(
                        closed,
                        LocalDateTime.of(2000, 2, 1, 7, 0)
                                .atZone(BusinessTime.ZONE)
                                .toInstant()));
        // Công việc làm lại trỏ tới mục của danh sách; mục trỏ tới công việc gốc (FR-051a).
        assertThat(checklists.referencesTo(closed)).containsExactly(new DataRef("M04.TASK", rework));
        assertThat(provider("M04.TASK").referencesTo(original)).contains(new DataRef("M04.QUALITY_CHECKLIST", closed));

        checklists.delete(List.of(closed));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE id IN (?,?)", Integer.class, open, closed))
                .isEqualTo(1);
    }
}
