package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.admission.api.events.ResidencyDaysChanged;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 004 T049, T052, T054 (FR-050, FR-052, FR-055, BR-M02-06, BR-M02-07; Q-345, Q-395): vắng tính như ngày lưu trú, giữ giường không hạn;
 * tới ngày CFG-M02-05 báo Quản lý viện đúng một lần, không tạo yêu cầu; Quản lý viện giải phóng giường có lý do, từ đó ngày vắng không
 * tính phí lưu trú. {@code BedAllocationCommandApi} là của 003, chưa có bản cài production nên dùng mock.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_07_ReleaseBedIT extends AbsenceTestBase {

    @MockitoBean
    BedAllocationCommandApi bedCommands;

    @Autowired
    ResidencyDayApi days;

    @Autowired
    com.eldercare.backend.admission.internal.absence.service.AbsenceTransition absenceTransition;

    private long absent(long residentId) {
        clockAt(d0, 10, 0);
        return grant(residentId, "HOME_VISIT", at(d0.plusDays(60), 10, 0));
    }

    private ResidencyDayView on(List<ResidencyDayView> list, LocalDate date) {
        return list.stream().filter(d -> d.date().equals(date)).findFirst().orElseThrow();
    }

    @Test
    void BR_M02_06_absentDays_areBilledAsResidencyDays_withoutCoefficient() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);
        clockAt(d0.plusDays(2), 12, 0);

        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(2));

        assertThat(list).hasSize(3).allSatisfy(day -> {
            assertThat(day.presence()).isEqualTo("ABSENT");
            assertThat(day.absenceId()).isEqualTo(absenceId);
            assertThat(day.bedReleased()).isFalse();
        });
        assertThat(detail(absenceId).get("holdBed").asBoolean()).isTrue();
    }

    @Test
    void BR_M02_07_managerIsNotifiedOnce_onDayThirty_andNoRequestIsCreated() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);
        int requestsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class);

        runJobOn(d0.plusDays(28));
        assertThat(managerNotices(absenceId)).as("ngày 29: chưa báo").isEmpty();

        runJobOn(d0.plusDays(29));
        assertThat(managerNotices(absenceId)).as("ngày 30: báo một lần").hasSize(1);
        assertThat(managerNotices(absenceId).getFirst().absentDays()).isEqualTo(30);

        runJobOn(d0.plusDays(31));
        assertThat(managerNotices(absenceId)).as("không báo lại").hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class))
                .as("Q-307: không tạo yêu cầu")
                .isEqualTo(requestsBefore);
    }

    @Test
    void FR_055_releaseBed_isManagerOnly_reasonRequired() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);

        postAs(office, "/api/v1/absences/" + absenceId + "/release-bed", "{\"reason\":\"Thử\"}")
                .andExpect(status().isForbidden());
        releaseBed(absenceId, "  ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));

        verify(bedCommands, never()).releaseWhenAbsent(eq(residentId), any(), any());
        assertThat(detail(absenceId).get("holdBed").asBoolean()).isTrue();
    }

    @Test
    void FR_055_releaseBed_stopsResidencyFeeFromThatDay_andNotifiesBilling() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);
        clockAt(d0.plusDays(5), 9, 0);

        releaseBed(absenceId, "Vắng dài, gia đình đồng ý trả giường")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdBed").value(false));

        verify(bedCommands).releaseWhenAbsent(eq(residentId), any(), any());
        clockAt(d0.plusDays(6), 12, 0);
        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(6));
        assertThat(on(list, d0.plusDays(4)).bedReleased()).isFalse();
        assertThat(on(list, d0.plusDays(5)).bedReleased()).isTrue();
        assertThat(on(list, d0.plusDays(6)).bedReleased()).isTrue();
        assertThat(events.stream(ResidencyDaysChanged.class)
                        .filter(e -> e.residentId() == residentId && "BED_RELEASED".equals(e.cause())))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.ABSENCE' AND entity_id = ?"
                                + " AND action = 'M02_ABSENCE_BED_RELEASED' AND reason IS NOT NULL",
                        Integer.class,
                        String.valueOf(absenceId)))
                .isEqualTo(1);
    }

    @Test
    void FR_055_releaseTwice_is409_andClosedAbsence_is409() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);
        releaseBed(absenceId, "Lần một").andExpect(status().isOk());

        releaseBed(absenceId, "Lần hai")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BED_ALREADY_RELEASED"));

        recordReturn(residentId, null);
        releaseBed(absenceId, "Sau khi về")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ABSENCE_CLOSED"));
    }

    @Test
    void FR_059_returningAfterRelease_warnsThatANewBedIsNeeded() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);
        releaseBed(absenceId, "Giải phóng").andExpect(status().isOk());

        assertThat(absenceTransition.returnWarnings(residentId))
                .extracting(w -> w.code())
                .contains("BED_REALLOCATION_NEEDED");
    }

    @Test
    void FR_055_bedCommandFailure_rollsBackTheRelease() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);
        doThrow(new BusinessException(HttpStatus.CONFLICT, "BED_STATE", "x"))
                .when(bedCommands)
                .releaseWhenAbsent(eq(residentId), any(), any());

        releaseBed(absenceId, "Thử lỗi").andExpect(status().isConflict());

        assertThat(detail(absenceId).get("holdBed").asBoolean()).isTrue();
    }
}
