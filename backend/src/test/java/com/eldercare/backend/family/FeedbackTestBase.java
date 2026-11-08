package com.eldercare.backend.family;

import static com.eldercare.backend.support.TestUsers.bearer;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.family.api.events.FeedbackNotice;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền test phản hồi (012 US6; FR-058 → FR-066, BR-M10-05): người cao tuổi A ở tầng {@link #floor} có Trưởng tầng được
 * giao {@link #floorManager} (bản giả nhiệm vụ của 008, vị trí của 003, phân công cho phạm vi); M có quyền xem sức khỏe
 * và có trong bản đồng ý, N không có quyền xem sức khỏe, R là người thân thứ ba. Đồng hồ giả lập đặt ở mốc riêng của mỗi
 * test; dời đồng hồ xa thì lấy phiên mới qua {@link #moveTo}, {@link #family}, {@link #staff}.
 */
@Import(FeedbackTestBase.Sources.class)
abstract class FeedbackTestBase extends FamilyCommandTestBase {

    @TestConfiguration
    static class Sources {
        @Bean
        @Primary
        FakeLocations fakeLocations() {
            return new FakeLocations();
        }

        @Bean
        @Primary
        FakeDuties fakeDuties() {
            return new FakeDuties();
        }

        @Bean
        @Primary
        FakeAssignments fakeAssignments() {
            return new FakeAssignments();
        }

        @Bean
        @Primary
        FakeBedAllocations fakeBedAllocations() {
            return new FakeBedAllocations();
        }
    }

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    List<M10TimedTask> tasks;

    long a;
    long floor;
    long mMember;
    long nMember;
    long rMember;
    TestUser floorManager;
    TestUser nurse;
    TestUser officeStaff;
    TestUser manager;
    private final Map<Long, Long> accounts = new HashMap<>();
    private final Map<Long, Long> residentAccounts = new HashMap<>();

    @BeforeEach
    void feedbackFamily() {
        // Mốc riêng mỗi lần chạy để việc theo phút không gặp phản hồi đến hạn của lần chạy trước. Giữ trước 2038-01-19
        // kể cả khi test dời thêm: EVENT_PUBLICATION.PUBLICATION_DATE là TIMESTAMP (trước đây % 20_000 ngày vượt 2038
        // khi id đã lớn, lớp family đỏ theo thứ tự chạy)
        Instant start = Instant.parse("2032-01-01T01:00:00Z").plus(Duration.ofDays(TestUsers.nextId() % 1_500));
        clock.setTo(start);
        a = resident(ResidentStatus.IN_RESIDENCE);
        floor = TestUsers.nextId();
        long room = TestUsers.nextId();
        locations.floor(floor, null).room(room, floor).stay(a, room, start.minus(Duration.ofDays(30)), null);
        bedAllocations.allocate(a, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), room, floor));
        floorManager = users.staff(Role.FLOOR_MANAGER);
        nurse = users.staff(Role.NURSING_STAFF);
        officeStaff = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        duties.floorManager(floor, floorManager.staffId());
        for (TestUser staff : List.of(floorManager, nurse)) {
            assignments.assign(
                    staff.staffId(),
                    new AssignmentView(
                            staff == floorManager ? AssignmentKind.FLOOR_MANAGER : AssignmentKind.SHIFT_MEMBER,
                            floor,
                            null,
                            null,
                            null,
                            start.minus(Duration.ofDays(30)),
                            null));
        }
        mMember = fixtures.familyMember("Lê M", "0905000001");
        nMember = fixtures.familyMember("Trần N", "0905000002");
        rMember = fixtures.familyMember("Phạm R", "0905000003");
        fixtures.relate(mMember, a, 1, true, true, FamilyRight.VIEW_HEALTH);
        fixtures.relate(nMember, a, 2, false, false);
        fixtures.relate(rMember, a, 3, false, false, FamilyRight.VIEW_COSTS);
        consents.active(a, Set.of(mMember), true);
        office = bearer(users.session(officeStaff.accountId()));
    }

    @AfterEach
    void resetFeedbackClock() {
        clock.reset();
    }

    /** Dời đồng hồ; phiên lấy sau khi dời (phiên cũ hết hạn khi đồng hồ nhảy xa, CFG-M15-10). */
    void moveTo(Instant instant) {
        clock.setTo(instant);
        office = bearer(users.session(officeStaff.accountId()));
    }

    Instant now() {
        return time.now().truncatedTo(ChronoUnit.MICROS);
    }

    String family(long familyMemberId) {
        long account =
                accounts.computeIfAbsent(familyMemberId, id -> users.family(id).accountId());
        return bearer(users.session(account));
    }

    String staff(TestUser user) {
        return bearer(users.session(user.accountId()));
    }

    /** Phiên của tài khoản người cao tuổi (AC-16); tài khoản tạo một lần mỗi test (US9). */
    String residentSession(long residentId) {
        long account = residentAccounts.computeIfAbsent(residentId, id -> users.resident(id));
        return bearer(users.session(account));
    }

    /** Người cao tuổi gửi qua kênh của mình (FR-058a (a)); {@code wishTopic} chỉ khi loại Mong muốn. */
    ResultActions channelSubmit(String authorization, String type, String group, String wishTopic, String content)
            throws Exception {
        String topic = wishTopic == null ? "" : ", \"wishTopic\": \"" + wishTopic + "\"";
        return postAs(
                authorization,
                "/api/v1/resident-channel/feedback",
                "{\"type\": \"" + type + "\", \"group\": \"" + group + "\"" + topic + ", \"content\": \"" + content
                        + "\"}");
    }

    long channelSubmitted(String authorization, String type, String group, String wishTopic) throws Exception {
        return id(channelSubmit(authorization, type, group, wishTopic, "Tôi muốn được tắm buổi tối")
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isCreated()));
    }

    ResultActions getAs(String authorization, String path) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                .header("Authorization", authorization));
    }

    static List<Long> idsOf(tools.jackson.databind.JsonNode list) {
        List<Long> result = new java.util.ArrayList<>();
        list.forEach(item -> result.add(item.get("id").asLong()));
        return result;
    }

    /** Nhân viên ghi thay người cao tuổi khi người đó nói trực tiếp (FR-058a (a)). */
    ResultActions onBehalfSubmit(String authorization, long residentId, String type, String group) throws Exception {
        return postAs(
                authorization,
                "/api/v1/feedback",
                "{\"residentId\": " + residentId + ", \"senderType\": \"RESIDENT\", \"type\": \"" + type
                        + "\", \"group\": \"" + group + "\", \"content\": \"Bà Lan nói nước tắm hơi lạnh\"}");
    }

    static String submission(long residentId, Long familyMemberId, String type, String group, String content) {
        String member = familyMemberId == null ? "" : ", \"familyMemberId\": " + familyMemberId;
        return "{\"residentId\": " + residentId + member + ", \"type\": \"" + type + "\", \"group\": \"" + group
                + "\", \"content\": \"" + content + "\"}";
    }

    ResultActions submit(String authorization, Long familyMemberId, String type, String group) throws Exception {
        return postAs(
                authorization,
                "/api/v1/feedback",
                submission(a, familyMemberId, type, group, "Phòng P201 không được dọn hai ngày"));
    }

    long submitted(String authorization, Long familyMemberId, String type, String group) throws Exception {
        return id(submit(authorization, familyMemberId, type, group)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isCreated()));
    }

    ResultActions feedbackCommand(String authorization, long feedbackId, String command, String body) throws Exception {
        return postAs(authorization, "/api/v1/feedback/" + feedbackId + "/" + command, body);
    }

    static String reply() {
        return "{\"handlingDirection\": \"Bố trí dọn phòng hằng ngày\", \"content\": \"Đã dọn và nhắc tổ chăm sóc\"}";
    }

    /** Mốc lưu ở cột (UTC); đồng hồ giả lập vẫn chạy sau khi đặt nên mốc lấy từ chính phản hồi. */
    Instant at(long feedbackId, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM feedback WHERE id = ?", LocalDateTime.class, feedbackId)
                .toInstant(ZoneOffset.UTC);
    }

    Map<String, Object> row(long feedbackId) {
        return jdbc.queryForMap("SELECT * FROM feedback WHERE id = ?", feedbackId);
    }

    List<FeedbackNotice> notices(long feedbackId, FeedbackNotice.Kind kind) {
        return events.stream(FeedbackNotice.class)
                .filter(n -> n.feedbackId() == feedbackId && n.kind() == kind)
                .toList();
    }

    /** Chạy việc theo phút của phản hồi ở thời điểm đã cho. */
    void runFeedbackTasks(Instant at) {
        moveTo(at);
        tasks.stream()
                .filter(t -> t.cadence() == M10TimedTask.Cadence.MINUTE)
                .filter(t -> t.name().startsWith("FEEDBACK_"))
                .forEach(t -> t.run(at, at));
    }
}
