package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.roombedallocation.api.*;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.api.spi.*;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.*;
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

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.administration.parameter.api.ParameterApi parameterSource;
}
