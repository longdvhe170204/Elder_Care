package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.api.spi.ResidencyType;
import com.cambae.backend.m03.internal.structure.domain.*;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** T012: kiểm chính sách với API thật M01 và nguồn lưu trú đúng chủ khai báo. */
@Transactional
class FR_006_RoomPolicyIT extends StructureTestSupport {
    @Autowired
    com.cambae.backend.m03.internal.bed.service.BedService beds;

    @Autowired
    FakeAllocationResidencySource residency;

    private long resident(String gender, String level, String status) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01',?,?,?, ?,?,?)",
                id,
                gender,
                status,
                Timestamp.from(time.now()),
                level,
                Timestamp.from(time.now()),
                user.accountId());
        return id;
    }

    private void allocation(long resident, String status) {
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,?, 'BASIC','CURRENT')",
                resident,
                bed,
                status,
                Timestamp.from(time.now()));
    }

    private Room revise(GenderPolicy gender, Set<CareLevelCode> levels) {
        return (Room) structures.update(
                StructureKind.ROOM,
                room,
                "\"0\"",
                StructureChange.room(floor, type, "R" + room, "Phòng " + room, gender, levels));
    }

    @Test
    void FR_006_allowsCompatiblePolicyWithActiveResident() {
        allocation(resident("FEMALE", "BASIC", "IN_RESIDENCE"), "ACTIVE");
        assertThat(revise(GenderPolicy.FEMALE, Set.of(CareLevelCode.BASIC)).genderPolicy())
                .isEqualTo(GenderPolicy.FEMALE);
    }

    @Test
    void FR_006_listsOnlyViolatingResidentsAndRollsBackPolicy() {
        long male = resident("MALE", "BASIC", "IN_RESIDENCE");
        long female = resident("FEMALE", "BASIC", "ADMISSION_IN_PROGRESS");
        allocation(male, "ACTIVE");
        long second = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", second, room);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                female,
                second,
                Timestamp.from(time.now()));
        assertThatThrownBy(() -> revise(GenderPolicy.FEMALE, Set.of(CareLevelCode.BASIC)))
                .isInstanceOfSatisfying(ConditionNotMetException.class, e -> {
                    assertThat(e.unmetConditions()).hasSize(1);
                    assertThat(e.unmetConditions().getFirst().description())
                            .contains(Long.toString(male))
                            .doesNotContain(Long.toString(female));
                });
        assertThat(jdbc.queryForObject("SELECT gender_policy FROM room WHERE id=?", String.class, room))
                .isEqualTo("UNRESTRICTED");
    }

    @Test
    void FR_006_mapsRegularLevelWithoutRejectingCompatibleFutureAllocation() {
        allocation(resident("FEMALE", "REGULAR", "ADMISSION_IN_PROGRESS"), "FUTURE");
        assertThat(revise(GenderPolicy.FEMALE, Set.of(CareLevelCode.FREQUENT)).careLevels())
                .containsExactly(CareLevelCode.FREQUENT);
    }

    @Test
    void FR_006_rejectsCareLevelViolation() {
        long id = resident("FEMALE", "SPECIAL", "IN_RESIDENCE");
        allocation(id, "ACTIVE");
        assertThatThrownBy(() -> revise(GenderPolicy.FEMALE, Set.of(CareLevelCode.BASIC)))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        e -> assertThat(e.unmetConditions().getFirst().description())
                                .contains(Long.toString(id)));
    }

    @Test
    void FR_006_admissionWithoutCurrentLevelUsesContractBeforeRegistration() {
        long id = resident("FEMALE", null, "ADMISSION_IN_PROGRESS");
        allocation(id, "FUTURE");
        residency.resident(id, ResidencyType.LONG_TERM, CareLevelCode.FREQUENT, CareLevelCode.SPECIAL, null, null);
        assertThat(revise(GenderPolicy.FEMALE, Set.of(CareLevelCode.FREQUENT)).careLevels())
                .containsExactly(CareLevelCode.FREQUENT);
    }

    @Test
    void FR_006_admissionUsesRegistrationWhenContractLevelIsMissing() {
        long id = resident("FEMALE", null, "ADMISSION_IN_PROGRESS");
        allocation(id, "FUTURE");
        residency.resident(id, ResidencyType.LONG_TERM, null, CareLevelCode.SPECIAL, null, null);
        assertThat(revise(GenderPolicy.FEMALE, Set.of(CareLevelCode.SPECIAL)).careLevels())
                .containsExactly(CareLevelCode.SPECIAL);
    }

    @Test
    void FR_007_roomWithAllocationHistoryCannotChangeFloor() {
        allocation(resident("FEMALE", "BASIC", "IN_RESIDENCE"), "ACTIVE");
        assertThatThrownBy(() -> structures.update(
                        StructureKind.ROOM,
                        room,
                        "\"0\"",
                        StructureChange.room(
                                floor + 1,
                                type,
                                "R" + room,
                                "Phòng",
                                GenderPolicy.UNRESTRICTED,
                                Set.of(CareLevelCode.BASIC))))
                .isInstanceOf(ConditionNotMetException.class);
    }

    @Test
    void FR_007_bedWithAllocationHistoryCannotChangeRoom() {
        allocation(resident("FEMALE", "BASIC", "IN_RESIDENCE"), "ACTIVE");
        var otherRoom = structures.create(
                StructureKind.ROOM,
                StructureChange.room(
                        floor,
                        type,
                        "NEW" + room,
                        "Phòng đích",
                        GenderPolicy.UNRESTRICTED,
                        Set.of(CareLevelCode.BASIC)));

        assertThatThrownBy(() -> beds.update(bed, "\"0\"", otherRoom.getId(), "G1"))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        error -> assertThat(error.unmetConditions())
                                .anySatisfy(condition ->
                                        assertThat(condition.code()).isEqualTo("FR-007")));
        assertThat(jdbc.queryForObject("SELECT room_id FROM bed WHERE id=?", Long.class, bed))
                .isEqualTo(room);
    }
}
