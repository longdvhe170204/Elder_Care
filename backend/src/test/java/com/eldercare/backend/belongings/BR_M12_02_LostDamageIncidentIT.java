package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.incident.service.IncidentTypeLookup;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.incident.FakeIncidentCommands;
import com.eldercare.backend.support.fake.incident.FakeIncidents;
import com.eldercare.backend.support.fake.incident.FakeM05Config;
import java.time.Instant;
import java.util.List;
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
    void Q23_FR_021_recordDamageWithNewIncident_usesTheDamagedType() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long incident = newIncidentId();

        recordDamage(phone, location(), newIncident("LOW")).andExpect(status().isOk());

        assertThat(statusOf(phone)).isEqualTo("DAMAGED");
        assertThat(incidentOf(latestHandover(phone))).isEqualTo(incident);
        assertThat(commands.recorded().stream().filter(c -> resident == c.residentId()))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.incidentTypeId()).isEqualTo(DAMAGED_TYPE);
                    assertThat(c.level()).isEqualTo("LOW");
                });
    }

    @Test
    void Q23_FR_021_linkExistingIncident_recordsNothingNew_andStoresTheLink() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long incident = TestUsers.nextId();
        existingIncident(incident, resident, "NEW");

        reportLost(phone, linkIncident(incident)).andExpect(status().isOk());

        assertThat(incidentOf(latestHandover(phone))).isEqualTo(incident);
        assertThat(commands.recorded().stream().filter(c -> resident == c.residentId()))
                .isEmpty();
    }

    @Test
    void Q23_FR_021_noIncident_isAllowed_noIncidentRecorded_noteStillSent() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());

        reportLost(phone, ", \"incident\": {\"method\": \"NONE\"}").andExpect(status().isOk());

        long record = latestHandover(phone);
        assertThat(incidentOf(record)).isNull();
        assertThat(commands.recorded().stream().filter(c -> resident == c.residentId()))
                .isEmpty();
        assertThat(noticesOf("BELONGING_LOST", record)).isEqualTo(1);
    }

    @Test
    void FR_032_lostAndDamaged_oneMediumNoticeToPrimaryContactAndOffice() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location());
        newIncidentId();

        reportLost(phone, newIncident("MEDIUM")).andExpect(status().isOk());
        recordDamage(glasses, location(), "").andExpect(status().isOk());

        long lost = latestHandover(phone);
        long damaged = latestHandover(glasses);
        assertThat(noticesOf("BELONGING_LOST", lost)).isEqualTo(1);
        assertThat(noticesOf("BELONGING_DAMAGED", damaged)).isEqualTo(1);
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("BELONGING_LOST");
            assertThat(n.level()).contains(NotificationLevel.MEDIUM);
            assertThat(n.recipientGroups())
                    .contains(
                            new RecipientGroup.PrimaryContact(resident),
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        });
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

    @Test
    void FR_021_newIncidentNeedsALevel_andOnlyLossOrDamageCommandsCarryAnIncident() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());

        reportLost(phone, ", \"incident\": {\"method\": \"NEW\", \"description\": \"Mất\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
        handover(tokenA, command("GIVE_FOR_USE", item(phone, newIncident("LOW")), ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));

        assertThat(statusOf(phone)).isEqualTo("HELD");
        assertThat(commands.recorded().stream().filter(c -> resident == c.residentId()))
                .isEmpty();
    }

    @Test
    void FR_021_linkMustBeAnExistingIncidentOfTheSameResident_notVoided() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long other = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long ofOther = TestUsers.nextId();
        long voided = TestUsers.nextId();
        existingIncident(ofOther, other, "NEW");
        existingIncident(voided, resident, "VOIDED");

        for (long bad : List.of(ofOther, voided, TestUsers.nextId())) {
            reportLost(phone, linkIncident(bad))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INCIDENT"));
        }
        assertThat(statusOf(phone)).isEqualTo("HELD");
    }

    @Test
    void FR_024_missingPartOfCountedHandover_getsItsOwnIncident() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 6", location);
        handover(tokenA, command("GIVE_FOR_USE", item(clothes, ""), "")).andExpect(status().isOk());
        long incident = newIncidentId();

        handover(
                        tokenA,
                        command(
                                "TAKE_BACK",
                                item(clothes, ", \"countedQuantity\": 5" + newIncident("LOW")),
                                ", \"storageLocationId\": " + location + ", \"reason\": \"Đếm thiếu một bộ\""))
                .andExpect(status().isOk());

        long lostPart = sameIntake(clothes).get(1);
        assertThat(statusOf(lostPart)).isEqualTo("LOST");
        assertThat(incidentOf(latestHandover(lostPart))).isEqualTo(incident);
        assertThat(incidentOf(latestHandover(clothes))).isNull();
    }

    @Test
    void Q25_FR_024_linkLater_setsTheIncidentOnce() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        reportLost(phone, "").andExpect(status().isOk());
        long record = latestHandover(phone);
        long incident = TestUsers.nextId();
        existingIncident(incident, resident, "IN_PROGRESS");

        postJson(tokenA, "/api/v1/belongings/handovers/" + record + "/link-incident", body(incident))
                .andExpect(status().isOk());
        assertThat(incidentOf(record)).isEqualTo(incident);

        long second = TestUsers.nextId();
        existingIncident(second, resident, "NEW");
        postJson(tokenA, "/api/v1/belongings/handovers/" + record + "/link-incident", body(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_ALREADY_LINKED"));
        assertThat(incidentOf(record)).isEqualTo(incident);
    }

    @Test
    void Q25_linkLater_rejectsInvalidIncident_andRecordsThatAreNotLossOrDamage() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long other = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        reportLost(phone, "").andExpect(status().isOk());
        long record = latestHandover(phone);
        long ofOther = TestUsers.nextId();
        existingIncident(ofOther, other, "NEW");
        long intakeRecord =
                jdbc.queryForObject("SELECT MIN(id) FROM belonging_handover WHERE belonging_id = ?", Long.class, phone);
        long fine = TestUsers.nextId();
        existingIncident(fine, resident, "NEW");

        postJson(tokenA, "/api/v1/belongings/handovers/" + record + "/link-incident", body(ofOther))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INCIDENT"));
        postJson(tokenA, "/api/v1/belongings/handovers/" + intakeRecord + "/link-incident", body(fine))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("HANDOVER_NOT_LINKABLE"));
        assertThat(incidentOf(record)).isNull();
        assertThat(incidentOf(intakeRecord)).isNull();
    }

    @Test
    void Q26_FR_025_foundPartially_addsProgression_incidentStaysOpen() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 5", location);
        long incident = newIncidentId();
        reportLost(clothes, newIncident("MEDIUM")).andExpect(status().isOk());

        handover(
                        tokenA,
                        command(
                                "FOUND",
                                item(clothes, ", \"quantity\": 3"),
                                ", \"storageLocationId\": " + location + ", \"reason\": \"Tìm thấy ở phòng giặt\""))
                .andExpect(status().isOk());

        assertThat(commands.progressions()).anyMatch(p -> p.startsWith(incident + ":FOUND:"));
        assertThat(commands.progressions().stream().filter(p -> p.startsWith(incident + ":FOUND:")))
                .singleElement()
                .asString()
                .contains("đã tìm thấy 3/5");
        assertThat(commands.acknowledgedIncidents()).doesNotContain(incident);
        assertThat(statusOf(clothes)).isEqualTo("LOST");
        assertThat(quantityOf(clothes)).isEqualByComparingTo("2");
    }

    @Test
    void Q26_foundWithoutLinkedIncident_addsNoProgression() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location);
        reportLost(phone, "").andExpect(status().isOk());
        int before = commands.progressions().size();

        handover(
                        tokenA,
                        command(
                                "FOUND",
                                item(phone, ", \"photoFileIds\": [" + photo() + "]"),
                                ", \"storageLocationId\": " + location + ", \"reason\": \"Thấy trong tủ\""))
                .andExpect(status().isOk());

        assertThat(statusOf(phone)).isEqualTo("HELD");
        assertThat(commands.progressions()).hasSize(before);
    }

    @Test
    void FR_011a_voidingRecordWithIncident_notifiesIncidentHandler_doesNotCancelTheIncident() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long incident = newIncidentId();
        reportLost(phone, newIncident("MEDIUM")).andExpect(status().isOk());
        long record = latestHandover(phone);
        events.clear();

        postJson(
                        tokenA,
                        "/api/v1/corrections/M12.BELONGING_HANDOVER/" + record,
                        "{\"type\": \"VOID\", \"reason\": \"Ghi nhầm\"}")
                .andExpect(status().isCreated());

        assertThat(statusOf(phone)).isEqualTo("HELD");
        assertThat(incidentOf(record)).isEqualTo(incident);
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("BELONGING_INCIDENT_LINK_VOIDED");
            assertThat(n.level()).contains(NotificationLevel.LOW);
            assertThat(n.recipientGroups()).isNotEmpty();
        });
        assertThat(commands.acknowledgedIncidents()).doesNotContain(incident);
        assertThat(commands.progressions()).noneMatch(p -> p.startsWith(incident + ":"));
    }

    @Test
    void FR_011a_voidingRecordWithoutIncident_sendsNoIncidentNotice() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        reportLost(phone, "").andExpect(status().isOk());
        long record = latestHandover(phone);
        events.clear();

        postJson(
                        tokenA,
                        "/api/v1/corrections/M12.BELONGING_HANDOVER/" + record,
                        "{\"type\": \"VOID\", \"reason\": \"Ghi nhầm\"}")
                .andExpect(status().isCreated());

        assertThat(notices()).noneMatch(n -> "BELONGING_INCIDENT_LINK_VOIDED".equals(n.eventType()));
    }

    private static String body(long incidentId) {
        return "{\"incidentId\": " + incidentId + "}";
    }
}
