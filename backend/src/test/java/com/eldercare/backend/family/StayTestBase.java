package com.eldercare.backend.family;

import static com.eldercare.backend.support.TestUsers.bearer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.BedStatusApi;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.incident.FakeIsolation;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền test lượt ở lại (012 US8; FR-040 → FR-045, BR-M10-04): người cao tuổi A Đang lưu trú ở phòng {@link #room} tầng
 * {@link #floor} có Trưởng tầng được giao {@link #floorManager} (bản giả vị trí của 003, nhiệm vụ của 008); R, M có quan
 * hệ Hiệu lực với A. Số giường đang dùng của phòng qua bản giả {@link BedStatusApi} (mặc định không giường nào), khoanh
 * vùng qua {@link FakeIsolation}. Ngày gốc {@link #day} khác nhau giữa các lần chạy; việc nửa đêm chạy trực tiếp và test
 * chỉ xét lượt của mình.
 */
@Import(StayTestBase.Sources.class)
abstract class StayTestBase extends FamilyCommandTestBase {

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

        @Bean
        FakeIsolation fakeIsolation() {
            return new FakeIsolation();
        }

        @Bean
        @Primary
        BedStatusApi fakeBedStatus() {
            return mock(BedStatusApi.class);
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
    FakeIsolation isolation;

    @Autowired
    BedStatusApi bedStatus;

    @Autowired
    List<M10TimedTask> tasks;

    LocalDate day;
    long a;
    long floor;
    long room;
    long rMember;
    long mMember;
    TestUser floorManager;
    TestUser officeStaff;
    TestUser manager;
    private final Map<Long, Long> accounts = new HashMap<>();

    @BeforeEach
    void stayFamily() {
        // Trước 2038-01-19: EVENT_PUBLICATION.PUBLICATION_DATE là TIMESTAMP, đồng hồ giả lập đặt theo ngày này
        day = LocalDate.of(2028, 1, 1).plusDays(TestUsers.nextId() % 3000);
        Instant start = at(day.minusDays(1), 8, 0);
        clock.setTo(start);
        a = resident(ResidentStatus.IN_RESIDENCE);
        floor = TestUsers.nextId();
        room = TestUsers.nextId();
        locations.floor(floor, null).room(room, floor).stay(a, room, start.minus(Duration.ofDays(30)), null);
        bedAllocations.allocate(a, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), room, floor));
        floorManager = users.staff(Role.FLOOR_MANAGER);
        officeStaff = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        duties.floorManager(floor, floorManager.staffId());
        assignments.assign(
                floorManager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER, floor, null, null, null, start.minus(Duration.ofDays(30)), null));
        rMember = fixtures.familyMember("Phạm R", "0906000001");
        mMember = fixtures.familyMember("Lê M", "0906000002");
        fixtures.relate(rMember, a, 1, true, true);
        fixtures.relate(mMember, a, 2, false, false);
        office = staff(officeStaff);
    }

    @AfterEach
    void resetStay() {
        clock.reset();
        isolation.resident(a, List.of());
        reset(bedStatus);
    }

    static Instant at(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
                .atZone(BusinessTime.ZONE)
                .toInstant();
    }

    /** Dời đồng hồ; phiên lấy sau khi dời (phiên cũ hết hạn khi đồng hồ nhảy xa, CFG-M15-10). */
    void moveTo(Instant instant) {
        clock.setTo(instant);
        office = staff(officeStaff);
    }

    String staff(TestUser user) {
        return bearer(users.session(user.accountId()));
    }

    String family(long familyMemberId) {
        long account =
                accounts.computeIfAbsent(familyMemberId, id -> users.family(id).accountId());
        return bearer(users.session(account));
    }

    /** Phòng của A có {@code beds} giường đang dùng (FR-040: từ hai giường thì cần ý kiến người cùng phòng). */
    void occupiedBeds(int beds) {
        doReturn(Map.of(BedStatus.OCCUPIED, beds))
                .when(bedStatus)
                .countByState(any(ScopeFilter.class), any(Instant.class));
    }

    static String registration(long residentId, long familyMemberId, Instant start, Instant end, boolean meal) {
        return "{\"familyMemberId\": " + familyMemberId + ", \"residentId\": " + residentId
                + ", \"expectedStartAt\": \""
                + start + "\", \"expectedEndAt\": \"" + end + "\", \"location\": \"PHONG_NCT\", \"reason\": \"Chăm sóc"
                + " sau mổ\", \"mealRegistered\": " + meal + "}";
    }

    ResultActions register(String authorization, long familyMemberId, Instant start, Instant end) throws Exception {
        return postAs(authorization, "/api/v1/family-stays", registration(a, familyMemberId, start, end, true));
    }

    long registered(long familyMemberId, Instant start, Instant end) throws Exception {
        return id(register(office, familyMemberId, start, end)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isCreated()));
    }

    ResultActions command(String authorization, long stayId, String command, String body) throws Exception {
        return postAs(authorization, "/api/v1/family-stays/" + stayId + "/" + command, body);
    }

    /** Lượt Đã xác nhận do Trưởng tầng xác nhận (phòng một giường, không cần ý kiến người cùng phòng). */
    long confirmed(long familyMemberId, Instant start, Instant end) throws Exception {
        long id = registered(familyMemberId, start, end);
        command(staff(floorManager), id, "confirm", "{}")
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk());
        return id;
    }

    String stayStatus(long stayId) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, stayId);
    }

    /** Chạy việc nửa đêm của lượt ở lại cho mốc {@code midnight} như khung M10 (transaction riêng). */
    int runNights(Instant midnight) {
        moveTo(midnight);
        return tasks.stream()
                .filter(t -> t.cadence() == M10TimedTask.Cadence.MIDNIGHT)
                .filter(t -> t.name().startsWith("FAMILY_STAY_"))
                .mapToInt(t -> tx.execute(s -> t.run(midnight, midnight)))
                .sum();
    }
}
