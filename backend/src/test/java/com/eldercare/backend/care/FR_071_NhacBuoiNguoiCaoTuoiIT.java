package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.api.events.ActivityResidentReminder;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.care.internal.session.service.ResidentSessionReminders;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.StatusTestBase;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 014 T031, T033 (FR-071 dòng bổ sung Q-366; quickstart H3): nhắc chính người cao tuổi có tài khoản về buổi trong ngày đã
 * có tên, ở việc hằng ngày và khi đăng ký trong ngày, một lần mỗi đăng ký, mức Nhẹ; giờ yên tĩnh do 009 áp.
 */
@AutoConfigureMockMvc
@WithMockUser
@RecordApplicationEvents
@Import(FakeM04Config.class)
class FR_071_NhacBuoiNguoiCaoTuoiIT extends StatusTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ResidentSessionReminders reminders;

    @Autowired
    ApplicationEvents recorded;

    long floor;
    long session;
    TestUsers.TestUser manager;

    @BeforeEach
    void fixture() throws Exception {
        floor = TestUsers.nextId();
        manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
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
        long type = tx.execute(s -> catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Văn nghệ " + TestUsers.nextId(),
                                null,
                                0,
                                new ActivityTypeAttributes(true, false, List.of())))
                .id());
        long activity = id(mvc.perform(write(
                post("/api/v1/activities"),
                "{\"name\":\"Hát cùng nhau\",\"typeId\":" + type + ",\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":10,\"groupActivity\":true}")));
        // Buổi trong ngày, sau bây giờ một giờ (ngày nghiệp vụ hiện tại).
        Instant start = time.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        assumeSameDay(start);
        session = id(mvc.perform(write(
                post("/api/v1/activity-sessions"),
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + start + "\",\"endsAt\":\""
                        + start.plusSeconds(3600) + "\"}")));
    }

    void assumeSameDay(Instant start) {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                LocalDate.ofInstant(start.plusSeconds(3600), BusinessTime.ZONE).equals(time.today()),
                "buổi phải nằm trọn trong ngày nghiệp vụ hiện tại");
    }

    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder write(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body) {
        return request.header("Idempotency-Key", "remind-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    long id(ResultActions result) throws Exception {
        String location =
                result.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    long resident(boolean account) {
        long r = residentIn(ResidentStatus.IN_RESIDENCE);
        bedAllocations.allocate(
                r, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        if (account) new TestUsers(jdbc, time).resident(r);
        user.set(manager.user());
        return r;
    }

    List<ActivityResidentReminder> remindersFor(long resident) {
        return recorded.stream(ActivityResidentReminder.class)
                .filter(e -> e.residentId() == resident && e.sessionId() == session)
                .toList();
    }

    @Test
    void FR_071_theDailyStepRemindsResidentsWithAnAccountOncePerRegistration() {
        long withAccount = resident(true), without = resident(false);
        for (long r : List.of(withAccount, without))
            jdbc.update(
                    "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status) VALUES(?,?,'MANUAL',?,'REGISTERED')",
                    session,
                    r,
                    LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        tx.executeWithoutResult(t -> reminders.run(time.today(), time.now()));
        tx.executeWithoutResult(t -> reminders.run(time.today(), time.now()));
        assertThat(remindersFor(withAccount)).singleElement().satisfies(e -> {
            assertThat(e.level()).isEqualTo(NotificationLevel.LOW);
            assertThat(e.context().recipients()).containsExactly(new RecipientGroup.Resident(withAccount));
        });
        assertThat(remindersFor(without)).isEmpty();
        assertThat(reminders.dueDates(time.today())).containsExactly(time.today());
    }

    @Test
    void FR_071_registeringWithinTheDayRemindsImmediatelyAndTheDailyStepDoesNotRepeatIt() throws Exception {
        long r = resident(true);
        mvc.perform(write(
                        post("/api/v1/activity-sessions/" + session + "/registrations"), "{\"residentId\":" + r + "}"))
                .andExpect(status().isCreated());
        assertThat(remindersFor(r)).hasSize(1);
        tx.executeWithoutResult(t -> reminders.run(time.today(), time.now()));
        assertThat(remindersFor(r)).hasSize(1);
    }
}
