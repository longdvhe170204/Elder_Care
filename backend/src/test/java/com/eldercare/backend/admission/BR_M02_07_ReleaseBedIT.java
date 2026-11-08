package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.admission.api.events.ResidencyDaysChanged;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
}
