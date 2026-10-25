package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cambae.backend.m03.internal.bed.domain.BedAssetPort;
import com.cambae.backend.m03.internal.bed.service.BedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** T013: vượt chuẩn vẫn tạo, lỗi ghi tăng tài sản phải rollback cả giường/lịch sử. */
class BR_M03_22_BedCreationIT extends StructureTestSupport {
    @Autowired
    BedService beds;

    @MockitoBean(name = "fakeBedAssets")
    BedAssetPort assets;

    @BeforeEach
    void resetAssetSpy() {
        reset(assets);
    }

    @Test
    void BR_M03_22_exceedingStandardBedsWarnsWithoutRejectingCreation() {
        jdbc.update("UPDATE room_type SET standard_beds=1 WHERE id=?", type);
        assertThatCode(() -> {
                    var result = beds.create(room, "G2");
                    assertThat(result.warnings())
                            .extracting(w -> w.code())
                            .containsExactly("STANDARD_BED_COUNT_EXCEEDED");
                    assertThat(jdbc.queryForObject(
                                    "SELECT state FROM bed WHERE id=?",
                                    String.class,
                                    result.bed().id()))
                            .isEqualTo("EMPTY");
                    assertThat(jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM bed_state_history WHERE bed_id=? AND event='CREATE_BED'",
                                    Integer.class,
                                    result.bed().id()))
                            .isEqualTo(1);
                    verify(assets).recordIncreaseForBed(result.bed().id());
                    var creationLogs = jdbc.queryForList(
                            "SELECT JSON_UNQUOTE(JSON_EXTRACT(data_after,'$.code')) code, JSON_UNQUOTE(JSON_EXTRACT(details,'$.warnings[0].code')) warning FROM audit_log WHERE action='M03_STRUCTURE_CREATED' AND entity_type='M03.BED' AND entity_id=?",
                            Long.toString(result.bed().id()));
                    assertThat(creationLogs).hasSize(1);
                    assertThat(creationLogs.getFirst())
                            .containsEntry("code", "G2")
                            .containsEntry("warning", "STANDARD_BED_COUNT_EXCEEDED");
                })
                .doesNotThrowAnyException();
    }

    @Test
    void Q_238_assetFailureRollsBackBedAndItsStateHistory() {
        doThrow(new IllegalStateException("Tài sản lỗi giả lập")).when(assets).recordIncreaseForBed(anyLong());
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history", Integer.class);
        int auditBefore = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);
        assertThatThrownBy(() -> beds.create(room, "ROLLBACK"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tài sản lỗi giả lập");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed WHERE room_id=? AND code='ROLLBACK'", Integer.class, room))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history", Integer.class))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class))
                .isEqualTo(auditBefore);
    }
}
