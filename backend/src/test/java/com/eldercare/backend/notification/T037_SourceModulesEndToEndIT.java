package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.administration.account.api.events.PermissionDataError;
import com.eldercare.backend.admission.api.events.ContractOverdueEscalated;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ActivityAttendanceReminder;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.eldercare.backend.incident.api.IncidentTypeApi;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.medication.api.events.FloorWithoutNurse;
import com.eldercare.backend.resident.api.events.MissingConsentReminder;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.events.BedReadinessReminder;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentReminders;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * 009 T037 (quickstart mục 3, NFR-03, SC-001): kiểm thử đầu–cuối với sự kiện thật của các feature 001 → 008, mỗi feature
 * một sự kiện, qua listener {@code HasNotificationRequests} duy nhất của 009 (FR-001); 007 đi trọn lệnh ghi nhận sự cố
 * Khẩn cấp ({@code IncidentCommandApi.record}) tới lần đẩy FCM (giả) cho Quản lý viện, đo thời gian ≤ 10 giây (NFR-03).
 * Các sự kiện khác dựng bằng record sự kiện công bố của module nguồn, yêu cầu thông báo do chính module nguồn dựng; 008 gọi
 * bean nhắc phân công thật.
 */
@Import({T037_SourceModulesEndToEndIT.PushConfig.class, SwitchableUserConfig.class})
class T037_SourceModulesEndToEndIT extends M13IntegrationTestBase {

    @TestConfiguration
    static class PushConfig {
        @Bean
        @Primary
        FakePushGateway fakePushGateway() {
            return new FakePushGateway();
        }
    }

    @Autowired
    FakePushGateway push;

    @Autowired
    BusinessTime time;

    @Autowired
    IncidentCommandApi incidents;

    @Autowired
    IncidentTypeApi incidentTypes;

    @Autowired
    AssignmentReminders assignmentReminders;

    @Autowired
    SwitchableUser currentUser;

    @AfterEach
    void reset() {
        push.reset();
        currentUser.reset();
    }

    private record Source(String feature, String module, String recordType, long recordId) {}

