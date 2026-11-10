package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.care.FakeActivityHealthGate;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * 014 T010 (BR-M04-15; FR-015 → FR-018; US2 kịch bản 1 → 3): đăng ký có giới hạn số lượng dưới khóa buổi, chặn nghi
 * nhiễm ở hoạt động nhóm, cảnh báo cần xác nhận, hủy đăng ký có lý do. Không dùng transaction của test để hai đăng ký chỗ
 * cuối chạy đồng thời thật; dữ liệu do test tạo được dọn ở cuối.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class BR_M04_15_DangKyIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeActivityHealthGate gate;

    @Autowired
    JsonMapper json;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    long floor;
    long typeId;
    final List<Long> activities = new ArrayList<>();

    @BeforeEach
    void fixture() {
        floor = TestUsers.nextId();
        var manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        user.set(manager.user());
        typeId = new org.springframework.transaction.support.TransactionTemplate(transactions)
                .execute(tx -> catalogs.create(
                                ActivityKinds.ACTIVITY_TYPES,
                                new NewCatalogItem<>(
                                        null,
                                        "Vận động " + TestUsers.nextId(),
                                        null,
                                        0,
                                        new ActivityTypeAttributes(true, false, List.of())))
                        .id());
    }

    @AfterEach
    void cleanup() throws Exception {
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            for (long a : activities) {
                statement.executeUpdate(
                        "DELETE r FROM activity_registration r JOIN activity_session s ON s.id=r.session_id WHERE s.activity_id="
                                + a);
                statement.executeUpdate("DELETE FROM activity_session WHERE activity_id=" + a);
                statement.executeUpdate("DELETE FROM simple_record WHERE id=" + a);
            }
            statement.executeUpdate("DELETE FROM catalog_item WHERE id=" + typeId);
        }
    }

    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "registration-" + TestUsers.nextId())
                .content(body);
    }

    long id(ResultActions result) throws Exception {
        var location =
                result.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    /** Hoạt động mới của tầng và một buổi lẻ ngày mai 15:00–16:00. */
    long session(int max, boolean group) throws Exception {
        long activity = id(mvc.perform(write(
                post("/api/v1/activities"),
                "{\"name\":\"Cờ tướng\",\"typeId\":" + typeId + ",\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":" + max + ",\"groupActivity\":" + group
                        + (group ? "" : ",\"groupActivityChangeReason\":\"Chơi một mình\"") + "}")));
        activities.add(activity);
        var start =
                time.today().plusDays(1).atTime(15, 0).atZone(BusinessTime.ZONE).toInstant();
        return id(mvc.perform(write(
                post("/api/v1/activity-sessions"),
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + start + "\",\"endsAt\":\""
                        + start.plusSeconds(3600) + "\"}")));
    }

    long resident() {
        long r = TestUsers.nextId();
        residents.resident(r, "[Họ tên NCT]", Gender.FEMALE, ResidentStatus.IN_RESIDENCE);
        return r;
    }

    ResultActions register(long session, long resident, boolean acknowledge) throws Exception {
        return mvc.perform(write(
                post("/api/v1/activity-sessions/" + session + "/registrations"),
                "{\"residentId\":" + resident + (acknowledge ? ",\"acknowledgeWarnings\":true" : "") + "}"));
    }

    @Test
    void BR_M04_15_twoConcurrentRegistrationsForTheLastSeatLetOnlyOneThrough() throws Exception {
        long s = session(1, true);
        long a = resident(), b = resident();
        var statuses = new ConcurrentLinkedQueue<Integer>();
        var start = new CountDownLatch(1);
        // @WithMockUser chỉ gắn ngữ cảnh bảo mật cho luồng test; luồng của pool cần bản sao.
        var security = org.springframework.security.test.context.TestSecurityContextHolder.getContext();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<Future<?>>();
            for (long r : List.of(a, b))
                futures.add(pool.submit(() -> {
                    org.springframework.security.test.context.TestSecurityContextHolder.setContext(security);
                    start.await();
                    statuses.add(register(s, r, false).andReturn().getResponse().getStatus());
                    return null;
                }));
            start.countDown();
            for (var f : futures) f.get(30, TimeUnit.SECONDS);
        }
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM activity_registration WHERE session_id=? AND status<>'CANCELLED'",
                        Integer.class,
                        s))
                .isEqualTo(1);
    }

    /** T010 Q10, Q11; gộp một ca theo ngân sách IT (AGENTS.md mục 4), mỗi dòng dùng buổi và người cao tuổi riêng. */
    @Test
    void FR_018_FR_021_warningAcknowledgementAndIsolationCancellation() throws Exception {
        com.eldercare.backend.support.TableRows.each(List.of("WARNING_ACKNOWLEDGEMENT", "ISOLATION"), scenario -> {
            switch (scenario) {
                case "WARNING_ACKNOWLEDGEMENT" -> FR_018_warningsNeedAcknowledgementAndAreStored();
                case "ISOLATION" -> FR_021_isolationCancelsSessionsInTheZoneAndRegistrationsOfIsolatedResidents();
            }
        });
    }

    /** Q10 (FR-018, FR-019): cảnh báo chỉ cần xác nhận; xác nhận thì đăng ký lưu dấu cảnh báo đã xác nhận. */
    private void FR_018_warningsNeedAcknowledgementAndAreStored() throws Exception {
        long s = session(20, true);
        long r = resident();
        gate.openMediumItem(r);
        register(s, r, false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WARNING_ACKNOWLEDGEMENT_REQUIRED"))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("OPEN_ALERT_OR_INCIDENT"));
        register(s, r, true).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject(
                        "SELECT acknowledged_warnings FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .contains("OPEN_ALERT_OR_INCIDENT");
    }

    /**
     * Q11 (FR-021): 007 gọi trong lệnh khoanh vùng; buổi chưa điểm danh ở địa điểm trong vùng bị hủy kèm đăng ký, đăng ký
     * của người trong vùng ở buổi khác bị hủy. Gỡ vùng không khôi phục: API không có hàm gỡ, bản ghi giữ Đã hủy.
     */
    private void FR_021_isolationCancelsSessionsInTheZoneAndRegistrationsOfIsolatedResidents() throws Exception {
        long room = TestUsers.nextId();
        long inZone = session(20, true), elsewhere = session(20, true);
        jdbc.update("UPDATE activity_session SET location_room_id=? WHERE id=?", room, inZone);
        long isolated = resident(), other = resident(), third = resident();
        long r1 = id(register(inZone, other, false));
        long r2 = id(register(elsewhere, isolated, false));
        long r3 = id(register(elsewhere, third, false));
        gate.isolateRoom(room);
        gate.isolateResident(isolated);

        new org.springframework.transaction.support.TransactionTemplate(transactions)
                .executeWithoutResult(tx -> autoCancel.cancelForIsolation(TestUsers.nextId(), time.now()));

        assertThat(jdbc.queryForMap("SELECT status,cancelled_by FROM activity_session WHERE id=?", inZone))
                .containsEntry("status", "CANCELLED")
                .containsEntry("cancelled_by", "ISOLATION");
        assertThat(registration(r1)).containsEntry("status", "CANCELLED");
        assertThat(jdbc.queryForObject("SELECT status FROM activity_session WHERE id=?", String.class, elsewhere))
                .isEqualTo("SCHEDULED");
        assertThat(registration(r2)).containsEntry("status", "CANCELLED").containsEntry("auto_cancel", "ISOLATION");
        assertThat(registration(r3)).containsEntry("status", "REGISTERED");
    }

    @Autowired
    com.eldercare.backend.care.api.ActivityRegistrationAutoCancelApi autoCancel;

    Map<String, Object> registration(long id) {
        return jdbc.queryForMap("SELECT status,auto_cancel FROM activity_registration WHERE id=?", id);
    }
}
