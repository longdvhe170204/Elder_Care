package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.care.api.spi.TimetableSourcePort;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 014 T006 (module-interfaces 1.1, research L-15): phạm vi, phân loại trường, lưu giữ, thời khóa biểu. Bảng buổi, đăng ký
 * tạo ở US1; trước đó provider báo thiếu bản ghi, nguồn thời khóa biểu trả rỗng (mẫu T008 của 005).
 */
class T006_ActivityGovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<FieldClassificationProvider> fields;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    List<TimetableSourcePort> timetable;

    @Autowired
    JdbcTemplate jdbc;

    private ScopeAssignmentProvider scope(String type) {
        return scopes.stream()
                .filter(p -> p.entityType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu GanPhamViProvider " + type));
    }

    @Test
    void FR_034_preferenceIsScopedByResidentAndChecklistByFloor() {
        long resident = TestUsers.nextId(), floor = TestUsers.nextId();
        long preference = insert("M04.PREFERENCE", resident, "{}");
        long checklist = insert("M04.QUALITY_CHECKLIST", null, "{\"shiftId\":1,\"floorAreaId\":" + floor + "}");
        try {
            var p = scope("M04.PREFERENCE").assignScope(preference);
            assertThat(p.residentId()).isEqualTo(resident);
            var q = scope("M04.QUALITY_CHECK").assignScope(checklist);
            assertThat(q.residentId()).isNull();
            assertThat(q.floorAreaId()).isEqualTo(floor);
            // Id của loại khác không được nhận nhầm.
            assertThatThrownBy(() -> scope("M04.QUALITY_CHECK").assignScope(preference))
                    .isInstanceOf(InvalidPermissionDataException.class);
        } finally {
            cleanup("DELETE FROM simple_record WHERE id IN (" + preference + "," + checklist + ")");
        }
    }

    @Test
    void FR_034_sessionRegistrationTripReportMissingRecordUntilTheirTablesExist() {
        long id = TestUsers.nextId();
        for (String type : List.of("M04.ACTIVITY_SESSION", "M04.ACTIVITY_REGISTRATION", "M04.TRIP"))
            assertThatThrownBy(() -> scope(type).assignScope(id)).isInstanceOf(InvalidPermissionDataException.class);
    }

    @Test
    void FR_018_acknowledgedWarningsAreHealthInformation() {
        var provider = fields.stream()
                .filter(p -> p.entityType().equals("M04.ACTIVITY_REGISTRATION"))
                .findFirst()
                .orElseThrow();
        assertThat(provider.fieldGroups()).containsEntry("acknowledgedWarnings", InfoGroup.HEALTH);
    }

    @Test
    void L_15_sessionsAreNotResidentLinkedAndRegistrationsFollowTheResidentRecord() {
        var session = retention.stream()
                .filter(p -> p.dataType().equals("M04.ACTIVITY_SESSION"))
                .findFirst()
                .orElseThrow();
        var registration = retention.stream()
                .filter(p -> p.dataType().equals("M04.ACTIVITY_REGISTRATION"))
                .findFirst()
                .orElseThrow();
        assertThat(session.retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        assertThat(registration.retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        var cutoff = Instant.parse("2001-01-01T00:00:00Z");
        if (!table("activity_session")) {
            assertThat(session.expiredCandidates(cutoff, 100)).isEmpty();
            assertThat(registration.expiredCandidates(cutoff, 100)).isEmpty();
        }
    }

    @Test
    void FR_048_activityTimetableSourceIsRegisteredAndEmptyWithoutSessions() {
        var source = timetable.stream()
                .filter(p -> p.getClass().getPackageName().startsWith("com.eldercare.backend.care.internal.session"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu ThoiKhoaBieuNguonPort của 014"));
        assertThat(source.onDate(TestUsers.nextId(), LocalDate.of(2026, 11, 5))).isEmpty();
    }

    private boolean table(String name) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                        Integer.class,
                        name)
                > 0;
    }

    private long insert(String kind, Long resident, String content) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,resident_id,status,content,created_at,created_by)"
                        + " VALUES(?,?,'M04',?,'ACTIVE',?,'2026-11-05',1)",
                id,
                kind,
                resident,
                content);
        return id;
    }

    private void cleanup(String sql) {
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException(error);
        }
    }
}
