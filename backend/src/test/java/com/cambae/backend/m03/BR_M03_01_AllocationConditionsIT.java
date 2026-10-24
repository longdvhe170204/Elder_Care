package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m03.api.spi.*;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

/** T019/T025: cùng một kết quả FR-018 cho danh sách và lệnh, không dừng ở lỗi đầu. */
@Transactional
class BR_M03_01_AllocationConditionsIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    jakarta.persistence.EntityManager entities;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    FakeAllocationInfectionStatus infection;

    @Autowired
    FakeBedAssets assets;

    long resident;

    @BeforeEach
    void residentFixture() {
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        infection.reset();
        assets.reset();
    }

    @SuppressWarnings("unchecked")
    private List<UnmetCondition> check(Instant start, Instant end, boolean future) throws Exception {
        assertThat(context.containsBean("allocationChecker"))
                .as("Cần checker FR-018 dùng chung")
                .isTrue();
        Object checker = context.getBean("allocationChecker");
        return (List<UnmetCondition>) checker.getClass()
                .getMethod("check", long.class, long.class, Instant.class, Instant.class, Long.class, boolean.class)
                .invoke(checker, resident, bed, start, end, null, future);
    }

    @Test
    void BR_M03_01_validBedHasNoViolations() throws Exception {
        assertThat(check(time.now(), null, true)).isEmpty();
    }

    @Test
    void BR_M03_01_returnsAllIndependentViolations() throws Exception {
        jdbc.update("UPDATE resident SET current_care_level='SPECIAL',status='DECEASED' WHERE id=?", resident);
        jdbc.update("UPDATE room SET gender_policy='MALE',isolated=TRUE WHERE id=?", room);
        jdbc.update("UPDATE bed SET state='SUSPENDED',suspension_reason='Tạm ngừng' WHERE id=?", bed);
        residency.resident(resident, ResidencyType.DAY_CARE, null, null, null, null);
        assets.flag(bed, true);
        assertThat(check(time.now(), null, true))
                .extracting(UnmetCondition::code)
                .contains(
                        "ALLOCATION_CARE_LEVEL",
                        "ALLOCATION_RESIDENCY_TYPE",
                        "ALLOCATION_GENDER",
                        "ALLOCATION_ISOLATION",
                        "ALLOCATION_BED_STATE",
                        "ALLOCATION_PROFILE_CLOSED",
                        "ALLOCATION_ASSET_FLAG");
    }

    @Test
    void BR_M03_01_awaitingReadyOnlyAcceptsFutureAllocation() throws Exception {
        var deadline = time.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now()),
                Timestamp.from(deadline),
                bed);
        assertThat(check(deadline, null, true)).isEmpty();
        assertThat(check(time.now(), null, false))
                .extracting(UnmetCondition::code)
                .contains("ALLOCATION_BED_STATE");
        assertThat(check(time.now(), null, true))
                .extracting(UnmetCondition::code)
                .contains("ALLOCATION_NOT_READY_BEFORE_DEADLINE");
    }

    @Test
    void BR_M03_01_holdForAnotherResidentIsRejected() throws Exception {
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=? WHERE id=?",
                resident + 1,
                bed);
        assertThat(check(time.now(), null, true))
                .extracting(UnmetCondition::code)
                .contains("ALLOCATION_HOLD");
        jdbc.update("UPDATE bed SET hold_resident_id=? WHERE id=?", resident, bed);
        assertThat(check(time.now(), null, true)).isEmpty();
    }

    @Test
    void BR_M03_01_cordonBlocksDestinationFloor() throws Exception {
        infection.cordon(time.now().minusSeconds(1), new CordonedArea(Set.of(floor), Set.of()));
        assertThat(check(time.now(), null, true))
                .extracting(UnmetCondition::code)
                .contains("ALLOCATION_ISOLATION");
    }

    @Test
    void DBR_09_halfOpenIntervalsTouchButDoNotOverlap() throws Exception {
        Instant start = time.now();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                resident + 1,
                bed,
                Timestamp.from(start.minusSeconds(3600)),
                Timestamp.from(start));
        assertThat(check(start, null, true)).isEmpty();
        assertThat(check(start.minusSeconds(1), null, true))
                .extracting(UnmetCondition::code)
                .contains("ALLOCATION_TIME_OVERLAP");
    }

    @Test
    void BR_M03_01_contractFallbackOnlyForAdmission() throws Exception {
        jdbc.update("UPDATE resident SET current_care_level=NULL WHERE id=?", resident);
        residency.resident(resident, ResidencyType.LONG_TERM, CareLevelCode.BASIC, null, null, null);
        assertThat(check(time.now(), null, true)).isEmpty();
        jdbc.update("UPDATE resident SET status='IN_RESIDENCE' WHERE id=?", resident);
        entities.clear();
        assertThat(check(time.now(), null, false))
                .extracting(UnmetCondition::code)
                .contains("ALLOCATION_CARE_LEVEL");
    }
}
