package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.api.events.FallIncidentRecorded;
import com.eldercare.backend.incident.api.events.PostFallFollowUpRequested;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.service.PostFallMonitoringService;
import com.eldercare.backend.incident.internal.incident.service.PostFallTaskSource;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * BR-M05-07, FR-051, FR-052 (US6 #1 → #5): ghi sự cố Ngã mở một khoảng theo dõi sau ngã, phát ba tín hiệu và không tự
 * tạo công việc; ngã mới thay khoảng cũ; Bác sĩ kết thúc sớm có lý do; hết thời gian thì sang Đã hết; nguồn công việc
 * cho 005 chỉ hợp lệ trong khoảng.
 */
@RecordApplicationEvents
class BR_05_FallIT extends EmergencyTestBase {
    @Autowired
    ApplicationEvents events;

    @Autowired
    FallMonitoringStore windows;

    @Autowired
    PostFallMonitoringService monitoring;

    @Autowired
    PostFallTaskSource taskSource;

    @Test
    void FR_051_fallOpensWindowAndSignalsWithoutCreatingTasks() throws Exception {
        long incident = recordFall();

        var window = windows.active(resident).orElseThrow();
        assertThat(window.status()).isEqualTo(FallMonitoringStore.ACTIVE);
        assertThat(window.content().incidentId()).isEqualTo(incident);
        assertThat(Duration.between(window.content().anchor(), window.content().endsAt()))
                .isEqualTo(Duration.ofHours(72));
        assertThat(events.stream(FallIncidentRecorded.class).toList())
                .singleElement()
                .satisfies(event -> assertThat(event.incidentId()).isEqualTo(incident));
        assertThat(events.stream(PostFallFollowUpRequested.class).toList()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        // ngã báo đánh giá lại qua ReassessmentApi (001 FR-024, 007 FR-043); 001 tự bỏ lần gọi lại cùng sự cố
        verify(reassessments).notifyAfterIncident(resident, incident);
    }

    /** Ghi sự cố Ngã mức mặc định Khẩn cấp qua HTTP; trả mã sự cố vừa tạo. */
    private long recordFall() throws Exception {
        String body = "{\"residentId\":" + resident + ",\"incidentTypeId\":" + typeId("FALL") + ",\"discoveredAt\":\""
                + time.now()
                + "\",\"description\":\"Trượt ngã ở hành lang\",\"level\":\"CRITICAL\",\"context\":\"CARE\","
                + "\"createDespiteOpen\":true}";
        postNew(nurse, INCIDENTS, body).andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT MAX(id) FROM incident WHERE resident_id = ?", Long.class, resident);
    }
}
