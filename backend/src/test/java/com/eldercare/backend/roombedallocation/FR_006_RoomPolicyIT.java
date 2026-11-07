package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.roombedallocation.api.dto.GenderPolicy;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.internal.structure.domain.*;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** T012: kiểm chính sách với API thật M01 và nguồn lưu trú đúng chủ khai báo. */
@Transactional
class FR_006_RoomPolicyIT extends StructureTestSupport {
    @Autowired
    com.eldercare.backend.roombedallocation.internal.bed.service.BedService beds;

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
}
