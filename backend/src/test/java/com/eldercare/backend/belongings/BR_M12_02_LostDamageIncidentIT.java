package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.incident.service.IncidentTypeLookup;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.incident.FakeIncidentCommands;
import com.eldercare.backend.support.fake.incident.FakeIncidents;
import com.eldercare.backend.support.fake.incident.FakeM05Config;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 013 T020 (US4; BR-M12-02, FR-020 → FR-025, Q-307, SC-012 phần thông báo; quickstart Q23 → Q26): thất lạc, hư hỏng,
 * tìm thấy kèm sự cố do người ghi chọn (NEW, LINK, NONE); mỗi lệnh đúng một thông báo (Trung bình cho thất lạc, hư hỏng;
 * Nhẹ cho tìm thấy); lỗi 007 thì cả lệnh không lưu; liên kết sau một lần; Tìm thấy ghi diễn biến, không đóng sự cố; Hủy
 * ghi nhận bản ghi có sự cố báo người xử lý, không hủy sự cố. Bản giả của 007 dùng chung ({@code FakeM05Config}).
 */
@Import({FakeM05Config.class, BR_M12_02_LostDamageIncidentIT.Types.class})
class BR_M12_02_LostDamageIncidentIT extends HandoverTestBase {

    private static final long LOST_TYPE = 9_001L;
    private static final long DAMAGED_TYPE = 9_002L;

    /** Tra mã loại sự cố của 007 (bản thật do D cung cấp, plan 013 điểm 14); bật {@code failing} để mô phỏng lỗi 007. */
    static class SwitchableTypes implements IncidentTypeLookup {
        volatile boolean failing;

        @Override
        public long idOf(String typeCode) {
            if (failing) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "007 lỗi");
            }
            return switch (typeCode) {
                case "BELONGINGS_LOST" -> LOST_TYPE;
                case "BELONGINGS_DAMAGED" -> DAMAGED_TYPE;
                default -> throw new IllegalArgumentException(typeCode);
            };
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Types {
        @Bean
        @Primary
        SwitchableTypes switchableIncidentTypes() {
            return new SwitchableTypes();
        }
    }

    @Autowired
    SwitchableTypes types;

    @Autowired
    FakeIncidentCommands commands;

    @Autowired
    FakeIncidents incidents;

    @AfterEach
    void resetTypes() {
        types.failing = false;
    }

    private long newIncidentId() {
        long id = TestUsers.nextId();
        commands.nextId(id);
        return id;
    }

    private void existingIncident(long id, long resident, String status) {
        incidents.put(new IncidentView(id, resident, "BELONGINGS_LOST", "MEDIUM", status, Instant.now(), null));
    }

    private static String newIncident(String level) {
        return ", \"incident\": {\"method\": \"NEW\", \"level\": \"" + level
                + "\", \"description\": \"Mất điện thoại\", \"place\": \"Phòng sinh hoạt\"}";
    }

    private static String linkIncident(long incidentId) {
        return ", \"incident\": {\"method\": \"LINK\", \"incidentId\": " + incidentId + "}";
    }

    private Long incidentOf(long handoverId) {
        return jdbc.queryForObject("SELECT incident_id FROM belonging_handover WHERE id = ?", Long.class, handoverId);
    }

    private long handoverRows(long belongingId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ?", Long.class, belongingId);
    }

    private ResultActions reportLost(long belongingId, String incident) throws Exception {
        return handover(
                tokenA, command("REPORT_LOST", item(belongingId, incident), ", \"reason\": \"Không thấy điện thoại\""));
    }

    private ResultActions recordDamage(long belongingId, long location, String incident) throws Exception {
        return handover(
                tokenA,
                command(
                        "RECORD_DAMAGE",
                        item(belongingId, ", \"photoFileIds\": [" + photo() + "]" + incident),
                        ", \"storageLocationId\": " + location + ", \"reason\": \"Vỡ màn hình\""));
    }

    private long noticesOf(String eventType, long handoverId) {
        return notices().stream()
                .filter(n -> eventType.equals(n.eventType())
                        && Long.toString(handoverId).equals(n.sourceRecord().id()))
                .count();
    }

    @Test
    void Q23_FR_021_reportLostWithNewIncident_recordsItInTheSameCommand_andLinksIt() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long incident = newIncidentId();

        reportLost(phone, newIncident("MEDIUM")).andExpect(status().isOk());

        long record = latestHandover(phone);
        assertThat(statusOf(phone)).isEqualTo("LOST");
        assertThat(incidentOf(record)).isEqualTo(incident);
        var recorded = commands.recorded().stream()
                .filter(c -> resident == c.residentId())
                .toList();
        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().incidentTypeId()).isEqualTo(LOST_TYPE);
        assertThat(recorded.getFirst().source()).isEqualTo("BELONGINGS");
        assertThat(recorded.getFirst().level()).isEqualTo("MEDIUM");
        assertThat(recorded.getFirst().description()).isEqualTo("Mất điện thoại");
        assertThat(recorded.getFirst().location()).isEqualTo("Phòng sinh hoạt");
    }

    @Test
    void Q24_FR_021_incidentFailure_rollsBackTheWholeCommand() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long rowsBefore = handoverRows(phone);
        int versionBefore = version(phone);
        events.clear();
        types.failing = true;

        reportLost(phone, newIncident("MEDIUM")).andExpect(status().isServiceUnavailable());

        assertThat(statusOf(phone)).isEqualTo("HELD");
        assertThat(version(phone)).isEqualTo(versionBefore);
        assertThat(handoverRows(phone)).isEqualTo(rowsBefore);
        assertThat(notices()).noneMatch(n -> "BELONGING_LOST".equals(n.eventType()));
    }

    private static String body(long incidentId) {
        return "{\"incidentId\": " + incidentId + "}";
    }
}
