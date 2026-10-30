package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.TestOfflineHandlers.CareNotes;
import com.eldercare.backend.administration.account.api.events.RecordSyncedAfterPermissionLoss;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.common.offline.SyncResult;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T050 (US4 #12; FR-041a, BR-M15-20, research A-11): bản ghi ngoại tuyến có thời điểm thiết bị hợp lý được kiểm ba
 * lớp tại thời điểm đó; còn quyền lúc đồng bộ thì {@code ACCEPTED}, mất quyền sau khi ghi thì {@code ACCEPTED_WITH_FLAG}
 * kèm sự kiện báo trưởng tầng; ghi sau khi mất quyền thì {@code REJECTED}; thao tác bắt buộc trực tuyến bị từ chối; trùng
 * khóa chống lặp không ghi hai lần; phản hồi báo kết quả từng bản ghi, phiên kết thúc và người cao tuổi còn trong phạm vi.
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, TestOfflineHandlers.class})
@RecordApplicationEvents
class SyncResultIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    CareNotes careNotes;

    @Autowired
    ApplicationEvents events;

    AccountFixtures fixtures;
    long floor;
    long resident;
    long account;
    String device;
    String token;
    Instant now;

    @BeforeEach
    void caregiverOnFloorWithDevice() {
        fixtures = new AccountFixtures(jdbc, time);
        now = time.now();
        floor = AccountFixtures.nextId();
        resident = AccountFixtures.nextId();
        bedAllocations.allocate(
                resident, new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), 1, floor));
        account = fixtures.staffAccount("AC-05");
        assignments.assign(
                fixtures.staffId(account),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER, floor, null, null, null, now.minus(Duration.ofDays(1)), null));
        device = UUID.randomUUID().toString();
        token = fixtures.session(account, device);
    }

    @Test
    void FR_041a_plausibleAndAllowed_accepted_appliedAtDeviceTime() throws Exception {
        Instant connected = lastConnected(now.minus(Duration.ofHours(1)));
        Instant deviceTime = now.minus(Duration.ofMinutes(30));
        String key = UUID.randomUUID().toString();

        sync(connected, record(key, TestOfflineHandlers.CARE_NOTE, deviceTime, resident, "accepted-" + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionEnded").value(false))
                .andExpect(jsonPath("$.residentsInScope", hasItem((int) resident)))
                .andExpect(jsonPath("$.results[0].idempotencyKey").value(key))
                .andExpect(jsonPath("$.results[0].result").value("ACCEPTED"))
                .andExpect(jsonPath("$.results[0].reference").isNotEmpty());

        assertThat(careNotes.appliedWith("accepted-" + key)).singleElement().satisfies(a -> {
            assertThat(a.effectiveAt()).isEqualTo(deviceTime);
            assertThat(a.outcome().result()).isEqualTo(SyncResult.ACCEPTED);
        });
    }

    @Test
    void US4_12_lockedAfterRecording_acceptedWithFlag_recordAfterLock_rejected_sessionEnded() throws Exception {
        Instant connected = lastConnected(now.minus(Duration.ofHours(3)));
        lockAt(now.minus(Duration.ofMinutes(30)));
        String before = UUID.randomUUID().toString();
        String after = UUID.randomUUID().toString();

        sync(
                        connected,
                        record(before, TestOfflineHandlers.CARE_NOTE, now.minus(Duration.ofHours(2)), resident, before),
                        record(
                                after,
                                TestOfflineHandlers.CARE_NOTE,
                                now.minus(Duration.ofMinutes(10)),
                                resident,
                                after))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionEnded").value(true))
                .andExpect(jsonPath("$.residentsInScope").isEmpty())
                .andExpect(jsonPath("$.results[0].result").value("ACCEPTED_WITH_FLAG"))
                .andExpect(jsonPath("$.results[1].result").value("REJECTED"))
                .andExpect(jsonPath("$.results[1].reason").isNotEmpty());

        assertThat(careNotes.appliedWith(before))
                .singleElement()
                .satisfies(a -> assertThat(a.outcome().result()).isEqualTo(SyncResult.ACCEPTED_WITH_FLAG));
        assertThat(careNotes.appliedWith(after)).isEmpty();
        assertThat(events.stream(RecordSyncedAfterPermissionLoss.class)
                        .filter(e -> e.recordType().equals(TestOfflineHandlers.CARE_NOTE))
                        .filter(e -> floor == e.floorAreaId()))
                .hasSize(1);
    }

    @Test
    void FR_041a_outOfScopeAtDeviceTime_rejected() throws Exception {
        long elsewhere = AccountFixtures.nextId();
        bedAllocations.allocate(
                elsewhere,
                new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), 2, AccountFixtures.nextId()));
        String key = UUID.randomUUID().toString();

        sync(
                        lastConnected(now.minus(Duration.ofHours(1))),
                        record(key, TestOfflineHandlers.CARE_NOTE, now.minus(Duration.ofMinutes(5)), elsewhere, key))
                .andExpect(jsonPath("$.results[0].result").value("REJECTED"));
        assertThat(careNotes.appliedWith(key)).isEmpty();
    }

    @Test
    void Q_01_requiresOnline_rejected_invalidContent_rejected() throws Exception {
        String emergency = UUID.randomUUID().toString();
        String blank = UUID.randomUUID().toString();

        sync(
                        lastConnected(now.minus(Duration.ofHours(1))),
                        record(
                                emergency,
                                TestOfflineHandlers.EMERGENCY,
                                now.minus(Duration.ofMinutes(5)),
                                resident,
                                emergency),
                        record(blank, TestOfflineHandlers.CARE_NOTE, now.minus(Duration.ofMinutes(5)), resident, ""))
                .andExpect(jsonPath("$.results[0].result").value("REJECTED"))
                .andExpect(jsonPath("$.results[1].result").value("REJECTED"));
    }

    @Test
    void FR_041c_sameIdempotencyKeyTwice_appliedOnce_sameResult() throws Exception {
        Instant connected = lastConnected(now.minus(Duration.ofHours(1)));
        String key = UUID.randomUUID().toString();
        String body = record(key, TestOfflineHandlers.CARE_NOTE, now.minus(Duration.ofMinutes(5)), resident, key);

        String first = sync(connected, body).andReturn().getResponse().getContentAsString();
        String second = sync(connected, body).andReturn().getResponse().getContentAsString();

        assertThat(careNotes.appliedWith(key)).hasSize(1);
        assertThat(second).contains("\"result\":\"ACCEPTED\"").contains(key);
        assertThat(first).contains("\"result\":\"ACCEPTED\"");
    }

    /** Lần kết nối gần nhất của thiết bị mà máy chủ đã thấy, trước khi ngoại tuyến. */
    private Instant lastConnected(Instant at) {
        jdbc.update(
                "INSERT INTO device (device_id, last_connected_at, last_account_id) VALUES (?, ?, ?)",
                device,
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                account);
        return at;
    }

    /** Tài khoản bị khóa tại thời điểm đã cho: lịch sử trạng thái, phiên kết thúc như lệnh khóa (FR-013). */
    private void lockAt(Instant at) {
        String history = "INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value,"
                + " starts_at, by_system, details) VALUES ('M15.ACCOUNT', ?, 'STATUS', 'M15', 'POINT', ?, ?, TRUE, ?)";
        jdbc.update(
                history,
                account,
                "ACTIVE",
                LocalDateTime.ofInstant(now.minus(Duration.ofDays(1)), ZoneOffset.UTC),
                "{\"lockCause\": null}");
        jdbc.update(
                history, account, "LOCKED", LocalDateTime.ofInstant(at, ZoneOffset.UTC), "{\"lockCause\": \"MANUAL\"}");
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", account);
        jdbc.update(
                "UPDATE login_session SET ended_at = ?, end_reason = 'ACCOUNT_LEFT_ACTIVE' WHERE account_id = ?",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                account);
    }

    private ResultActions sync(Instant connected, String... records) throws Exception {
        return mvc.perform(post("/api/v1/sync")
                .header("Authorization", bearer(token))
                .header("X-Device-Id", device)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"lastConnectedAt\":\"" + connected + "\",\"records\":[" + String.join(",", records) + "]}"));
    }

    static String record(String key, String type, Instant deviceTime, Long residentId, String note) {
        return "{\"idempotencyKey\":\"" + key + "\",\"recordType\":\"" + type + "\",\"deviceTime\":\"" + deviceTime
                + "\",\"content\":{\"residentId\":" + residentId + ",\"note\":\"" + note + "\"}}";
    }
}
