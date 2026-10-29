package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;

import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.roombedallocation.api.LocationHistoryApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/** T045–T046: SQL thật; chỉ kiểm soát đầu ra API khoảng vắng còn do M01 sở hữu. */
@Transactional
class FR_035_LocationHistoryIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @MockitoSpyBean
    ResidentStatusApi statuses;

    final Instant start = Instant.parse("2026-01-01T17:00:00Z");
    long resident;

    @BeforeEach
    void historyFixture() {
        resident = person();
        doReturn(List.of()).when(statuses).statusPeriods(anyLong(), anySet(), any(), any());
    }

    private LocationHistoryApi history() {
        var beans = context.getBeansOfType(LocationHistoryApi.class);
        assertThat(beans).as("T046 cần bản thật LocationHistoryApi").hasSize(1);
        return beans.values().iterator().next();
    }

    private long person() {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','IN_RESIDENCE',?,?,?)",
                id,
                Timestamp.from(start),
                Timestamp.from(start),
                user.accountId());
        return id;
    }

    private long allocation(long who, long targetBed, Instant from, Instant to) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source,opened_by_id) VALUES(?,?,?,'CLOSED',?,?,'BASIC','CURRENT',?)",
                id,
                who,
                targetBed,
                Timestamp.from(from),
                Timestamp.from(to),
                user.accountId());
        return id;
    }

    @Test
    void FR_035_halfOpenHistoryUsesLatestCorrectionAndVoid() {
        long id = allocation(resident, bed, start, start.plusSeconds(3600));
        var api = history();
        assertThat(api.byResident(resident, start, start.plusSeconds(7200))).hasSize(1);
        assertThat(api.byLocation(new LocationRef("ROOM", room), start.plusSeconds(3600)))
                .isEmpty();
        jdbc.update(
                "INSERT INTO correction(record_type,original_record_id,type,corrected_content,reason,actor_id,creation_method,recorded_at,sequence_no) VALUES('M03.BED_ALLOCATION',?,'AMEND_CONTENT',?,'Sửa mốc',?,'A',?,1)",
                id,
                "{\"endAt\":\"2026-01-01T19:00:00Z\"}",
                user.accountId(),
                Timestamp.from(time.now()));
        assertThat(api.byLocation(new LocationRef("FLOOR", floor), start.plusSeconds(3600)))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.corrected()).isTrue();
                    assertThat(row.to()).isEqualTo(start.plusSeconds(7200));
                });
        jdbc.update(
                "INSERT INTO correction(record_type,original_record_id,type,reason,actor_id,creation_method,recorded_at,sequence_no) VALUES('M03.BED_ALLOCATION',?,'VOID','Hủy ghi nhận',?,'A',?,2)",
                id,
                user.accountId(),
                Timestamp.from(time.now()));
        assertThat(api.byResident(resident, start, start.plusSeconds(7200))).isEmpty();
    }

    @Test
    void FR_035_calendarDayAndRoommatesClipOverlapAndIncludeBothAbsences() {
        allocation(resident, bed, start, start.plusSeconds(7200));
        long otherBed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", otherBed, room);
        long other = person();
        allocation(other, otherBed, start.plusSeconds(1800), start.plusSeconds(10800));
        doReturn(List.of(new StatusPeriod(
                        ResidentStatus.TEMPORARILY_ABSENT,
                        start.plusSeconds(2400),
                        Optional.of(start.plusSeconds(3000)))))
                .when(statuses)
                .statusPeriods(eq(resident), anySet(), any(), any());
        doReturn(List.of(new StatusPeriod(ResidentStatus.HOSPITALIZED, start.plusSeconds(3600), Optional.empty())))
                .when(statuses)
                .statusPeriods(eq(other), anySet(), any(), any());
        var api = history();
        assertThat(api.byLocationOn(new LocationRef("AREA", area), LocalDate.of(2026, 1, 1)))
                .isEmpty();
        assertThat(api.byLocationOn(new LocationRef("AREA", area), LocalDate.of(2026, 1, 2)))
                .hasSize(2);
        assertThat(api.roommates(resident, start, start.plusSeconds(10800)))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.residentId()).isEqualTo(other);
                    assertThat(row.from()).isEqualTo(start.plusSeconds(1800));
                    assertThat(row.to()).isEqualTo(start.plusSeconds(7200));
                    assertThat(row.absences()).hasSize(2);
                });
    }

    @Test
    void FR_035_invalidWindowAndUnknownLocationAreRejected() {
        var api = history();
        assertThatThrownBy(() -> api.byResident(resident, start, start))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        assertThatThrownBy(() -> api.byLocation(new LocationRef("TABLE", room), start))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
    }

    @Test
    void FR_037_userWithoutHistoryPermissionCannotQuery() {
        allocation(resident, bed, start, start.plusSeconds(3600));
        user.set(new TestUsers(jdbc, time)
                .staff(com.eldercare.backend.common.Role.CAREGIVER)
                .user());
        assertThatThrownBy(() -> context.getBean(
                                com.eldercare.backend.roombedallocation.internal.history.service.LocationHistoryQueries
                                        .class)
                        .residentStays(resident, start, start.plusSeconds(7200)))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
    }

    @Test
    void FR_037_moduleApiSupportsNurseCallerFor007() {
        allocation(resident, bed, start, start.plusSeconds(3600));
        user.set(new TestUsers(jdbc, time)
                .staff(com.eldercare.backend.common.Role.NURSING_STAFF)
                .user());
        assertThat(history().byResident(resident, start, start.plusSeconds(7200)))
                .hasSize(1);
    }

    @Test
    void FR_027_pendingRequestBeforeQueryStartStillMarksAffectedInterval() {
        long allocation = allocation(resident, bed, start, start.plusSeconds(7200));
        jdbc.update(
                "INSERT INTO approval_request(type,status,content,requester_id,created_at,reason,requested_effective_date) VALUES('M03.BED_BACKFILL','PENDING_APPROVAL','{}',?,?,'Nhập bù','2026-01-01')",
                user.accountId(),
                Timestamp.from(time.now()));
        // Cùng connection của transaction test; không đẩy AUTO_INCREMENT của module 000 bằng id fixture lớn.
        long request = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO bed_backfill_request(approval_request_id,resident_id,current_allocation_id,new_bed_id,transfer_at) VALUES(?,?,?,?,?)",
                request,
                resident,
                allocation,
                bed,
                Timestamp.from(start.plusSeconds(600)));
        assertThat(history().byResident(resident, start.plusSeconds(1800), start.plusSeconds(3600)))
                .singleElement()
                .satisfies(row -> assertThat(row.pendingBackfill()).isTrue());
    }

    @Test
    void SC_003_thirtyDaysTenPeopleThreeRoomsUnderOneSecond() {
        var people = new ArrayList<Long>();
        for (int i = 0; i < 10; i++) {
            long targetRoom = room;
            if (i == 3 || i == 6) {
                targetRoom = TestUsers.nextId();
                jdbc.update(
                        "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,'Phòng thử','UNRESTRICTED')",
                        targetRoom,
                        floor,
                        type,
                        "P" + targetRoom);
                room = targetRoom;
            }
            long targetBed = TestUsers.nextId();
            jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,?)", targetBed, targetRoom, "G" + targetBed);
            long who = person();
            people.add(who);
            allocation(who, targetBed, start, start.plusSeconds(30 * 86400));
        }
        var api = history();
        api.byResident(people.getFirst(), start, start.plusSeconds(30 * 86400));
        long began = System.nanoTime();
        for (long person : people)
            assertThat(api.byResident(person, start, start.plusSeconds(30 * 86400)))
                    .hasSize(1);
        for (int day : List.of(0, 1, 5, 15, 29))
            assertThat(api.byLocation(new LocationRef("AREA", area), start.plusSeconds(day * 86400)))
                    .hasSize(10);
        assertThat(api.roommates(people.getFirst(), start, start.plusSeconds(5 * 86400)))
                .hasSize(2);
        assertThat(java.time.Duration.ofNanos(System.nanoTime() - began)).isLessThan(java.time.Duration.ofSeconds(1));
    }
}
