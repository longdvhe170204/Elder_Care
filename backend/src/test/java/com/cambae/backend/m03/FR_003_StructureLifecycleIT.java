package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.structure.domain.StructureChange;
import com.cambae.backend.m03.internal.structure.domain.StructureKind;
import com.cambae.backend.support.TestUsers;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Transactional;

/** T015: vòng đời thật của từng danh mục, bao gồm lưu dữ liệu và nhật ký cùng lệnh. */
@Transactional
class FR_003_StructureLifecycleIT extends StructureTestSupport {
    @ParameterizedTest
    @EnumSource(StructureKind.class)
    void FR_003_eachUnreferencedCatalogSupportsVersionedLifecycleAndAudit(StructureKind kind) {
        String suffix = Long.toString(TestUsers.nextId());
        String code = "C" + suffix;
        var created = structures.create(kind, change(kind, code, "Danh mục " + suffix));
        long id = created.getId();
        String table = switch (kind) {
            case AREA -> "area";
            case FLOOR -> "floor";
            case ROOM_TYPE -> "room_type";
            case ROOM -> "room";
            case BED_TRANSFER_REASON -> "bed_transfer_reason";
        };
        assertThat(jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id))
                .isEqualTo("ACTIVE");

        String revised = "Đã sửa " + suffix;
        structures.update(kind, id, "\"0\"", change(kind, code, revised));
        assertThat(jdbc.queryForObject("SELECT name FROM " + table + " WHERE id=?", String.class, id))
                .isEqualTo(revised);
        structures.deactivate(kind, id, "\"1\"", Reason.of("Ngừng danh mục kiểm thử"));
        assertThat(jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id))
                .isEqualTo("INACTIVE");
        structures.reactivate(kind, id, "\"2\"", Reason.of("Dùng lại danh mục kiểm thử"));
        assertThat(jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id))
                .isEqualTo("ACTIVE");
        structures.delete(kind, id, "\"3\"");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id=?", Integer.class, id))
                .isZero();

        String entity = kind == StructureKind.BED_TRANSFER_REASON ? "TRANSFER_REASON" : kind.name();
        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type=? AND entity_id=? ORDER BY id",
                        String.class,
                        "M03." + entity,
                        Long.toString(id)))
                .containsExactly(
                        "M03_STRUCTURE_CREATED",
                        "M03_STRUCTURE_UPDATED",
                        "M03_STRUCTURE_DEACTIVATED",
                        "M03_STRUCTURE_REACTIVATED",
                        "M03_STRUCTURE_DELETED");
    }

    private StructureChange change(StructureKind kind, String code, String name) {
        return switch (kind) {
            case AREA -> StructureChange.area(code, name);
            case FLOOR -> StructureChange.floor(area, code, name, "Tòa kiểm thử");
            case ROOM_TYPE -> StructureChange.roomType(name, 2, Set.of(CareLevelCode.BASIC), true, true);
            case ROOM ->
                StructureChange.room(floor, type, code, name, GenderPolicy.UNRESTRICTED, Set.of(CareLevelCode.BASIC));
            case BED_TRANSFER_REASON -> StructureChange.transferReason(code, name, false);
        };
    }
}
