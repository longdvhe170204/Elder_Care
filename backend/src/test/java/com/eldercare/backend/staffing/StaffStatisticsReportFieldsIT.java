package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.HandoverReportView;
import com.eldercare.backend.staffing.internal.ratio.domain.StaffingRatioCalculator.Result;
import com.eldercare.backend.staffing.internal.ratio.persistence.StaffingRatioStore;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
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
@org.springframework.transaction.annotation.Transactional
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
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu chỉ tiêu] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,TRUE AS requires_handover) src");
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
                "INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes) SELECT 'M09.CREDENTIAL_TYPE','M09',src.code,src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'credentialGroup',src.credential_group,'hasExpiry',IF(src.has_expiry,CAST('true' AS JSON),CAST('false' AS JSON)),'requiredForRoles',CAST(src.required_for_roles AS JSON),'hasPracticeScopes',IF(FALSE,CAST('true' AS JSON),CAST('false' AS JSON)),'legacyCode',src.code) FROM (SELECT ? AS code,? AS name,'PROFESSIONAL_LICENSE' AS credential_group,TRUE AS has_expiry,JSON_ARRAY() AS required_for_roles) src",
                "LIC-" + staff,
                "[Giấy phép chỉ tiêu " + staff + "]");
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE' AND COALESCE(c.m09_legacy_id,c.id)=src.credential_type_id),src.issue_date,src.expiry_date FROM (SELECT ? AS staff_id,? AS credential_type_id,? AS issue_date,? AS expiry_date) src",
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
