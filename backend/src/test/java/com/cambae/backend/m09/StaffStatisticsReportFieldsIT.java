package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.StaffStatisticsApi;
import com.cambae.backend.m09.api.dto.HandoverReportView;
import com.cambae.backend.m09.ratio.internal.domain.StaffingRatioCalculator.Result;
import com.cambae.backend.m09.ratio.internal.persistence.StaffingRatioStore;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.support.*;
import com.cambae.backend.support.fake.m03.FakeLocations;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Trường bổ sung của {@code StaffStatisticsApi} cho chỉ tiêu 016 (luật "Đổi api", chủ dự án duyệt 2026-10-27): số nhân
 * viên, tổng trọng số, mẫu ca, giờ ca của lần tính tỷ lệ (016 FR-070); bàn giao đang mở kèm giờ kết thúc ca (016 FR-020);
 * nhóm chứng chỉ (016 FR-020, CFG-M06-02 / CFG-M09-03).
 */
@Import(FakeLocations.Config.class)
class StaffStatisticsReportFieldsIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StaffStatisticsApi statistics;

    @Autowired
    StaffingRatioStore store;

    @Autowired
    FakeLocations locations;

    @Autowired
    BusinessTime time;

    final Instant start = Instant.parse("2026-10-25T00:00:00Z");
    final LocalDate day = LocalDate.of(2026, 10, 25);

    private long template() {
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu chỉ tiêu]','07:00','15:00','FLOOR',TRUE)");
    }

    private final Map<Long, Long> schedules = new HashMap<>();

    private long shift(long template, long floor, Instant from, String status) {
        // Một lịch tháng cho mỗi tầng (uq_shift_schedule_month).
        long schedule = schedules.computeIfAbsent(
                floor,
                f -> AssignmentFixtures.insert(
                        jdbc,
                        "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10','FLOOR',?,'PUBLISHED')",
                        f));
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,?,?,'FLOOR',?,TRUE,?)",
                schedule,
                template,
                ShiftStore.sql(from),
                ShiftStore.sql(from.plusSeconds(28800)),
                floor,
                status);
    }

    private long staff() {
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên chỉ tiêu kiểm thử]','Chăm sóc')");
    }

    private long handover(long shift, String status, Instant finalizedAt, Instant confirmedAt) {
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_handover(shift_id,handover_staff_id,status,created_at,finalized_at,confirmed_at) VALUES (?,?,?,?,?,?)",
                shift,
                staff(),
                status,
                ShiftStore.sql(start),
                finalizedAt == null ? null : ShiftStore.sql(finalizedAt),
                confirmedAt == null ? null : ShiftStore.sql(confirmedAt));
    }

    @Test
    void FR_070_ratioCarriesStaffCountWeightTemplateAndShiftTimes() {
        long floor = TestUsers.nextId(), template = template();
        locations.floor(floor, null);
        long id = shift(template, floor, start, "CLOSED");
        store.append(
                id,
                start.plusSeconds(3600),
                "CARE_LEVEL_CHANGED",
                new Result(new BigDecimal("35.00"), 5, new BigDecimal("7.00"), new BigDecimal("8.00"), true));

        var rows = statistics
                .staffingRatios(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), day, day)
                .toList();

        assertThat(rows).singleElement().satisfies(r -> {
            assertThat(r.staffCount()).isEqualTo(5);
            assertThat(r.totalWeight()).isEqualByComparingTo("35.00");
            assertThat(r.shiftTemplateId()).isEqualTo(template);
            assertThat(r.shiftStartsAt()).isEqualTo(start);
            assertThat(r.shiftEndsAt()).isEqualTo(start.plusSeconds(28800));
        });
    }

    @Test
    void FR_020_openHandoversArePreparedOrCommented_orDraftOfShiftAwaitingHandover() {
        long floor = TestUsers.nextId(), other = TestUsers.nextId(), template = template();
        locations.floor(floor, null).floor(other, null);
        Instant old = start.minus(Duration.ofDays(40));
        long draftAwaiting = handover(shift(template, floor, start, "AWAITING_HANDOVER"), "DRAFT", null, null);
        handover(shift(template, floor, start, "IN_PROGRESS"), "DRAFT", null, null);
        long preparedLongAgo =
                handover(shift(template, floor, old, "CLOSED"), "PREPARED", old.plusSeconds(28000), null);
        long commented = handover(shift(template, floor, start, "CLOSED"), "HAS_COMMENTS", start, null);
        handover(shift(template, floor, start, "CLOSED"), "CONFIRMED", start, start.plusSeconds(60));
        handover(shift(template, other, start, "CLOSED"), "PREPARED", start, null);

        var rows = statistics
                .openHandovers(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()))
                .toList();

        assertThat(rows)
                .extracting(HandoverReportView::handoverId)
                .containsExactlyInAnyOrder(draftAwaiting, preparedLongAgo, commented);
        assertThat(rows)
                .filteredOn(r -> r.handoverId() == preparedLongAgo)
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.status()).isEqualTo("PREPARED");
                    assertThat(r.shiftEndsAt()).isEqualTo(old.plusSeconds(28800));
                    assertThat(r.floorAreaId()).isEqualTo(floor);
                });
    }

    @Test
    void FR_020_expiringCredentialCarriesGroup() {
        long staff = staff();
        long type = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential_type(code,name,credential_group,has_expiry,required_for_roles) VALUES (?,?,'PROFESSIONAL_LICENSE',TRUE,JSON_ARRAY())",
                "LIC-" + staff,
                "[Giấy phép chỉ tiêu " + staff + "]");
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) VALUES (?,?,?,?)",
                staff,
                type,
                time.today().minusYears(1),
                time.today().plusDays(10));

        var rows = statistics
                .expiringCredentials(30)
                .filter(c -> c.staffId() == staff)
                .toList();

        assertThat(rows)
                .singleElement()
                .satisfies(c -> assertThat(c.credentialGroup()).isEqualTo("PROFESSIONAL_LICENSE"));
    }
}
