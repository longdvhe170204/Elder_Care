package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.TimetableApi;
import com.eldercare.backend.care.api.spi.TimetableSourcePort.TimetableItemView;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.care.FakeTimetableSource;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/** T058: thời khóa biểu theo ngày, chỉ xem, gộp công việc và nguồn 006, 012, 014 (FR-048). */
class FR_048_TimetableIT extends CareOverdueTestBase {
    @Autowired
    @Qualifier("fakeMedicationTimetable")
    FakeTimetableSource medication;

    @Autowired
    @Qualifier("fakeActivityTimetable")
    FakeTimetableSource activity;

    @Test
    void FR_048_floorManagerSeesTasksAndSourceItemsSortedByTime() throws Exception {
        long id = create();
        var taskAt = context.getBean(CareTaskStore.class).get(id, false).plannedAt();
        var date = time.today();
        medication.items(
                residentId,
                date,
                List.of(new TimetableItemView(taskAt.plusSeconds(60), "MEDICATION_DOSE", "Liều sáng", "SCHEDULED")));
        activity.items(
                residentId,
                date,
                List.of(new TimetableItemView(
                        taskAt.minusSeconds(60), "ACTIVITY_SESSION", "Buổi vận động", "REGISTERED")));
        try {
            mvc.perform(get("/api/v1/residents/" + residentId + "/timetable").param("date", date.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].source").value("ACTIVITY_SESSION"))
                    .andExpect(jsonPath("$[1].source").value("CARE_TASK"))
                    .andExpect(jsonPath("$[1].description").value("Nội dung kiểm thử"))
                    .andExpect(jsonPath("$[1].status").value("NOT_YET_DUE"))
                    .andExpect(jsonPath("$[2].source").value("MEDICATION_DOSE"));
            assertThat(context.getBean(TimetableApi.class).onDate(residentId, date))
                    .extracting(TimetableItemView::source)
                    .containsExactly("ACTIVITY_SESSION", "CARE_TASK", "MEDICATION_DOSE");
        } finally {
            medication.reset();
            activity.reset();
        }
    }

    /**
     * 014 T006: nguồn thật của 014 (không qua bản giả) trả buổi đã đăng ký và chuyến trong ngày theo mã nguồn của
     * hợp đồng 005 (`ACTIVITY_SESSION`); chuyến ghi trạng thái chuyến; buổi bị hủy (đăng ký hủy theo buổi) không hiện.
     */
    @Test
    void FR_048_activitySourceListsRegisteredSessionsAndTripsButNotCancelledSessions() throws Exception {
        var date = time.today();
        long activity = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_at,created_by)"
                        + " VALUES(?,'M04.ACTIVITY','M04','ACTIVE','{\"name\":\"Tập dưỡng sinh\"}',NOW(6),1)",
                activity);
        long session = activitySession(activity, date.atTime(9, 0), "SCHEDULED", null);
        long trip = activitySession(activity, date.atTime(13, 0), "SCHEDULED", null);
        jdbc.update(
                "UPDATE activity_session SET trip_destination='Chùa',trip_planned_departure_at=starts_at,"
                        + "trip_planned_return_at=ends_at,trip_transport='Xe viện',trip_facility_vehicle=FALSE,"
                        + "trip_status='IN_PROGRESS' WHERE id=?",
                trip);
        long cancelled = activitySession(activity, date.atTime(15, 0), "CANCELLED", "STAFF");
        registration(session, "REGISTERED", null);
        registration(trip, "REGISTERED", "DEPARTED");
        registration(cancelled, "CANCELLED", null);
        var items = context.getBean(TimetableApi.class).onDate(residentId, date).stream()
                .filter(i -> "ACTIVITY_SESSION".equals(i.source()))
                .toList();
        assertThat(items)
                .extracting(TimetableItemView::at, TimetableItemView::description, TimetableItemView::status)
                .containsExactly(
                        tuple(date.atTime(9, 0).atZone(BusinessTime.ZONE).toInstant(), "Tập dưỡng sinh", "SCHEDULED"),
                        tuple(
                                date.atTime(13, 0).atZone(BusinessTime.ZONE).toInstant(),
                                "Tập dưỡng sinh",
                                "IN_PROGRESS"));
        assertThat(context.getBean(TimetableApi.class).onDate(residentId, date.plusDays(1)))
                .noneMatch(i -> "ACTIVITY_SESSION".equals(i.source()));
    }

    long activitySession(long activity, LocalDateTime start, String status, String cancelledBy) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,floor_area_id,status,cancelled_by,"
                        + "created_at,created_by) VALUES(?,?,?,?,'SINGLE',?,?,?,NOW(6),1)",
                id,
                activity,
                start,
                start.plusHours(1),
                floor,
                status,
                cancelledBy);
        return id;
    }

    void registration(long session, String status, String participantStatus) {
        jdbc.update(
                "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status,participant_status)"
                        + " VALUES(?,?,'MANUAL',NOW(6),?,?)",
                session,
                residentId,
                status,
                participantStatus);
    }

    @Test
    void FR_048_otherDayShowsNoTask() throws Exception {
        create();
        mvc.perform(get("/api/v1/residents/" + residentId + "/timetable")
                        .param("date", time.today().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void FR_048_caregiverCannotViewTimetable() throws Exception {
        // Nhân viên chăm sóc trong phạm vi vẫn bị chặn theo vai trò (FR-048 chỉ Trưởng tầng, Điều dưỡng).
        assignments.assign(
                caregiver.staffId(),
                new com.eldercare.backend.administration.account.api.spi.AssignmentView(
                        com.eldercare.backend.administration.account.api.spi.AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        residentId,
                        shiftId,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(14400)));
        user.set(caregiver.user());
        mvc.perform(get("/api/v1/residents/" + residentId + "/timetable")
                        .param("date", time.today().toString()))
                .andExpect(status().isForbidden());
    }
}
