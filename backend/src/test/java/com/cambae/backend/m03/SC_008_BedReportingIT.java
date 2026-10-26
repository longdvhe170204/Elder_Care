package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m03.api.*;
import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.*;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

/** T061: API dữ liệu thật003; consumer016/008 tự kiểm quyền và truyền ScopeFilter. */
@Transactional
class SC_008_BedReportingIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeBedAssets assets;

    @Autowired
    FakeAllocationInfectionStatus infection;

    Instant at;

    @BeforeEach
    void reportingFixture() {
        clock.advanceBy(Duration.ofDays(1));
        at = time.now().minusSeconds(36000).truncatedTo(ChronoUnit.MICROS);
        infection.reset();
        fact(bed, "EMPTY", "CREATE_BED", at);
    }

    @AfterEach
    void reset() {
        clock.reset();
        infection.reset();
        assets.flag(bed, false);
    }

    private BedStatusApi status() {
        return context.getBean(BedStatusApi.class);
    }

    private BedStatisticsApi statistics() {
        return context.getBean(BedStatisticsApi.class);
    }

    private ScopeFilter floorScope() {
        return new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
    }

    private void fact(long id, String state, String event, Instant when) {
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,to_state,event,occurred_at,basis_type,basis_id,floor_area_id,disinfection_required) VALUES(?,?,?,?,'TEST',1,?,?)",
                id,
                state,
                event,
                Timestamp.from(when),
                floor,
                event.equals("CONFIRM_READY") ? Boolean.FALSE : null);
    }

    private long anotherBed(String code, Instant created) {
        long id = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,?)", id, room, code);
        fact(id, "EMPTY", "CREATE_BED", created);
        return id;
    }

    @Test
    void SC_008_stateCountsUseTheSameHistoricalSnapshotAndNeverCountBeforeCreation() {
        fact(bed, "AWAITING_READY", "REUSE", at.plusSeconds(3600));
        fact(bed, "EMPTY", "CONFIRM_READY", at.plusSeconds(7200));
        anotherBed("FUTURE", at.plusSeconds(10800));
        var point = at.plusSeconds(5400);
        var rows = statistics().stateAt(floorScope(), point).toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().state()).isEqualTo(BedStatus.AWAITING_READY);
        var counts = status().countByState(floorScope(), point);
        assertThat(counts.get(BedStatus.AWAITING_READY)).isEqualTo(1);
        assertThat(counts.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(rows.size());
        assertThat(statistics().stateAt(new ScopeFilter(false, Set.of(), Set.of(), Set.of()), point))
                .isEmpty();
    }

    @Test
    void FR_018_availableCountRejectsFutureReservationsAssetFlagsAndCordons() {
        long reserved = anotherBed("RESERVED", at);
        long damaged = anotherBed("DAMAGED", at);
        assets.flag(damaged, true);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                TestUsers.nextId(),
                reserved,
                Timestamp.from(at.plusSeconds(7200)));
        try {
            assertThat(status().countAvailable(
                                    floorScope(),
                                    CareLevelCode.BASIC,
                                    ResidentGender.FEMALE,
                                    ResidencyType.LONG_TERM,
                                    at.plusSeconds(1)))
                    .isEqualTo(1);
            assertThat(status().countAvailable(
                                    floorScope(),
                                    CareLevelCode.SPECIAL,
                                    ResidentGender.FEMALE,
                                    ResidencyType.LONG_TERM,
                                    at.plusSeconds(1)))
                    .isZero();
            infection.cordon(at, new CordonedArea(Set.of(), Set.of(area)));
            assertThat(status().countAvailable(
                                    floorScope(),
                                    CareLevelCode.BASIC,
                                    ResidentGender.FEMALE,
                                    ResidencyType.LONG_TERM,
                                    at.plusSeconds(1)))
                    .isZero();
        } finally {
            assets.flag(damaged, false);
        }
    }

    @Test
    void FR_024a_handoverOnlyReturnsUnresolvedDoctorFlagsAndWithoutBedHasNoInventedBed() {
        long resident = TestUsers.nextId();
        long lost = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source,awaiting_doctor_confirmation,room_unsuitable) VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT',TRUE,TRUE)",
                resident,
                bed,
                Timestamp.from(at));
        jdbc.update(
                "INSERT INTO resident_position(resident_id,without_bed_since) VALUES(?,?)", lost, Timestamp.from(at));
        assertThat(context.getBean(BedHandoverAlertApi.class).byFloor(floor, at.plusSeconds(1)))
                .hasSize(1)
                .allSatisfy(row -> assertThat(row.code()).isEqualTo("AWAITING_DOCTOR_CONFIRMATION"));
        var flags = status().flaggedBeds(new ScopeFilter(false, Set.of(), Set.of(), Set.of(resident, lost)));
        assertThat(flags).anySatisfy(row -> {
            assertThat(row.residentId()).isEqualTo(lost);
            assertThat(row.bedId()).isNull();
            assertThat(row.flags()).contains("WITHOUT_BED");
        });
        jdbc.update("UPDATE bed_allocation SET awaiting_doctor_confirmation=FALSE WHERE resident_id=?", resident);
        assertThat(context.getBean(BedHandoverAlertApi.class).byFloor(floor, at.plusSeconds(1)))
                .isEmpty();
    }

    @Test
    void FR_012_emptyPeriodSurvivesWaitingHoldButNotMaintenance() {
        fact(bed, "HELD", "HOLD_WAITING_RECORD", at.plusSeconds(60));
        fact(bed, "EMPTY", "HOLD_EXPIRED", at.plusSeconds(120));
        assertThat(status().emptyPeriodStarted(bed)).contains(at);
        fact(bed, "UNDER_MAINTENANCE", "ASSET_MAINTENANCE", at.plusSeconds(180));
        fact(bed, "EMPTY", "ASSET_READY", at.plusSeconds(240));
        assertThat(status().emptyPeriodStarted(bed)).contains(at.plusSeconds(240));
    }

    @Test
    void FR_044_statisticsIntersectReadinessWindowsAndReconstructIsolationIntervals() {
        fact(bed, "AWAITING_READY", "REUSE", at.plusSeconds(3600));
        fact(bed, "EMPTY", "CONFIRM_READY", at.plusSeconds(21600));
        var intervals = statistics()
                .awaitingReady(floorScope(), at.plusSeconds(10800), at.plusSeconds(25200))
                .toList();
        assertThat(intervals).hasSize(1);
        assertThat(intervals.getFirst().from()).isEqualTo(at.plusSeconds(10800));
        assertThat(intervals.getFirst().to()).isEqualTo(at.plusSeconds(21600));
        assertThat(intervals.getFirst().deadline()).isEqualTo(at.plusSeconds(18000));
        jdbc.update(
                "INSERT INTO room_isolation_history(room_id,action,occurred_at,actor_id,reason) VALUES(?,'ISOLATE',?,?,'Kiểm tra')",
                room,
                Timestamp.from(at),
                user.accountId());
        jdbc.update(
                "INSERT INTO room_isolation_history(room_id,action,occurred_at,actor_id,reason) VALUES(?,'LIFT',?,?,'Đủ điều kiện')",
                room,
                Timestamp.from(at.plusSeconds(3600)),
                user.accountId());
        assertThat(statistics().isolatedRooms(floorScope(), at.plusSeconds(1))).hasSize(1);
        assertThat(statistics().isolatedRooms(floorScope(), at.plusSeconds(3600)))
                .isEmpty();
        assertThat(status().overdueAwaitingReady(floor, at.plusSeconds(18001)))
                .extracting(BedView::id)
                .contains(bed);
    }

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.cambae.backend.m15.parameter.api.ParameterApi parameterSource;

    @Test
    void SC_008_residentScopeIsRecheckedForEachReadinessWindow() {
        long first = TestUsers.nextId();
        long second = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                first,
                bed,
                Timestamp.from(at),
                Timestamp.from(at.plusSeconds(3600)));
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                second,
                bed,
                Timestamp.from(at.plusSeconds(7200)),
                Timestamp.from(at.plusSeconds(10800)));
        fact(bed, "AWAITING_READY", "RESIDENCY_ENDED", at.plusSeconds(3600));
        fact(bed, "EMPTY", "CONFIRM_READY", at.plusSeconds(7200));
        fact(bed, "AWAITING_READY", "RESIDENCY_ENDED", at.plusSeconds(10800));
        fact(bed, "EMPTY", "CONFIRM_READY", at.plusSeconds(14400));
        var scopeFirst = new ScopeFilter(false, Set.of(), Set.of(), Set.of(first));
        var scopeSecond = new ScopeFilter(false, Set.of(), Set.of(), Set.of(second));
        assertThat(statistics().awaitingReady(scopeFirst, at, at.plusSeconds(18000)))
                .extracting(AwaitingReadyInterval::from)
                .containsExactly(at.plusSeconds(3600));
        assertThat(statistics().awaitingReady(scopeSecond, at, at.plusSeconds(18000)))
                .extracting(AwaitingReadyInterval::from)
                .containsExactly(at.plusSeconds(10800));
    }

    @Test
    void SC_008_readinessStreamMaterializesConfigurationBeforeReturning() {
        fact(bed, "AWAITING_READY", "REUSE", at.plusSeconds(3600));
        var target = org.springframework.test.util.AopTestUtils
                .<com.cambae.backend.m15.parameter.api.ParameterApi>getUltimateTargetObject(parameterSource);
        org.mockito.Mockito.clearInvocations(target);
        try (var result = statistics().awaitingReady(floorScope(), at, at.plusSeconds(7200))) {
            org.mockito.Mockito.verify(target)
                    .readAt(com.cambae.backend.m15.parameter.api.ParameterCode.CFG_M03_04, at.plusSeconds(3600));
            assertThat(result).hasSize(1);
        }
    }
}
