package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.internal.structure.domain.*;
import com.cambae.backend.support.TestUsers;
import org.junit.jupiter.api.Test;

/** T011: dựng cơ cấu, sao chính sách loại phòng và quyền thật Phụ lục 27. */
class FR_001_StructureIT extends StructureTestSupport {
    @Test
    void US1_1_managerCreatesHierarchyAndRoomCopiesDefaultLevels() {
        assertThatCode(() -> {
                    var a = structures.create(StructureKind.AREA, StructureChange.area("N" + area, "Khu mới"));
                    var f = structures.create(
                            StructureKind.FLOOR, StructureChange.floor(a.getId(), "F1", "Tầng 1", null));
                    var r = (Room) structures.create(
                            StructureKind.ROOM,
                            StructureChange.room(f.getId(), type, "N" + room, "Phòng mới", GenderPolicy.FEMALE, null));
                    assertThat(r.floorId()).isEqualTo(f.getId());
                    assertThat(r.careLevels()).containsExactly(com.cambae.backend.m03.api.spi.CareLevelCode.BASIC);
                    assertThat(jdbc.queryForObject("SELECT status FROM room WHERE id=?", String.class, r.getId()))
                            .isEqualTo("ACTIVE");
                })
                .doesNotThrowAnyException();
    }

    @Test
    void FR_002_floorManagerCanReadButCannotChangeStructure() {
        user.set(new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER).user());
        assertThatCode(() -> assertThat(structures.list(StructureKind.AREA)).isNotEmpty())
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> structures.create(StructureKind.AREA, StructureChange.area("X" + area, "Cấm tạo")))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_004_deactivatedRoomTypeStaysOnExistingRoomButCannotBeChosenAgain() {
        assertThatCode(() -> {
                    structures.deactivate(
                            StructureKind.ROOM_TYPE, type, "\"0\"", com.cambae.backend.common.Reason.of("Ngừng loại"));
                    assertThat(structures.detail(StructureKind.ROOM, room)).isInstanceOf(Room.class);
                })
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> structures.create(
                        StructureKind.ROOM,
                        StructureChange.room(floor, type, "NEW" + room, "Phòng mới", GenderPolicy.FEMALE, null)))
                .isInstanceOf(com.cambae.backend.common.error.ConditionNotMetException.class);
    }
}
