package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T057 (US7; FR-066, research T-09): lập hồ sơ kết thúc tạo bảng kỳ cuối nháp ở 010 (tác động kèm lệnh, cùng
 * transaction), đổi ngày tính lại, hủy bỏ mốc dừng tính phí. 010 lỗi thì lệnh không để lại gì.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_066_FinalPeriodIT extends EndingTestBase {

    @Test
    void FR_066_createChangeAndCancel_callTheFinalPeriodPortEachTime() throws Exception {
        long residentId = inResidence();
        LocalDate first = d0.plusDays(5);
        long id = createEnding(residentId, first);
        changeDateRaw(id, d0.plusDays(7), "Dời ngày").andExpect(status().isOk());
        changeDateRaw(id, d0.plusDays(9), "Dời tiếp").andExpect(status().isOk());
        cancelRaw(office, id, "Hủy hồ sơ").andExpect(status().isOk());

        assertThat(calls())
                .containsExactly(
                        "createDraft:" + residentId + ":" + first,
                        "recalculate:" + residentId + ":" + d0.plusDays(7),
                        "recalculate:" + residentId + ":" + d0.plusDays(9),
                        "cancelStop:" + residentId);
    }

    @Test
    void FR_066_theFinalPeriodReference_isKeptOnTheEnding() throws Exception {
        long residentId = inResidence();

        long id = createEnding(residentId, d0.plusDays(5));

        assertThat(jdbc.queryForObject("SELECT final_period_ref FROM residency_ending WHERE id = ?", Long.class, id))
                .isNotNull();
    }

    @Test
    void FR_066_whenTheFinalPeriodFails_creatingTheEndingRollsBack() throws Exception {
        long residentId = inResidence();
        finalPeriods.failOnCall();

        assertThatThrownBy(() -> createEndingRaw(office, endingBody(residentId, "DISCHARGE", d0.plusDays(5))))
                .hasRootCauseMessage("FINAL_PERIOD_FAILED");

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM residency_ending WHERE resident_id = ?", Integer.class, residentId))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ENDING_CREATED' AND resident_id = ?",
                        Integer.class,
                        residentId))
                .isZero();
    }

    @Test
    void FR_066_whenTheFinalPeriodFails_changingTheDateAndCancellingRollBack() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));
        finalPeriods.failOnCall();

        assertThatThrownBy(() -> changeDateRaw(id, d0.plusDays(8), "Dời ngày"))
                .hasRootCauseMessage("FINAL_PERIOD_FAILED");
        assertThatThrownBy(() -> cancelRaw(office, id, "Hủy")).hasRootCauseMessage("FINAL_PERIOD_FAILED");
        finalPeriods.reset();

        assertThat(ending(id).get("expectedEndDate").asString())
                .isEqualTo(d0.plusDays(5).toString());
        assertThat(endingStatus(id)).isEqualTo("PREPARING");
    }
}
