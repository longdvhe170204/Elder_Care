package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.SwapStatisticsApi;
import com.eldercare.backend.staffing.api.dto.StaffRequestView;
import com.eldercare.backend.staffing.api.dto.WorkingHoursView;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** FR-042, FR-025: số liệu cho 016 và hết hiệu lực qua job theo phút. */
class FR_042_SwapStatisticsIT extends SwapTestBase {

    @Autowired
    SwapStatisticsApi statistics;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob minuteJob;

    private String statusOf(long requestId) {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, requestId);
    }

    @Test
    void FR_042_requestsExposeFinalStatusAndDecisionTimeWithoutReasons() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isOk());
        send("/api/v1/shift-swaps/" + id + "/approve", head, null).andExpect(status().isOk());

        List<StaffRequestView> rows = statistics
                .requests(
                        ScopeFilter.FACILITY,
                        time.today().minusDays(1),
                        time.today().plusDays(1))
                .filter(r -> r.requestId() == id)
                .toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).status()).isEqualTo("APPLIED");
        assertThat(rows.get(0).decidedAt()).isNotNull();
        assertThat(rows.get(0).floorAreaId()).isEqualTo(floor);
        assertThat(rows.get(0).toString()).doesNotContain("riêng tư");
        // Phạm vi khác không thấy.
        assertThat(statistics
                        .requests(
                                new ScopeFilter(false, java.util.Set.of(-1L), java.util.Set.of(), java.util.Set.of()),
                                time.today().minusDays(1),
                                time.today().plusDays(1))
                        .filter(r -> r.requestId() == id))
                .isEmpty();
    }

    @Test
    void FR_042_workingHoursCountScheduledMinutesOfTheMonth() {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, third);
        YearMonth month = YearMonth.from(base.atZone(BusinessTime.ZONE));
        WorkingHoursView hours = statistics
                .workingHoursByMonth(ScopeFilter.FACILITY, month)
                .filter(h -> h.staffId() == third.staffId())
                .findFirst()
                .orElseThrow();
        assertThat(hours.scheduledMinutes()).isGreaterThanOrEqualTo(8 * 60);
        assertThat(x).isPositive();
    }

    @Test
    void FR_025_openSwapExpiresWhenTheEarliestShiftStarts() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        clock.advanceBy(Duration.ofDays(4).plusMinutes(7));
        minuteJob.runNow();
        assertThat(statusOf(id)).isEqualTo("EXPIRED");
        // Ca không còn trong yêu cầu mở: khóa đã nhả, nhưng ca đã bắt đầu nên không lập mới được.
        send(
                        "/api/v1/shift-swaps",
                        giver,
                        Map.of("kind", "TAKE_OVER", "shiftXId", x, "recipientId", third.staffId(), "reason", "[Lý do]"))
                .andExpect(status().isConflict());
    }
}
