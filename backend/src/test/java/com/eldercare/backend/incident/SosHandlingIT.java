package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.EmergencyIncidentActivated;
import com.eldercare.backend.incident.internal.alert.service.AlertEscalationService;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * US12 #7 → #10, FR-047g (T071): cảnh báo SOS xử lý như mọi cảnh báo theo FR-029. Đóng kèm kết quả không báo người thân;
 * kích hoạt khẩn cấp từ cảnh báo SOS chạy FR-047 (có người liên hệ chính); leo thang như mọi cảnh báo Khẩn cấp (FR-031); không
 * có lệnh tắt nút SOS.
 */
class SosHandlingIT extends SosTestBase {

    @Autowired
    AlertEscalationService escalation;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void US12_7_closeWithResultDoesNotNotifyFamily() throws Exception {
        press("k1").andExpect(status().isCreated());
        long alertId = sosAlert();
        post(nurse, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/start-handling", alertId, "{}").andExpect(status().isOk());

        post(nurse, "/api/v1/alerts/{id}/close", alertId, "{\"resolution\":\"Hỗ trợ đứng dậy, không bị thương\"}")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, alertId))
                .isEqualTo("CLOSED");
        assertThat(allRequests())
                .flatMap(request -> request.recipientGroups())
                .noneMatch(group -> group instanceof RecipientGroup.PrimaryContact
                        || group instanceof RecipientGroup.Representative
                        || group instanceof RecipientGroup.FamilyWithRight);
    }

    @Test
    void US12_8_activateEmergencyFromSosAlertNotifiesPrimaryContact() throws Exception {
        press("k1").andExpect(status().isCreated());
        long alertId = sosAlert();

        post(
                        nurse,
                        "/api/v1/alerts/{id}/activate-emergency",
                        alertId,
                        "{\"incidentTypeId\":" + typeId("FALL") + ",\"description\":\"Ngã trong nhà vệ sinh\"}")
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, alertId))
                .isEqualTo("CONVERTED_TO_INCIDENT");
        assertThat(events.stream(EmergencyIncidentActivated.class)).singleElement();
        assertThat(allRequests())
                .flatMap(request -> request.recipientGroups())
                .contains(new RecipientGroup.PrimaryContact(resident));
    }

    @Test
    void US12_9_sosAlertFollowsCriticalAlertRules() throws Exception {
        press("k1").andExpect(status().isCreated());
        long alertId = sosAlert();
        var deadline = jdbc.queryForObject(
                "SELECT acknowledgement_deadline FROM alert WHERE id = ?", java.sql.Timestamp.class, alertId);
        // FR-031: Khẩn cấp không có hạn tiếp nhận, không leo thang theo cấp (nhắc theo BR-M13-02 của 009); US12 #9 ghi
        // "leo thang theo BR-M05-01 như mọi cảnh báo khác", lệch FR-031, đã báo chủ dự án (team-plan mục 8).
        assertThat(deadline).isNull();
        assertThat(escalation.escalate(alertId, time.now().plusSeconds(3_600))).isFalse();
        assertThat(jdbc.queryForObject("SELECT escalation_level FROM alert WHERE id = ?", Integer.class, alertId))
                .isZero();
    }

    @Test
    void FR_047g_noCommandDisablesSosButton() {
        Set<String> sosRoutes = mappings.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()
                        .filter(path -> path.startsWith("/api/v1/sos"))
                        .flatMap(path ->
                                info.getMethodsCondition().getMethods().stream().map(method -> method + " " + path)))
                .collect(Collectors.toSet());

        assertThat(sosRoutes)
                .isNotEmpty()
                .isSubsetOf(
                        "POST /api/v1/sos",
                        "GET /api/v1/sos/current",
                        "POST /api/v1/sos/{id}/place",
                        "POST /api/v1/sos/{id}/location");
    }
}
