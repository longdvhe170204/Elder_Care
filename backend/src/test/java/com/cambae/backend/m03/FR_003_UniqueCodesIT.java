package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m03.internal.structure.domain.*;
import org.junit.jupiter.api.Test;

/** T011: mã khu/phòng toàn viện, tầng trong khu kể cả mục ngừng hiệu lực. */
class FR_003_UniqueCodesIT extends StructureTestSupport {
    @org.springframework.beans.factory.annotation.Autowired
    com.cambae.backend.m03.internal.bed.service.BedService beds;

    @Test
    void FR_003_roomCodeIsUniqueAcrossFloors() {
        assertThatThrownBy(() -> structures.create(
                        StructureKind.ROOM,
                        StructureChange.room(
                                floor,
                                type,
                                "R" + room,
                                "Phòng trùng mã",
                                com.cambae.backend.m03.api.dto.GenderPolicy.UNRESTRICTED,
                                null)))
                .isInstanceOfSatisfying(
                        com.cambae.backend.common.error.BusinessException.class,
                        error -> assertThat(error.status()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
    }

    @Test
    void FR_003_bedCodeIsUniqueWithinItsRoom() {
        assertThatThrownBy(() -> beds.create(room, "G1"))
                .isInstanceOfSatisfying(
                        com.cambae.backend.common.error.BusinessException.class,
                        error -> assertThat(error.status()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
    }

    @Test
    void FR_003_bedCodeCanRepeatInAnotherRoom() {
        assertThatCode(() -> {
                    var another = structures.create(
                            StructureKind.ROOM,
                            StructureChange.room(
                                    floor,
                                    type,
                                    "S" + room,
                                    "Phòng khác",
                                    com.cambae.backend.m03.api.dto.GenderPolicy.UNRESTRICTED,
                                    null));
                    var created = beds.create(another.getId(), "G1");
                    assertThat(created.bed().roomId()).isEqualTo(another.getId());
                    assertThat(created.bed().code()).isEqualTo("G1");
                })
                .doesNotThrowAnyException();
    }

    @Test
    void FR_003_areaCodeRemainsReservedWhenInactive() {
        jdbc.update("UPDATE area SET status='INACTIVE' WHERE id=?", area);
        assertThatThrownBy(() -> structures.create(StructureKind.AREA, StructureChange.area("A" + area, "Tên khác")))
                .isInstanceOf(com.cambae.backend.common.error.BusinessException.class);
    }

    @Test
    void FR_003_floorCodeIsUniqueWithinArea() {
        assertThatThrownBy(() -> structures.create(
                        StructureKind.FLOOR, StructureChange.floor(area, "F" + floor, "Tầng khác", null)))
                .isInstanceOf(com.cambae.backend.common.error.BusinessException.class);
    }
}
