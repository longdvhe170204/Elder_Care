package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.staffing.api.events.ShiftBelowStaffingThreshold;
import com.eldercare.backend.staffing.internal.ratio.domain.StaffingRatioCalculator.Result;
import com.eldercare.backend.staffing.internal.ratio.service.StaffingRatioService;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.roombedallocation.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.*;

@Import(FakeLocations.Config.class)
@RecordApplicationEvents
class StaffingRatioWarningIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StaffingRatioService service;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents events;

    @Autowired
    FakeLocations locations;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    long shift;
    long floor;
    Instant start;

    @BeforeEach
    void setup() {
        clock.setTo(Instant.parse("2026-10-25T00:00:00Z"));
        floor = TestUsers.nextId();
        locations.floor(floor, null);
        start = time.now().plus(Duration.ofDays(3));
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu tỷ lệ] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,FALSE AS requires_handover) src");
        long schedule = AssignmentFixtures.insert(
                jdbc, "INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2026-10','FLOOR',?)", floor);
        shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover) VALUES (?,?,?,?,'FLOOR',?,FALSE)",
                schedule,
                template,
                sql(start),
                sql(start.plus(Duration.ofHours(8))),
                floor);
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    private LocalDateTime sql(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private Result result(boolean passed) {
        return new Result(
                new BigDecimal("35.00"),
                passed ? 5 : 4,
                new BigDecimal(passed ? "7.00" : "8.75"),
                new BigDecimal("8.00"),
                passed);
    }

    private long count() {
        return events.stream(ShiftBelowStaffingThreshold.class)
                .filter(e -> e.shiftId() == shift)
                .count();
    }

    @Test
    void FR_036_draftNoNotification_publishWarnsOnce_recoveryAllowsNewEpisode() {
        service.record(shift, "DRAFT_SAVED", result(false));
        assertThat(count()).isZero();
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        service.record(shift, "PUBLISHED", result(false));
        service.record(shift, "CARE_LEVEL_CHANGED", result(false));
        assertThat(count()).isEqualTo(1);
        var first = events.stream(ShiftBelowStaffingThreshold.class)
                .filter(e -> e.shiftId() == shift)
                .findFirst()
                .orElseThrow();
        assertThat(first.notificationRequests().getFirst().recipientGroups())
                .anyMatch(RecipientGroup.FloorManager.class::isInstance);
        assertThat(first.notificationRequests().getFirst().recipientGroups())
                .noneMatch(RecipientGroup.FacilityRole.class::isInstance);
        service.record(shift, "STAFF_ADDED", result(true));
        clock.advanceBy(Duration.ofSeconds(1));
        service.record(shift, "STAFF_ABSENT", result(false));
        assertThat(count()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS shift_id,e.occurred_at AS calculated_at,NULLIF(JSON_UNQUOTE(e.content->'$.event'),'null') AS event,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.totalWeight'),'null') AS DECIMAL(8,2)) AS total_weight,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.staffCount'),'null') AS SIGNED) AS staff_count,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.ratio'),'null') AS DECIMAL(8,2)) AS ratio,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.threshold'),'null') AS DECIMAL(8,2)) AS threshold,(e.content->'$.meetsThreshold'=CAST('true' AS JSON)) AS meets_threshold FROM record_event e WHERE e.kind='M09.STAFFING_RATIO_CALCULATION') staffing_ratio_calculation WHERE shift_id=?",
                        Integer.class,
                        shift))
                .isEqualTo(5);
    }
}
