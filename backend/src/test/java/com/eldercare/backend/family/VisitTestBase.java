package com.eldercare.backend.family;

import static com.eldercare.backend.support.TestUsers.bearer;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.incident.FakeIsolation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền test lượt thăm (012 US4; FR-029 → FR-039, BR-M10-02): người cao tuổi A Đang lưu trú có M (quyền đăng ký thăm), N
 * (quyền đăng ký thăm), X (quan hệ không có quyền đăng ký thăm). Ngày thăm {@link #day} khác nhau giữa các lần chạy;
 * lượt còn mở của ngày đó do lần chạy trước để lại được đóng trước mỗi test để sức chứa khung bắt đầu từ 0. Đồng hồ
 * đặt 08:00 ngày trước ngày thăm. Khoanh vùng của 007 qua bản giả {@link FakeIsolation} (007 chưa có bản cài).
 */
@Import(VisitTestBase.Isolation.class)
abstract class VisitTestBase extends FamilyCommandTestBase {

    static final LocalTime MORNING_START = LocalTime.of(9, 0);
    static final LocalTime MORNING_END = LocalTime.of(11, 0);
    static final LocalTime AFTERNOON_START = LocalTime.of(15, 0);
    static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);

    @TestConfiguration
    static class Isolation {
        @Bean
        FakeIsolation fakeIsolation() {
            return new FakeIsolation();
        }
    }

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeIsolation isolation;

    @Autowired
    SimpleRecordApi records;

    @Autowired
    List<M10TimedTask> tasks;

    LocalDate day;
    private long officeAccount;
    private final Map<Long, Long> familyAccounts = new HashMap<>();
    long a;
    long mMember;
    long nMember;
    long xMember;
    long m;
    long n;
    long x;

    @BeforeEach
    void visitFamily() {
        // Trước 2038-01-19: EVENT_PUBLICATION.PUBLICATION_DATE là TIMESTAMP, đồng hồ giả lập đặt theo ngày này
        day = LocalDate.of(2031, 1, 1).plusDays(TestUsers.nextId() % 2000);
        jdbc.update(
                "UPDATE simple_record SET status = 'CANCELLED' WHERE kind = 'M10.VISIT' AND effective_from = ?"
                        + " AND status IN ('APPROVED', 'CHECKED_IN')",
                day);
        officeAccount =
                users.staff(com.eldercare.backend.common.Role.OFFICE_STAFF).accountId();
        moveTo(at(day.minusDays(1), LocalTime.of(8, 0)));
        a = resident(ResidentStatus.IN_RESIDENCE);
        mMember = fixtures.familyMember("Lê M", "0904000001");
        nMember = fixtures.familyMember("Trần N", "0904000002");
        xMember = fixtures.familyMember("Phạm X", "0904000003");
        m = fixtures.relate(mMember, a, 1, true, true, FamilyRight.REGISTER_VISITS);
        n = fixtures.relate(nMember, a, 2, false, false, FamilyRight.REGISTER_VISITS);
        x = fixtures.relate(xMember, a, 3, false, false);
    }

    @AfterEach
    void resetVisitClock() {
        clock.reset();
        isolation.resident(a, List.of());
    }

    /**
     * Dời đồng hồ giả lập và lấy phiên mới cho Hành chính: phiên lập ở giờ cũ hết hạn khi đồng hồ nhảy quá thời gian không
     * thao tác (CFG-M15-10). Phiên người thân lấy qua {@link #family} sau khi dời.
     */
    void moveTo(Instant instant) {
        clock.setTo(instant);
        office = bearer(users.session(officeAccount));
    }

    /** Phiên cổng của người thân ở giờ hiện tại; tài khoản tạo một lần mỗi test. */
    String family(long familyMemberId) {
        long account = familyAccounts.computeIfAbsent(
                familyMemberId, id -> users.family(id).accountId());
        return bearer(users.session(account));
    }

    static Instant at(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, BusinessTime.ZONE).toInstant();
    }

    /** Thân đăng ký; {@code familyMemberId} chỉ khi Hành chính đăng ký thay; {@code companions} là họ tên người đi cùng. */
    static String registration(
            long residentId,
            Long familyMemberId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String... companions) {
        StringBuilder people = new StringBuilder();
        for (String c : companions) {
            if (!people.isEmpty()) {
                people.append(", ");
            }
            people.append("{\"fullName\": \"").append(c).append("\", \"relationship\": \"Cháu\"}");
        }
        String member = familyMemberId == null ? "" : ", \"familyMemberId\": " + familyMemberId;
        return "{\"residentId\": " + residentId + member + ", \"date\": \"" + date + "\", \"startTime\": \"" + start
                + "\", \"endTime\": \"" + end + "\", \"companions\": [" + people + "]}";
    }

    ResultActions register(
            String authorization, Long familyMemberId, LocalTime start, LocalTime end, String... companions)
            throws Exception {
        return postAs(authorization, "/api/v1/visits", registration(a, familyMemberId, day, start, end, companions));
    }

    ResultActions visitCommand(String authorization, long visitId, String command, String body) throws Exception {
        return postAs(authorization, "/api/v1/visits/" + visitId + "/" + command, body);
    }

    String visitStatus(long visitId) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, visitId);
    }

    /** Lượt Đã duyệt có sẵn của người cao tuổi khác (giữ chỗ trong khung buổi sáng của {@link #day}). */
    long existingVisit(int visitors) {
        long other = resident(ResidentStatus.IN_RESIDENCE);
        return tx.execute(s -> records.create(
                        Visit.KIND,
                        new NewSimpleRecord<>(
                                other,
                                new EntityRef("M01.RESIDENT", other),
                                Visit.Status.APPROVED.name(),
                                Visit.registered(
                                        mMember,
                                        day,
                                        MORNING_START,
                                        MORNING_END,
                                        visitors,
                                        Visit.Source.COUNTER,
                                        1,
                                        List.of()),
                                day,
                                day,
                                null,
                                1,
                                false))
                .id());
    }

    void runMinuteTasks(Instant at) {
        moveTo(at);
        tasks.stream()
                .filter(t -> t.cadence() == M10TimedTask.Cadence.MINUTE)
                .filter(t -> t.name().startsWith("VISIT_"))
                .forEach(t -> t.run(at, at));
    }
}
