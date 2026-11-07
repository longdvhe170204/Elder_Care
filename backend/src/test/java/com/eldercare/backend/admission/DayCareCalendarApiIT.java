package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.dto.DayCareAbsenceNoticeView;
import com.eldercare.backend.admission.api.dto.DayCareCalendarView;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004-3 (A duyệt 2026-11-05; FR-060, FR-060a; de-xuat-004-api-lich-ban-tru): {@code DayCareCalendarApi.onDate} trả ngày khu
 * bán trú nghỉ Hiệu lực và mọi báo vắng đã ghi của người trong ngày, theo thời điểm ghi, kèm cờ báo đủ hạn.
 */
@AutoConfigureMockMvc
@WithMockUser
class DayCareCalendarApiIT extends AbsenceTestBase {

    @Autowired
    DayCareCalendarApi calendar;

    private LocalDate target;

    @BeforeEach
    @AfterEach
    void cleanClosures() throws SQLException {
        WaitingListTestBase.admin(
                "UPDATE absence SET status = 'CANCELLED', locked_at = NOW(6) WHERE kind = 'CLOSURE_DAY' AND locked_at IS NULL");
    }

    private long dayCareResident() throws Exception {
        long residentId = residentWithRepresentative();
        target = d0.plusDays(3);
        ObjectNode body = body(residentId);
        body.put("residenceType", "DAY_CARE");
        body.remove("roomTypeId");
        body.put("residencyFeeBillingPeriod", "SESSION");
        body.putArray("arrivalSchedule")
                .addObject()
                .put("dayOfWeek", target.getDayOfWeek().getValue())
                .put("arrivalTime", "08:00")
                .put("departureTime", "16:00");
        activeContract(residentId, body);
        setStatus(residentId, "IN_RESIDENCE");
        user.set(office.user());
        clockAt(target.minusDays(1), 23, 0);
        return residentId;
    }

    private void notice(long residentId, Instant familyNotifiedAt) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/day-care-absence-notices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"residentId":%d,"date":"%s","familyNotifiedAt":"%s"}""".formatted(residentId, target, familyNotifiedAt)))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_060_onDate_returnsEveryNoticeOfTheDay_oldestFirst_withTheNoticePeriodFlag() throws Exception {
        long residentId = dayCareResident();
        Instant arrival = at(target, 8, 0);
        notice(residentId, arrival.minus(Duration.ofHours(30)));
        notice(residentId, arrival.minus(Duration.ofHours(10)));

        DayCareCalendarView view = calendar.onDate(residentId, target);

        assertThat(view.date()).isEqualTo(target);
        assertThat(view.closureDay()).isFalse();
        assertThat(view.notices())
                .extracting(DayCareAbsenceNoticeView::withinNoticePeriod)
                .containsExactly(true, false);
        assertThat(view.notices().get(0).noticeId())
                .isLessThan(view.notices().get(1).noticeId());
        assertThat(view.notices().get(0).sessionId()).isNull();
        assertThat(view.notices().get(0).familyNotifiedAt()).isEqualTo(arrival.minus(Duration.ofHours(30)));
    }

    @Test
    void FR_060a_onDate_showsTheActiveClosureDay_andOnlyForThatDate() throws Exception {
        long residentId = dayCareResident();
        user.set(office.user());
        mvc.perform(post("/api/v1/day-care-closure-days")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + target + "\",\"reason\":\"Sửa chữa khu bán trú\"}"))
                .andExpect(status().isCreated());

        assertThat(calendar.onDate(residentId, target).closureDay()).isTrue();
        assertThat(calendar.onDate(residentId, target).notices()).isEmpty();
        assertThat(calendar.onDate(residentId, target.plusDays(1)).closureDay()).isFalse();
    }
}
