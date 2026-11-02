package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.admission.api.events.ResidencyDaysChanged;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 004 T049 (BR-M02-18, FR-061; Q-395; research T-08): lượt vắng đã đóng không sửa trực tiếp; thời điểm về, rời đi đính chính qua {@code
 * M02.ABSENCE}. Đính chính làm ngày lưu trú đọc theo giá trị đúng, 010 được báo tính lại, có nhật ký, lượt vẫn Đã đóng.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_18_CorrectionIT extends AbsenceTestBase {

    @Autowired
    ResidencyDayApi days;

    private long closedOnDay30(long residentId) throws Exception {
        clockAt(d0, 9, 0);
        long absenceId = transferOut(residentId);
        clockAt(d0.plusDays(29), 15, 0);
        recordReturn(residentId, null);
        return absenceId;
    }

    private ResultActions correct(long absenceId, Instant returnedAt) throws Exception {
        return postAs(office, "/api/v1/absences/" + absenceId + "/correct", """
                {"type":"EDIT_CONTENT","correctedContent":{"returnedAt":"%s"},"reason":"Ghi nhầm ngày về"}""".formatted(returnedAt));
    }

    private String presence(long residentId, LocalDate date) {
        return days.daysBetween(residentId, date, date).stream()
                .map(ResidencyDayView::presence)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void FR_061_aClosedAbsence_cannotBeEditedDirectly() throws Exception {
        long absenceId = closedOnDay30(inResidence());

        assertThatThrownBy(() -> jdbc.update("UPDATE absence SET reason = 'sửa' WHERE id = ?", absenceId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M02_18_correctingTheReturn_changesTheDays_notifiesBilling_andKeepsTheAbsenceClosed() throws Exception {
        long residentId = inResidence();
        long absenceId = closedOnDay30(residentId);
        clockAt(d0.plusDays(33), 9, 0);
        assertThat(presence(residentId, d0.plusDays(30))).isNotEqualTo("ABSENT");

        correct(absenceId, at(d0.plusDays(31), 15, 0)).andExpect(status().isCreated());

        assertThat(presence(residentId, d0.plusDays(30))).isEqualTo("ABSENT");
        assertThat(presence(residentId, d0.plusDays(32))).isNotEqualTo("ABSENT");
        assertThat(events.stream(ResidencyDaysChanged.class)
                        .filter(e -> e.residentId() == residentId && "ABSENCE_CORRECTION".equals(e.cause())))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ABSENCE_RECALCULATED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(absenceId)))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM absence WHERE kind = 'ABSENCE' AND id = ?", String.class, absenceId))
                .isEqualTo("CLOSED");
    }

    @Test
    void BR_M02_18_aReturnBeforeTheDeparture_isRejected() throws Exception {
        long residentId = inResidence();
        long absenceId = closedOnDay30(residentId);

        correct(absenceId, at(d0.minusDays(1), 15, 0)).andExpect(status().isUnprocessableContent());

        assertThat(events.stream(ResidencyDaysChanged.class)
                        .filter(e -> e.residentId() == residentId && "ABSENCE_CORRECTION".equals(e.cause())))
                .isEmpty();
    }
}
