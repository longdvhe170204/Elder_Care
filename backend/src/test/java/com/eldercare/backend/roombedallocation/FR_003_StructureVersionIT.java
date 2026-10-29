package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.roombedallocation.internal.structure.domain.StructureChange;
import com.eldercare.backend.roombedallocation.internal.structure.domain.StructureKind;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** T015: If-Match và audit phần dữ liệu đã đổi, không ghi lại thuộc tính không đổi. */
class FR_003_StructureVersionIT extends StructureTestSupport {
    @Test
    void FR_003_staleVersionReturnsTheCurrentValueAndVersion() {
        assertThatCode(() -> structures.update(
                        StructureKind.AREA, area, "\"0\"", StructureChange.area("A" + area, "Tên đã đổi")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> structures.update(
                        StructureKind.AREA, area, "\"0\"", StructureChange.area("A" + area, "Dữ liệu cũ")))
                .isInstanceOfSatisfying(
                        VersionMismatchException.class,
                        error -> assertThat((Map<String, Object>) error.currentValue())
                                .containsEntry("name", "Tên đã đổi")
                                .containsEntry("version", 1));
    }

    @Test
    void FR_043a_auditContainsOnlyTheChangedBusinessAttributes() {
        assertThatCode(() -> structures.update(
                        StructureKind.AREA, area, "\"0\"", StructureChange.area("A" + area, "Tên khác")))
                .doesNotThrowAnyException();
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(data_after,'$.name')) FROM audit_log WHERE entity_type='M03.AREA' AND entity_id=? ORDER BY id DESC LIMIT 1",
                        String.class,
                        Long.toString(area)))
                .isEqualTo("Tên khác");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_LENGTH(data_after) FROM audit_log WHERE entity_type='M03.AREA' AND entity_id=? ORDER BY id DESC LIMIT 1",
                        Integer.class,
                        Long.toString(area)))
                .isEqualTo(1);
    }
}
