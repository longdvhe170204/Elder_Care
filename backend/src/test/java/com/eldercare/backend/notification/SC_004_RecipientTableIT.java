package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.StaffStatus;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.intake.domain.RequestValidation;
import com.eldercare.backend.notification.internal.recipient.domain.ResolvedRecipient;
import com.eldercare.backend.notification.internal.recipient.service.RecipientResolver;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.staffing.FakeM09Config;
import com.eldercare.backend.support.fake.staffing.FakeShifts;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SC-004 (T013, tên logic {@code SC_004_NguoiNhanTest}): 200 yêu cầu sinh theo hạt giống cố định trên dữ liệu có đổi Người
 * phụ trách ca giữa ca, vắng ca, Nghỉ việc, tạm nhận và tầng thiếu Trưởng tầng; người nhận thực tế khớp 100% bảng tính tay
 * từ mô hình tầng bên dưới, không ai nhận hai lần trong một yêu cầu, không ai theo danh sách tên cấu hình sẵn. Gọi thẳng
 * {@link RecipientResolver} (cần tài khoản thật nên là test tích hợp) để 200 lượt chạy nhanh.
 */
@Import({FakeM15Config.class, FakeM09Config.class})
class SC_004_RecipientTableIT extends MySqlIntegrationTestBase {

    private static final Substitution DEFAULT = new Substitution.Default();
    private static final List<Set<Role>> ROLE_FILTERS =
            List.of(Set.of(), Set.of(Role.NURSING_STAFF), Set.of(Role.CAREGIVER));

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    RecipientResolver resolver;

    @Autowired
    FakeShifts shifts;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeStaffProfiles profiles;

    /**
     * Một tầng trong bảng tính tay: hai người có thể làm Người phụ trách ca, Trưởng tầng được giao (rỗng là tầng thiếu
     * người, khi đó Người phụ trách ca thay theo Q-90), Điều dưỡng, chăm sóc có mặt, người vắng ca và người Nghỉ việc.
     */
    private record Floor(
            long id,
            TestUser lead1,
            TestUser lead2,
            Optional<TestUser> floorManager,
            List<TestUser> nurses,
            List<TestUser> caregivers,
            TestUser absent,
            TestUser resigned,
            List<Long> residents,
            Map<Long, Long> nurseOf) {}

    private final Map<Long, Long> currentLead = new java.util.HashMap<>();

    @AfterEach
    void resetShifts() {
        shifts.reset();
    }

