package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.AlertView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.incident.FakeAlerts;
import com.eldercare.backend.support.fake.incident.FakeIncidentCommands;
import com.eldercare.backend.support.fake.incident.FakeIncidents;
import com.eldercare.backend.support.fake.incident.FakeM05Config;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 009 T021 phần "Xác nhận và tiếp nhận" (FR-026a, Q-102, research B-07): xác nhận ghi trước và luôn còn; tiếp nhận do 007
 * kiểm (bản giả chung {@link FakeIncidentCommands}); 007 từ chối thì 200 với {@code takeCharge.result = REJECTED} và mã
 * lỗi của 007. Nút chỉ hiện ({@code canAcknowledgeAndTakeCharge}) khi nguồn là cảnh báo, sự cố còn tiếp nhận được và
 * người nhận có quyền xử lý.
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, FakeM05Config.class})
class FR_026a_AcknowledgeAndTakeChargeIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeIncidentCommands commands;

    @Autowired
    FakeAlerts alerts;

    @Autowired
    FakeIncidents incidents;

    TestUsers users;
    TestUser doctor;
    String token;
    long residentId;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        doctor = users.staff(Role.DOCTOR);
        token = TestUsers.bearer(users.session(doctor.accountId()));
        residentId = TestUsers.nextId();
    }

    private long send(TestUser to, String sourceType, String sourceId, NotificationLevel level) {
        String key = sourceType + "-" + sourceId + "-" + to.staffId();
        NotificationRequest r = new NotificationRequest(
                "M05",
                "ALERT_URGENT",
                SourceGroup.OTHER,
                new SourceRecord(sourceType, sourceId),
                key,
                Optional.of(level),
                Optional.empty(),
                Optional.empty(),
                List.of(new RecipientGroup.Staff(to.staffId(), "X")),
                List.of(new ContentPart(InformationType.GENERAL, "Chỉ số vượt ngưỡng")),
                Optional.of("Cảnh báo khẩn cấp"),
                Optional.of("Cảnh báo"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        long requestId = ((Number) awaitRequests(key, 1).getFirst().get("id")).longValue();
        return jdbc.queryForObject("SELECT id FROM notification WHERE request_id = ?", Long.class, requestId);
    }

    private long alert(String status) {
        long id = TestUsers.nextId();
        alerts.put(new AlertView(id, residentId, "VITAL_SIGN", "URGENT", status, time.now(), null));
        return id;
    }

    private ResultActions takeCharge(long id, String bearer) throws Exception {
        return mvc.perform(post("/api/v1/notifications/" + id + "/acknowledge-and-take-charge")
                .header("Authorization", bearer)
                .header("Idempotency-Key", UUID.randomUUID().toString()));
    }

    private String notificationStatus(long id) {
        return jdbc.queryForObject("SELECT status FROM notification WHERE id = ?", String.class, id);
    }

    @Test
    void FR_026a_alert_acknowledgedThenTakenCharge_twoRecords() throws Exception {
        long alertId = alert("NEW");
        long id = send(doctor, "M05.ALERT", Long.toString(alertId), NotificationLevel.URGENT);

        mvc.perform(get("/api/v1/me/notifications/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canAcknowledgeAndTakeCharge").value(true));
        mvc.perform(get("/api/v1/me/notifications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")].canAcknowledgeAndTakeCharge")
                        .value(true));
        takeCharge(id, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledgement").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.takeCharge.result").value("SUCCEEDED"));

        assertThat(notificationStatus(id)).isEqualTo("ACKNOWLEDGED");
        assertThat(commands.acknowledgedAlerts()).contains(alertId);
        assertThat(commands.acknowledgedIncidents()).doesNotContain(alertId);
    }
}
