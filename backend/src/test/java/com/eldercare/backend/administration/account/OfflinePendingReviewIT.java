package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.eldercare.backend.administration.account.TestOfflineHandlers.CareNotes;
import com.eldercare.backend.administration.account.api.events.RecordPendingReview;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeFloorAreaNames;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
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
 * 002 T050 (US4 #14; FR-041c, data-model mục 5): thời điểm thiết bị muộn hơn lúc đồng bộ, sớm hơn lần kết nối cuối,
 * hoặc ngoại tuyến quá CFG-M15-08 thì bản ghi chờ xem lại, không tự chấp nhận hay từ chối; Trưởng tầng được giao của
 * tầng thấy và quyết định (quyết định đầu tiên thắng, từ chối phải có lý do); chấp nhận thì bản ghi có hiệu lực từ lúc
 * chấp nhận; bản ghi không gắn tầng do Quản lý viện quyết định.
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, TestOfflineHandlers.class})
@RecordApplicationEvents
class OfflinePendingReviewIT extends MySqlIntegrationTestBase {

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
    FakeDuties duties;

    @Autowired
    CareNotes careNotes;

    @Autowired
    FakeStaffProfiles staffProfiles;

    @Autowired
    FakeFloorAreaNames floorNames;

    @Autowired
    ApplicationEvents events;

    AccountFixtures fixtures;
    long floor;
    long resident;
    long caregiver;
    String device;
    String caregiverToken;
    String floorManagerToken;
    Instant now;

    @BeforeEach
    void floorWithManagerAndCaregiver() {
        fixtures = new AccountFixtures(jdbc, time);
        now = time.now();
        floor = AccountFixtures.nextId();
        resident = AccountFixtures.nextId();
        bedAllocations.allocate(
                resident, new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), 1, floor));
        caregiver = fixtures.staffAccount("AC-05");
        assignments.assign(
                fixtures.staffId(caregiver),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER, floor, null, null, null, now.minus(Duration.ofDays(3)), null));
        device = UUID.randomUUID().toString();
        caregiverToken = fixtures.session(caregiver, device);
        long floorManager = fixtures.staffAccount("AC-02");
        duties.floorManager(floor, fixtures.staffId(floorManager));
        floorManagerToken = fixtures.session(floorManager);
    }

    @Test
    void US4_14_offlineBeyondCfgM15_08_allPendingReview_deciderNotified() throws Exception {
        Instant connected = lastConnected(now.minus(Duration.ofHours(26)));
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();

        sync(
                        connected,
                        SyncResultIT.record(
                                first, TestOfflineHandlers.CARE_NOTE, now.minus(Duration.ofHours(20)), resident, first),
                        SyncResultIT.record(
                                second,
                                TestOfflineHandlers.CARE_NOTE,
                                now.minus(Duration.ofHours(2)),
                                resident,
                                second))
                .andExpect(jsonPath("$.results[0].result").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.results[1].result").value("PENDING_REVIEW"));

        assertThat(careNotes.appliedWith(first)).isEmpty();
        assertThat(pending(first))
                .containsEntry("pending_reason", "EXCEEDS_CFG_M15_08")
                .containsEntry("status", "PENDING_REVIEW")
                .containsEntry("floor_area_id", floor);
        assertThat(events.stream(RecordPendingReview.class).filter(e -> floor == e.floorAreaId()))
                .hasSize(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'OFFLINE_PENDING_REVIEW' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(pending(first).get("id"))))
                .isEqualTo(1);
    }

    /** Một bản ghi chờ xem lại qua đồng bộ (ngoại tuyến quá CFG-M15-08); trả id. */
    private long pendingRecord(Long residentId) throws Exception {
        String key = UUID.randomUUID().toString();
        Instant connected = lastConnected(now.minus(Duration.ofHours(30)));
        sync(
                        connected,
                        SyncResultIT.record(
                                key, TestOfflineHandlers.CARE_NOTE, now.minus(Duration.ofHours(4)), residentId, key))
                .andExpect(jsonPath("$.results[0].result").value("PENDING_REVIEW"));
        long id = ((Number) pending(key).get("id")).longValue();
        jdbc.update(
                "UPDATE simple_record SET content = JSON_SET(content, '$.content.note', ?) WHERE id = ?",
                "pending-" + id,
                id);
        return id;
    }

    private java.util.Map<String, Object> pending(String key) {
        return jdbc.queryForMap(
                "SELECT id, content->>'$.pendingReason' AS pending_reason, status,"
                        + " JSON_VALUE(content, '$.floorAreaId' RETURNING SIGNED) AS floor_area_id FROM simple_record"
                        + " WHERE kind = 'M15.PENDING_REVIEW_RECORD' AND content->>'$.idempotencyKey' = ?",
                key);
    }

    private Instant lastConnected(Instant at) {
        jdbc.update(
                "INSERT INTO technical_lock (lock_key, version, updated_at) VALUES (?, 0, ?) AS new"
                        + " ON DUPLICATE KEY UPDATE updated_at = new.updated_at",
                "M15.DEVICE:" + device,
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
        return at;
    }

    private ResultActions sync(Instant connected, String... records) throws Exception {
        return mvc.perform(post("/api/v1/sync")
                .header("Authorization", bearer(caregiverToken))
                .header("X-Device-Id", device)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"lastConnectedAt\":\"" + connected + "\",\"records\":[" + String.join(",", records) + "]}"));
    }

    private ResultActions decide(String token, long id, String command, String ifMatch, String body) throws Exception {
        var request = post("/api/v1/pending-review-records/" + id + "/" + command)
                .header("Authorization", bearer(token))
                .header("If-Match", ifMatch)
                .header("Idempotency-Key", UUID.randomUUID().toString());
        if (!body.isEmpty()) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request);
    }
}
