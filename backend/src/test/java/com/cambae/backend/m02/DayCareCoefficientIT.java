package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.m02.api.events.DayCareAbsenceNoticeRecorded;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T050 (FR-060, CFG-M02-06; research T-08): Hành chính ghi gia đình báo vắng bán trú, hệ thống tự xác định "báo trước đủ
 * CFG-M02-06" theo giờ đến sớm nhất của ngày trên lịch đến của hợp đồng; hệ số từng buổi do 010 tra qua trạng thái có mặt
 * của 005 (đã phủ ở {@code BR_M02_06_AbsencePolicyLookupTest}).
 */
@AutoConfigureMockMvc
@WithMockUser
class DayCareCoefficientIT extends AbsenceTestBase {

    private LocalDate target;

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

    private Instant arrival() {
        return at(target, 8, 0);
    }

    private ResultActions notice(long residentId, Instant familyNotifiedAt) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/day-care-absence-notices")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"residentId":%d,"date":"%s","familyNotifiedAt":"%s"}""".formatted(residentId, target, familyNotifiedAt)));
    }

    @Test
    void FR_060_noticeGivenAtLeast24HoursBefore_isWithinTheNoticePeriod() throws Exception {
        long residentId = dayCareResident();

        notice(residentId, arrival().minus(Duration.ofHours(30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.withinNoticePeriod").value(true));
        notice(residentId, arrival().minus(Duration.ofHours(24)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.withinNoticePeriod").value(true));
        notice(residentId, arrival().minus(Duration.ofHours(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.withinNoticePeriod").value(false));

        assertThat(jdbc.queryForList(
                        "SELECT within_notice_period FROM day_care_absence_notice WHERE resident_id = ? ORDER BY id",
                        Boolean.class,
                        residentId))
                .containsExactly(true, true, false);
        assertThat(events.stream(DayCareAbsenceNoticeRecorded.class).filter(e -> e.residentId() == residentId))
                .extracting(DayCareAbsenceNoticeRecorded::withinNoticePeriod)
                .containsExactly(true, true, false);
    }

    @Test
    void CFG_M02_06_theNoticePeriodFollowsTheParameter() throws Exception {
        long residentId = dayCareResident();
        try {
            clock.reset();
            user.set(manager.user());
            String etag = mvc.perform(get("/api/v1/parameters/CFG-M02-06"))
                    .andReturn()
                    .getResponse()
                    .getHeader("ETag");
            mvc.perform(post("/api/v1/parameters/CFG-M02-06/change-value")
                            .with(csrf())
                            .header("If-Match", etag)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"value\":{\"value\":48,\"unit\":\"HOURS\"},\"reason\":\"Kéo dài cho test\"}"))
                    .andExpect(status().isOk());
            clockAt(target.minusDays(1), 23, 0);

            notice(residentId, arrival().minus(Duration.ofHours(30)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.withinNoticePeriod").value(false));
            notice(residentId, arrival().minus(Duration.ofHours(49)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.withinNoticePeriod").value(true));
        } finally {
            clock.reset();
            user.set(manager.user());
            String current = mvc.perform(get("/api/v1/parameters/CFG-M02-06"))
                    .andReturn()
                    .getResponse()
                    .getHeader("ETag");
            mvc.perform(post("/api/v1/parameters/CFG-M02-06/change-value")
                    .with(csrf())
                    .header("If-Match", current)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"restoreDefault\":true,\"reason\":\"khôi phục sau test\"}"));
            user.set(office.user());
        }
    }

    @Test
    void FR_060_aDateWithoutAnArrivalLine_isRefused_andANoticeFromTheFutureIs422() throws Exception {
        long residentId = dayCareResident();
        target = target.plusDays(1);

        notice(residentId, at(d0, 7, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_ARRIVAL_ON_DATE"));

        target = target.minusDays(1);
        notice(residentId, at(target, 23, 0)).andExpect(status().isUnprocessableContent());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM day_care_absence_notice WHERE resident_id = ?",
                        Integer.class,
                        residentId))
                .isZero();
    }

    @Test
    void FR_060_onlyOfficeStaffRecordTheNotice() throws Exception {
        long residentId = dayCareResident();
        user.set(users.staff(Role.NURSING_STAFF).user());

        mvc.perform(post("/api/v1/day-care-absence-notices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"residentId":%d,"date":"%s","familyNotifiedAt":"%s"}""".formatted(residentId, target, arrival().minus(Duration.ofHours(30)))))
                .andExpect(status().isForbidden());
    }
}