    private Floor floor(TestUsers users, boolean withFloorManager) {
        long id = TestUsers.nextId();
        TestUser lead1 = users.staff(Role.NURSING_STAFF);
        TestUser lead2 = users.staff(Role.NURSING_STAFF);
        Optional<TestUser> fm = withFloorManager ? Optional.of(users.staff(Role.FLOOR_MANAGER)) : Optional.empty();
        List<TestUser> nurses = List.of(users.staff(Role.NURSING_STAFF), users.staff(Role.NURSING_STAFF));
        List<TestUser> caregivers = List.of(users.staff(Role.CAREGIVER), users.staff(Role.CAREGIVER));
        TestUser absent = users.staff(Role.CAREGIVER);
        TestUser resigned = users.staff(Role.NURSING_STAFF);
        profiles.status(resigned.staffId(), StaffStatus.RESIGNED);
        List<Long> residents = List.of(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId());
        // Người cao tuổi thứ ba không có Điều dưỡng phụ trách; thứ hai được giao cho người Nghỉ việc
        Map<Long, Long> nurseOf = Map.of(
                residents.get(0), nurses.get(0).staffId(),
                residents.get(1), resigned.staffId());
        nurseOf.forEach((resident, staff) -> assignments.assign(
                staff,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        null,
                        time.now().minus(Duration.ofHours(1)),
                        null)));
        List<Long> present = new ArrayList<>();
        for (TestUser u : List.of(lead1, lead2, nurses.get(0), nurses.get(1), caregivers.get(0), caregivers.get(1))) {
            present.add(u.staffId());
        }
        present.add(resigned.staffId());
        // Người vắng ca không còn trong danh sách có mặt của ca đang diễn ra (008 FR-020)
        shifts.shift(
                new ShiftView(
                        TestUsers.nextId(),
                        id,
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(7)),
                        "IN_PROGRESS",
                        lead1.staffId()),
                present);
        fm.ifPresentOrElse(f -> duties.floorManager(id, f.staffId()), () -> {});
        Floor f = new Floor(id, lead1, lead2, fm, nurses, caregivers, absent, resigned, residents, nurseOf);
        setLead(f, lead1);
        return f;
    }

    private void setLead(Floor f, TestUser lead) {
        currentLead.put(f.id(), lead.staffId());
        duties.shiftLead(f.id(), lead.staffId());
        if (f.floorManager().isEmpty()) {
            duties.floorManagerSubstitute(f.id(), lead.staffId());
        }
    }

    /** Bậc thay mặc định của bảng tính tay: Trưởng tầng được giao, không có thì Người phụ trách ca (Q-90). */
    private Set<Long> floorManagerOrSubstitute(Floor f) {
        return Set.of(f.floorManager().map(TestUser::staffId).orElse(currentLead.get(f.id())));
    }

    private Set<Long> expected(RecipientGroup g, Floor f, TestUser doctor, TestUser provisional) {
        return switch (g) {
            case RecipientGroup.AssignedNurse n -> {
                Long nurse = f.nurseOf().get(n.residentId());
                yield nurse != null && nurse != f.resigned().staffId()
                        ? Set.of(nurse)
                        : Set.of(currentLead.get(f.id()));
            }
            case RecipientGroup.ShiftLead l -> Set.of(currentLead.get(f.id()));
            case RecipientGroup.FloorManager m -> floorManagerOrSubstitute(f);
            case RecipientGroup.OnShiftStaff o -> {
                Set<Long> out = new LinkedHashSet<>();
                if (o.roles().isEmpty() || o.roles().contains(Role.NURSING_STAFF)) {
                    out.add(f.lead1().staffId());
                    out.add(f.lead2().staffId());
                    f.nurses().forEach(u -> out.add(u.staffId()));
                }
                if (o.roles().isEmpty() || o.roles().contains(Role.CAREGIVER)) {
                    f.caregivers().forEach(u -> out.add(u.staffId()));
                }
                yield out;
            }
            case RecipientGroup.OnDutyDoctor d -> Set.of(doctor.staffId());
            case RecipientGroup.Staff s -> Set.of(provisional.staffId());
            default -> throw new IllegalStateException(g.toString());
        };
    }

    private RecipientGroup group(Random random, Floor f, TestUser provisional) {
        return switch (random.nextInt(6)) {
            case 0 -> new RecipientGroup.AssignedNurse(f.residents().get(random.nextInt(3)), DEFAULT);
            case 1 -> new RecipientGroup.ShiftLead(f.id(), DEFAULT);
            case 2 -> new RecipientGroup.FloorManager(f.id(), DEFAULT);
            case 3 -> new RecipientGroup.OnShiftStaff(f.id(), ROLE_FILTERS.get(random.nextInt(3)), Set.of(), DEFAULT);
            case 4 -> new RecipientGroup.OnDutyDoctor(DEFAULT);
            default -> new RecipientGroup.Staff(provisional.staffId(), "TAM_NHAN");
        };
    }

    private NotificationRequest request(int i, Floor f, Optional<Long> resident, List<RecipientGroup> groups) {
        return new NotificationRequest(
                "M04",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M04.TEST", "sc004-" + i),
                "sc004-" + f.id() + "-" + i,
                Optional.of(NotificationLevel.MEDIUM),
                resident,
                Optional.of(f.id()),
                groups,
                List.of(new ContentPart(InformationType.GENERAL, "Nội dung")),
                Optional.of("Rút gọn"),
                Optional.of("Tiêu đề"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }

    @Test
    void SC_004_twoHundredRequests_matchHandComputedTable_noDuplicates() {
        TestUsers users = new TestUsers(jdbc, time);
        List<Floor> floors = List.of(floor(users, true), floor(users, true), floor(users, true), floor(users, false));
        TestUser doctor = users.staff(Role.DOCTOR);
        TestUser offDutyDoctor = users.staff(Role.DOCTOR);
        TestUser provisional = users.staff(Role.NURSING_STAFF);
        long facilityShift = TestUsers.nextId();
        shifts.shift(
                        new ShiftView(
                                facilityShift,
                                null,
                                time.now().minus(Duration.ofHours(1)),
                                time.now().plus(Duration.ofHours(7)),
                                "IN_PROGRESS",
                                null),
                        List.of(doctor.staffId(), offDutyDoctor.staffId()))
                .onDutyDoctor(facilityShift, doctor.staffId());

        Random random = new Random(2026_10_26L);
        int compared = 0;
        for (int i = 0; i < 200; i++) {
            Floor f = floors.get(random.nextInt(floors.size()));
            if (i % 25 == 24) {
                // Đổi Người phụ trách ca giữa ca: người nhận xác định lại tại thời điểm gửi
                setLead(f, currentLead.get(f.id()) == f.lead1().staffId() ? f.lead2() : f.lead1());
            }
            List<RecipientGroup> groups = new ArrayList<>();
            int count = 1 + random.nextInt(3);
            for (int k = 0; k < count; k++) {
                groups.add(group(random, f, provisional));
            }
            Optional<Long> resident = groups.stream()
                    .filter(g -> g instanceof RecipientGroup.AssignedNurse)
                    .map(g -> ((RecipientGroup.AssignedNurse) g).residentId())
                    .findFirst();
            Set<Long> want = new LinkedHashSet<>();
            groups.forEach(g -> want.addAll(expected(g, f, doctor, provisional)));

            List<ResolvedRecipient> got = resolver.resolve(
                            RequestValidation.validate(request(i, f, resident, groups)), time.now())
                    .recipients();
            List<Long> ids = got.stream().map(ResolvedRecipient::id).toList();

            assertThat(ids).as("yêu cầu %d, nhóm %s", i, groups).containsExactlyInAnyOrderElementsOf(want);
            assertThat(ids).as("yêu cầu %d trùng người nhận", i).doesNotHaveDuplicates();
            assertThat(ids).doesNotContain(f.absent().staffId(), f.resigned().staffId(), offDutyDoctor.staffId());
            compared++;
        }
        assertThat(compared).isEqualTo(200);
    }
}