    @Test
    void T037_oneRealEventPerFeature001To008_reachesIntake_urgentIncidentPushedWithin10Seconds() {
        TestUsers users = new TestUsers(jdbc, time);
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        String token = "t037-" + UUID.randomUUID();
        jdbc.update(
                "INSERT INTO simple_record (kind, module, entity_type, entity_id, status, content, created_at, created_by)"
                        + " VALUES ('M13.PUSH_DEVICE', 'M13', 'M15.ACCOUNT', ?, 'ACTIVE',"
                        + " JSON_OBJECT('deviceId', ?, 'app', 'STAFF_APP', 'fcmToken', ?), UTC_TIMESTAMP(6), ?)",
                manager.accountId(),
                UUID.randomUUID().toString(),
                token,
                manager.accountId());
        Instant at = time.now();
        long floor = TestUsers.nextId();
        List<RecipientGroup> managers = List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));

        long resident = TestUsers.nextId();
        long errorId = TestUsers.nextId();
        long bedId = TestUsers.nextId();
        long contractId = TestUsers.nextId();
        long sessionId = TestUsers.nextId();
        long nurseShift = TestUsers.nextId();
        long leadShift = TestUsers.nextId();

        sourceCommand("T037 001", new MissingConsentReminder(resident, 1, at));
        sourceCommand("T037 002", new PermissionDataError(errorId, "T037", null, "MISSING", at));
        sourceCommand(
                "T037 003",
                new BedReadinessReminder(bedId, at.plusSeconds(3600), new BedNotificationContext(at, floor, managers)));
        sourceCommand("T037 004", new ContractOverdueEscalated(contractId, resident, 5, at));
        sourceCommand(
                "T037 005",
                new ActivityAttendanceReminder(
                        sessionId,
                        new CareEventContext(
                                "M04.ACTIVITY_SESSION", sessionId, "T037", at, Optional.of(floor), managers)));
        sourceCommand("T037 006", new FloorWithoutNurse(floor, nurseShift, 2, at));
        tx.executeWithoutResult(s -> assignmentReminders.missingFloorManager(
                floor, at, new SourceRecord("M09.SHIFT", Long.toString(leadShift)), "SHIFT:" + leadShift));

        // 007 T005 chưa nạp loại sự cố (chờ chủ dự án chốt mức mặc định): thêm như IncidentHttpTestBase nếu chưa có
        jdbc.update("INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)"
                + " SELECT 'M05.INCIDENT_TYPE', 'M05', 'FACILITY', 'Kiểm thử FACILITY', 99, 'ACTIVE',"
                + " CAST('{\"defaultLevel\":\"LOW\",\"nonHealth\":true,\"allowsNoResident\":true}' AS JSON) FROM DUAL"
                + " WHERE NOT EXISTS (SELECT 1 FROM catalog_item WHERE catalog_type = 'M05.INCIDENT_TYPE'"
                + " AND code = 'FACILITY')");

        // 007: lệnh ghi nhận sự cố cơ sở vật chất mức Khẩn cấp; đo từ lúc gọi lệnh tới lần đẩy FCM
        currentUser.set(manager.user());
        Instant started = Instant.now();
        // lệnh của 007 chạy trong transaction của module gọi (propagation MANDATORY)
        long incidentId = tx.execute(st -> incidents.record(
                new RecordIncidentCommand(
                        null,
                        incidentTypes.idOf("FACILITY"),
                        null,
                        at,
                        at,
                        floor,
                        "Kho tầng " + floor,
                        null,
                        "T037 chập điện",
                        "CRITICAL",
                        null,
                        "Ngắt cầu dao",
                        false),
                manager.user()));

        // các sự kiện 002, 004 ở trên cũng gửi Quản lý viện (Trung bình): chỉ đo lần đẩy Khẩn cấp
        await().atMost(Duration.ofSeconds(10)).until(() -> urgentPushes(token).size() > 0);
        assertThat(Duration.between(started, urgentPushes(token).getFirst().at()))
                .isLessThanOrEqualTo(Duration.ofSeconds(10));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(jdbc.queryForList(
                                "SELECT n.level FROM notification n JOIN notification_request q ON q.id = n.request_id"
                                        + " WHERE q.source_record_type = 'M05.INCIDENT' AND q.source_record_id = ?"
                                        + " AND n.account_id = ?",
                                String.class,
                                Long.toString(incidentId),
                                manager.accountId()))
                        .containsExactly("URGENT"));

        List<Source> sources = List.of(
                new Source("001", "M01", "M01.RESIDENT", resident),
                new Source("002", "M15", "M15.PERMISSION_DATA_ERROR", errorId),
                new Source("003", "M03", "M03.BED", bedId),
                new Source("004", "M02", "M02.CONTRACT", contractId),
                new Source("005", "M04", "M04.ACTIVITY_SESSION", sessionId),
                new Source("006", "M07", "M09.SHIFT", nurseShift),
                new Source("007", "M05", "M05.INCIDENT", incidentId),
                new Source("008", "M09", "M09.SHIFT", leadShift));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(sources)
                        .as("mỗi feature một yêu cầu hợp lệ ở 009")
                        .allSatisfy(s -> assertThat(request(s))
                                .as("feature %s (%s %s)", s.feature(), s.recordType(), s.recordId())
                                .hasValueSatisfying(r -> assertThat(r)
                                        .containsEntry("intake_result", "VALID")
                                        .satisfies(row -> {
                                            if (s.module() != null) {
                                                assertThat(row).containsEntry("source_module", s.module());
                                            }
                                        }))));
    }

    private List<FakePushGateway.Push> urgentPushes(String token) {
        return push.pushesTo(token).stream().filter(p -> p.message().urgent()).toList();
    }

    private Optional<Map<String, Object>> request(Source s) {
        return jdbc
                .queryForList(
                        "SELECT source_module, intake_result FROM notification_request"
                                + " WHERE source_record_type = ? AND source_record_id = ?",
                        s.recordType(),
                        Long.toString(s.recordId()))
                .stream()
                .findFirst();
    }
}
