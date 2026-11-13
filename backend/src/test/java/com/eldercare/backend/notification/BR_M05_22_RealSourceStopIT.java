package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * 009 T017, T019 (BR-M05-22; research B-08): lượt nhắc gọi của sự cố Khẩn cấp dừng khi Bác sĩ hạ sự cố qua lệnh thật của 007
 * ({@code IncidentService.changeLevel} phát {@code CallReminderStopRequested}, {@code IncidentCallReminderListener} nghe
 * sau commit). Trước 2026-11-11 chỉ kiểm bằng sự kiện tự phát; nay chạy với publisher thật. Xác nhận lại nguyện vọng (FR-050b
 * d) chạy với lệnh thật của 007 từ 2026-11-12.
 */
class BR_M05_22_RealSourceStopIT extends CallReminderTestBase {

    @Autowired
    IncidentCatalogService catalog;

    /** Sự cố Khẩn cấp thật của 007 ở tầng của test, không gắn người cao tuổi. */
    private long criticalIncident() {
        long id = TestUsers.nextId();
        long type = catalog.incidentTypes().stream()
                .filter(t -> t.code().equals("FACILITY") && t.active())
                .findFirst()
                .orElseThrow()
                .id();
        var at = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO incident (id, incident_type_id, current_type_id, type_code, discovered_at, floor_area_id,
                  description, initial_level, level, recorded_by, created_at, confirmed_at)
                VALUES (?, ?, ?, 'FACILITY', ?, ?, 'Cháy chập điện', 'CRITICAL', 'CRITICAL', ?, ?, ?)
                """, id, type, type, at, floor, TestUsers.nextId(), at, at);
        return id;
    }

    @Test
    void BR_M05_22_doctorLowersIncidentThroughRealCommand_stopsAwaitingCallReminder() throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser callee = users.staff(Role.NURSING_STAFF);
        TestUser doctor = users.staff(Role.DOCTOR);
        duties.shiftLead(floor, lead.staffId());
        long incident = criticalIncident();
        send(urgent(
                new SourceRecord("M05.INCIDENT", Long.toString(incident)),
                "ha-muc-" + incident,
                new RecipientGroup.Staff(callee.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        List<Long> forCallee = reminders("STAFF", callee.staffId());
        assertThat(forCallee).hasSize(1);
        long reminder = forCallee.getFirst();
        assertThat(state(reminder)).containsEntry("status", "AWAITING_RESULT");

        mvc.perform(post("/api/v1/incidents/" + incident + "/change-level")
                        .header("Authorization", bearer(doctor))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLevel\":\"MEDIUM\",\"reason\":\"Đã dập tắt, không còn nguy hiểm\"}"))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(10))
                .until(() -> "STOPPED".equals(state(reminder).get("status")));
        Map<String, Object> stopped = state(reminder);
        assertThat(stopped).containsEntry("stop_reason", "SOURCE_DOWNGRADED");
    }
}
