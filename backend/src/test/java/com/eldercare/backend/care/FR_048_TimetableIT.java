package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.TimetableApi;
import com.eldercare.backend.care.api.spi.TimetableSourcePort.TimetableItemView;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
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
}
