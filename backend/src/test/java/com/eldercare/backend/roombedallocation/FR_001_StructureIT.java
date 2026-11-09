package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.roombedallocation.api.dto.GenderPolicy;
import com.eldercare.backend.roombedallocation.internal.structure.domain.*;
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
                    assertThat(r.careLevels())
                            .containsExactly(com.eldercare.backend.roombedallocation.api.spi.CareLevelCode.BASIC);
                    assertThat(jdbc.queryForObject("SELECT status FROM room WHERE id=?", String.class, r.getId()))
                            .isEqualTo("ACTIVE");
                })
                .doesNotThrowAnyException();
    }
}
