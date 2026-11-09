package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T069 (contracts mục 1.4, 1.5a; FR-052, FR-066, FR-070, FR-074, FR-075a): {@code ResidencyDayApi} (tên logic {@code
 * NgayLuuTruApi}) trả mỗi ngày nội trú của hợp đồng đã ký với dữ liệu ngày vắng, các dấu và mốc dừng tính phí;
 * {@code ResidencyStatisticsApi} (tên logic {@code LuuTruThongKeApi}) trả loại lưu trú tại ngày cho 016.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidencyDayApiIT extends DeathTestBase {

    @Autowired
    ResidencyDayApi days;

    @Autowired
    ResidencyStatisticsApi statistics;

    private ResidencyDayView on(List<ResidencyDayView> list, LocalDate date) {
        return list.stream().filter(d -> d.date().equals(date)).findFirst().orElseThrow();
    }

    @Test
    void FR_052_everyResidentialDayOfTheSignedContract_isReturned_withTheAbsenceDaysMarked() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(4), 17, 0));

        List<ResidencyDayView> list = days.daysBetween(residentId, d0.minusDays(1), d0.plusDays(3));

        assertThat(list)
                .as("ngày trước ngày bắt đầu hợp đồng không có")
                .extracting(ResidencyDayView::date)
                .first()
                .isEqualTo(d0);
        assertThat(list).hasSize(4);
        assertThat(on(list, d0).presence()).isEqualTo("ABSENT");
        assertThat(on(list, d0).absenceId()).isEqualTo(absenceId);
        assertThat(on(list, d0).bedReleased())
                .as("Q-395: tính như ngày lưu trú")
                .isFalse();
        assertThat(on(list, d0.plusDays(3)).presence()).isEqualTo("PRESENT");
        assertThat(on(list, d0.plusDays(3)).absenceId()).isNull();
        assertThat(list).allSatisfy(d -> {
            assertThat(d.contractId()).isNotNull();
            assertThat(d.outsideBillingPeriod()).isFalse();
            assertThat(d.contractOverdue()).isFalse();
        });
    }

    private static String admittedOnSql(long residentId, LocalDate date) {
        return "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at) VALUES ("
                + residentId + ", 'M02', '{\"admittedOn\":\"" + date
                + "\"}', 0, UTC_TIMESTAMP(6)) AS new ON DUPLICATE KEY UPDATE data = new.data";
    }
}
