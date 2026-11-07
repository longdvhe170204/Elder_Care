package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.api.dto.AttendanceView;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.api.spi.SosTripPort;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.support.TestUsers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Nền cho IT nút SOS (US12; T067 → T071): như {@link EmergencyTestBase} (để kích hoạt khẩn cấp từ cảnh báo SOS), thêm
 * tài khoản người cao tuổi với thiết bị gắn thật của 002 (token thiết bị lưu SHA-256 ở {@code device_credential}), loại
 * lưu trú (001 SPI {@code ContractApi}), có mặt bán trú (005) và chuyến đi (014, bản giả Mockito của
 * {@link SosTripPort}). Mặc định: nội trú dài hạn, Đang lưu trú, phòng 11 giường 12, có Bác sĩ trực.
 */
@RecordApplicationEvents
abstract class SosTestBase extends EmergencyTestBase {
    static final String SOS = "/api/v1/sos";
    static final String CURRENT = "/api/v1/sos/current";
    static final String PLACE = "/api/v1/sos/{id}/place";
    static final long ROOM = 11;
    static final long BED = 12;

    @MockitoSpyBean
    ContractApi contracts;

    @MockitoSpyBean
    DayCareAttendanceApi attendance;

    @MockitoBean
    SosTripPort trips;

    /** Họ tên người tiếp nhận (008 hồ sơ nhân viên chưa có trong test). */
    @MockitoSpyBean
    com.eldercare.backend.administration.account.api.AccountApi accounts;

    @Autowired
    ApplicationEvents events;

    String deviceToken;
    long residentAccount;
    String deviceId;

    @BeforeEach
    void prepareSos() {
        residentAccount = residentAccount(resident);
        deviceId = "dev-" + resident;
        deviceToken = bindDevice(residentAccount, deviceId);
        doReturn(Optional.of("Điều Dưỡng Hoa")).when(accounts).fullName(nurse.accountId());
        doReturn(Optional.of(ResidenceType.LONG_TERM_RESIDENTIAL))
                .when(contracts)
                .currentResidenceType(anyLong(), any());
        doReturn(Optional.of(new com.eldercare.backend.roombedallocation.api.dto.LocationView(
                        null, null, floor, "Tầng", ROOM, "P11", BED, "G12")))
                .when(locations)
                .current(resident);
    }

    long residentAccount(long residentId) {
        var created = Timestamp.from(time.now().minusSeconds(86_400));
        jdbc.update("""
                INSERT INTO account (username, type, resident_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'RESIDENT', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "sos-resident-" + residentId, residentId, created, created);
        return jdbc.queryForObject("SELECT id FROM account WHERE resident_id = ?", Long.class, residentId);
    }

    /** Thiết bị đang gắn như 002 {@code DeviceBindings.bind}; trả token thiết bị. */
    String bindDevice(long accountId, String device) {
        String token = "sos-" + UUID.randomUUID();
        jdbc.update(
                "INSERT INTO device_credential (kind, account_id, device_id, secret_hash, created_at)"
                        + " VALUES ('BOUND_DEVICE', ?, ?, ?, ?)",
                accountId,
                device,
                sha256(token),
                Timestamp.from(time.now()));
        return token;
    }

    void dayCare(AttendanceStatus status) {
        doReturn(Optional.of(ResidenceType.DAY_CARE)).when(contracts).currentResidenceType(anyLong(), any());
        doReturn(Optional.ofNullable(
                        status == null
                                ? null
                                : new AttendanceView(
                                        TestUsers.nextId(),
                                        resident,
                                        time.today(),
                                        status,
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        false)))
                .when(attendance)
                .onDate(anyLong(), any());
    }

    ResultActions press(String key) throws Exception {
        return press(deviceToken, key);
    }

    ResultActions press(String token, String key) throws Exception {
        var request = MockMvcRequestBuilders.post(SOS).header("X-Device-Token", token);
        if (key != null) request = request.header("Idempotency-Key", key);
        return mvc.perform(request);
    }

    ResultActions current() throws Exception {
        return mvc.perform(get(CURRENT).header("X-Device-Token", deviceToken));
    }

    ResultActions place(long pressId, String place) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(PLACE, pressId)
                .header("X-Device-Token", deviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"place\":\"" + place + "\"}"));
    }

    long sosAlert() {
        return jdbc.queryForObject(
                "SELECT id FROM alert WHERE resident_id = ? AND type_lock = 'SOS'", Long.class, resident);
    }

    int sosAlerts() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM alert WHERE resident_id = ? AND type_lock = 'SOS'", Integer.class, resident);
    }

    int presses() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.SOS_PRESS' AND entity_id = ?",
                Integer.class,
                resident);
    }

    /** Nhóm người nhận của các {@link AlertCreated} của cảnh báo SOS. */
    List<RecipientGroup> createdGroups() {
        return events.stream(AlertCreated.class)
                .filter(event -> "SOS".equals(event.eventKey()))
                .flatMap(event -> event.notificationRequests().stream())
                .flatMap(request -> request.recipientGroups().stream())
                .toList();
    }

    /** Mọi yêu cầu thông báo đã phát trong test (sự kiện cài {@code HasNotificationRequests}). */
    List<NotificationRequest> allRequests() {
        return events.stream(com.eldercare.backend.common.notification.HasNotificationRequests.class)
                .flatMap(event -> event.notificationRequests().stream())
                .toList();
    }

    private static byte[] sha256(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
