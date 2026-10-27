package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m02.api.ResidencyDayApi;
import com.cambae.backend.m02.api.dto.Attendance;
import com.cambae.backend.m02.api.dto.DayCareSessionView;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * Audit Feature 004 (FR-060, FR-060a; contracts mục 1.4): {@code ResidencyDayApi.dayCareSessions} trả mỗi buổi bán trú có
 * lịch với hệ số theo trạng thái có mặt do 005 cung cấp. Hệ số theo bảng CFG-M02-05 mặc định (V000_04): Có mặt 100%, Vắng
 * có báo 0%, Vắng không báo 50%; buổi chưa xác định chưa có hệ số; ngày khu bán trú nghỉ không có hệ số. {@code sessionId}
 * hiện là số thứ tự buổi trong ngày theo giờ đến; mã buổi thật chờ 005 (blocker chờ C).
 */
@AutoConfigureMockMvc
@WithMockUser
class DayCareSessionsIT extends AbsenceTestBase {

    @Autowired
    ResidencyDayApi days;

    private LocalDate target;

    @BeforeEach
    void clean() throws SQLException {
        WaitingListTestBase.admin("UPDATE day_care_closure_day SET status = 'CANCELLED'");
    }

    @AfterEach
    void cleanAfter() throws SQLException {
        WaitingListTestBase.admin("UPDATE day_care_closure_day SET status = 'CANCELLED'");
    }

    /** Người bán trú, hợp đồng Hiệu lực hôm nay, hai buổi trong ngày {@code target} (08:00 và 13:00). */
    private long dayCareResident() throws Exception {
        long residentId = residentWithRepresentative();
        target = d0.plusDays(3);
        ObjectNode body = body(residentId);
        body.put("residenceType", "DAY_CARE");
        body.remove("roomTypeId");
        body.put("residencyFeeBillingPeriod", "SESSION");
        var schedule = body.putArray("arrivalSchedule");
        schedule.addObject()
                .put("dayOfWeek", target.getDayOfWeek().getValue())
                .put("arrivalTime", "08:00")
                .put("departureTime", "11:00");
        schedule.addObject()
                .put("dayOfWeek", target.getDayOfWeek().getValue())
                .put("arrivalTime", "13:00")
                .put("departureTime", "16:00");
        activeContract(residentId, body);
        setStatus(residentId, "IN_RESIDENCE");
        user.set(office.user());
        return residentId;
    }

    private List<DayCareSessionView> sessions(long residentId) {
        return days.dayCareSessions(residentId, target, target);
    }

    @Test
    void FR_060_eachScheduledSession_getsTheCoefficientOfItsAttendance() throws Exception {
        long residentId = dayCareResident();
        attendance.attendance(residentId, target, 1, Attendance.PRESENT);
        attendance.attendance(residentId, target, 2, Attendance.ABSENT_NOTIFIED);

        List<DayCareSessionView> list = sessions(residentId);

        assertThat(list).hasSize(2);
        assertThat(list).extracting(DayCareSessionView::sessionId).containsExactly(1L, 2L);
        assertThat(list.get(0).attendance()).isEqualTo(Attendance.PRESENT);
        assertThat(list.get(0).coefficientPercent()).isEqualTo(100);
        assertThat(list.get(1).attendance()).isEqualTo(Attendance.ABSENT_NOTIFIED);
        assertThat(list.get(1).coefficientPercent()).isZero();
        assertThat(list).noneMatch(DayCareSessionView::closureDay);
    }

    @Test
    void FR_060_absentWithoutNotice_isHalfFee() throws Exception {
        long residentId = dayCareResident();
        attendance.attendance(residentId, target, 1, Attendance.ABSENT_UNNOTIFIED);

        assertThat(sessions(residentId).getFirst().coefficientPercent()).isEqualTo(50);
    }

    @Test
    void FR_060_aSessionNotYetDetermined_hasNoCoefficient() throws Exception {
        long residentId = dayCareResident();

        List<DayCareSessionView> list = sessions(residentId);

        assertThat(list).hasSize(2);
        assertThat(list).allSatisfy(session -> {
            assertThat(session.attendance()).isNull();
            assertThat(session.coefficientPercent()).isNull();
        });
    }

    @Test
    void FR_060a_aClosureDay_isFlagged_andHasNoCoefficientEvenWithAnAttendance() throws Exception {
        long residentId = dayCareResident();
        attendance.attendance(residentId, target, 1, Attendance.PRESENT);
        user.set(office.user());
        mvc.perform(post("/api/v1/day-care-closure-days")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + target + "\",\"reason\":\"Sửa chữa khu bán trú\"}"))
                .andExpect(status().isCreated());

        List<DayCareSessionView> list = sessions(residentId);

        assertThat(list).hasSize(2);
        assertThat(list).allSatisfy(session -> {
            assertThat(session.closureDay()).isTrue();
            assertThat(session.coefficientPercent()).isNull();
        });
    }

    @Test
    void aDateWithoutASchedule_hasNoSession() throws Exception {
        long residentId = dayCareResident();

        assertThat(days.dayCareSessions(residentId, target.plusDays(1), target.plusDays(1)))
                .isEmpty();
    }

    @Test
    void sessionsAfterTheBillingStopDate_areNotReturned() throws Exception {
        long residentId = dayCareResident();
        WaitingListTestBase.admin("UPDATE contract SET status = 'TERMINATED', termination_reason = 'Hủy tiếp nhận',"
                + " terminated_on = '" + target.minusDays(1) + "' WHERE resident_id = " + residentId);
        attendance.attendance(residentId, target, 1, Attendance.PRESENT);

        assertThat(sessions(residentId)).isEmpty();
    }
}
