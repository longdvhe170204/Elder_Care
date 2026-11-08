package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.spi.DayCareCalendarPort.CalendarView;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.care.internal.session.service.LateReturnConsentService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.admission.*;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.resident.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import({
    SwitchableUserConfig.class,
    FakeM15Config.class,
    FakeFamilyRelationshipsConfig.class,
    FakeM01Config.class,
    FakeM02Config.class,
    FakeM02PortsConfig.class,
    FakeM04Config.class,
    FakeLocations.Config.class
})
class FR_016a_DongYVeMuonIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeFamilyRelationships relationships;

    @Autowired
    FakeRepresentatives representatives;

    @Autowired
    FakeContracts contracts;

    @Autowired
    FakeContractSchedules schedules;

    @Autowired
    FakeDayCareCalendar calendar;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeLocations locations;

    @Autowired
    DayCareAttendanceApi attendance;

    @Autowired
    LateReturnConsentService consentWork;

    @Autowired
    TestClockControl clock;

    private static final String FAMILY_CONSENTS = "/api/v1/family-portal/late-return-consents";
    private static final String OFFICE_CONSENTS = "/api/v1/late-return-consents";
    private static final LocalTime SCHEDULED_RETURN = LocalTime.of(15, 0);
    private TestUsers.TestUser manager;
    private TestUsers.TestUser family;
    private long resident;
    private long representative;
    private long activity;
    private LocalDate date;

    @BeforeEach
    void fixture() throws Exception {
        date = time.today().plusDays(1);
        resident = TestUsers.nextId();
        long floor = TestUsers.nextId();
        representative = TestUsers.nextId();
        var users = new TestUsers(jdbc, time);
        manager = users.staff(Role.FLOOR_MANAGER);
        family = users.family(representative);
        user.set(manager.user());
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(3600),
                        null));
        residents.resident(resident, "[Người cao tuổi kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        contracts.period(
                resident, new ResidenceTypePeriod(ResidenceType.DAY_CARE, date.minusDays(2), Optional.empty()));
        schedules.schedule(resident, new AttendanceScheduleView(date, LocalTime.of(8, 0), SCHEDULED_RETURN));
        calendar.day(resident, date, new CalendarView(false, null, null));
        locations.floor(floor, null).daycareRestLocation(new LocationRef("FLOOR", floor));
        relationships.representative(relationships.relate(representative, resident));
        representatives.representative(resident, representative);
        long type = catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Sinh hoạt " + floor,
                                null,
                                0,
                                new ActivityTypeAttributes(true, false, List.of())))
                .id();
        activity = id(postTo(
                "/api/v1/activities",
                "{\"name\":\"Cờ tướng\",\"typeId\":" + type + ",\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":4,\"groupActivity\":true}"));
    }

    @Test
    void FR_016a_consentRegistrationWithdrawalAndOfficeUse() throws Exception {
        TableRows.each(List.of("WITHDRAW", "REPRESENTATIVE", "OFFICE"), scenario -> {
            fixture();
            switch (scenario) {
                case "WITHDRAW" -> FR_016a_withdrawingOneConsentKeepsTheReturnTimeOfTheLatestRemainingRegistration();
                case "REPRESENTATIVE" -> FR_016a_requiresConsentForTheExactSessionAndAnActiveRepresentative();
                case "OFFICE" -> FR_016a_officeRecordsTheSignedFormAndRegisteredConsentIsUsedAtSessionStart();
            }
        });
    }

    private void FR_016a_withdrawingOneConsentKeepsTheReturnTimeOfTheLatestRemainingRegistration() throws Exception {
        long early = session(LocalTime.of(15, 0), LocalTime.of(16, 0));
        long late = session(LocalTime.of(16, 30), LocalTime.of(17, 30));
        user.set(family.user());
        consent(early).andExpect(status().isCreated());
        consent(late).andExpect(status().isCreated());
        long earlyConsent = consentId(early);
        long lateConsent = consentId(late);
        assertThat(attendance.returnTimeOn(resident, date)).isEmpty();

        user.set(manager.user());
        register(early).andExpect(status().isCreated());
        register(late).andExpect(status().isCreated());
        assertThat(attendance.returnTimeOn(resident, date)).contains(LocalTime.of(17, 30));

        user.set(family.user());
        postTo(FAMILY_CONSENTS + "/" + lateConsent + "/withdraw", "").andExpect(status().isOk());
        assertThat(registrationStatus(late)).isEqualTo("CANCELLED");
        assertThat(registrationStatus(early)).isEqualTo("REGISTERED");
        assertThat(attendance.returnTimeOn(resident, date)).contains(LocalTime.of(16, 0));
        postTo(FAMILY_CONSENTS + "/" + earlyConsent + "/withdraw", "").andExpect(status().isOk());
        assertThat(attendance.returnTimeOn(resident, date)).contains(SCHEDULED_RETURN);
    }

    private void FR_016a_requiresConsentForTheExactSessionAndAnActiveRepresentative() throws Exception {
        long session = session(LocalTime.of(15, 0), LocalTime.of(16, 0));
        long otherRelative = TestUsers.nextId();
        relationships.relate(otherRelative, resident);
        user.set(new TestUsers(jdbc, time).family(otherRelative).user());
        consent(session).andExpect(status().isForbidden());
        user.set(manager.user());
        register(session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LATE_RETURN_CONSENT_REQUIRED"));
    }

    private void FR_016a_officeRecordsTheSignedFormAndRegisteredConsentIsUsedAtSessionStart() throws Exception {
        long session = session(LocalTime.of(15, 0), LocalTime.of(16, 0));
        var office = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF);
        long file = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO file(id,uuid,original_name,mime_type,size_bytes,sha256,uploaded_by,uploaded_at)"
                        + " VALUES(?,?,'dong-y.pdf','application/pdf',1,?,?,CURRENT_TIMESTAMP(6))",
                file,
                UUID.randomUUID().toString(),
                "0".repeat(64),
                office.accountId());
        String signed = "{\"residentId\":" + resident + ",\"sessionId\":" + session + ",\"representativeId\":"
                + representative + ",\"signedFileId\":" + file + "}";
        postTo(OFFICE_CONSENTS, signed).andExpect(status().isForbidden());
        user.set(office.user());
        postTo(OFFICE_CONSENTS, signed).andExpect(status().isCreated());
        long id = consentId(session);
        assertThat(jdbc.queryForObject("SELECT owner_id FROM file WHERE id=?", Long.class, file))
                .isEqualTo(id);

        user.set(manager.user());
        register(session).andExpect(status().isCreated());
        var startsAt = date.atTime(15, 0).atZone(BusinessTime.ZONE).toInstant();
        clock.setTo(startsAt);
        try {
            assertThat(consentWork.run(time.now())).isEqualTo(1);
            assertThat(consentWork.run(time.now())).isZero();
            assertThat(jdbc.queryForObject("SELECT status FROM simple_record WHERE id=?", String.class, id))
                    .isEqualTo("USED");
            user.set(office.user());
            postTo(OFFICE_CONSENTS + "/" + id + "/withdraw", "{\"reason\":\"Đại diện yêu cầu\"}")
                    .andExpect(status().isConflict());
            assertThat(registrationStatus(session)).isEqualTo("REGISTERED");
        } finally {
            clock.reset();
        }
    }

    private long session(LocalTime start, LocalTime end) throws Exception {
        return id(postTo(
                "/api/v1/activity-sessions",
                "{\"activityId\":" + activity + ",\"startsAt\":\""
                        + date.atTime(start).atZone(BusinessTime.ZONE).toInstant() + "\",\"endsAt\":\""
                        + date.atTime(end).atZone(BusinessTime.ZONE).toInstant() + "\"}"));
    }

    private ResultActions consent(long session) throws Exception {
        return postTo(FAMILY_CONSENTS, "{\"residentId\":" + resident + ",\"sessionId\":" + session + "}");
    }

    private ResultActions register(long session) throws Exception {
        return postTo("/api/v1/activity-sessions/" + session + "/registrations", "{\"residentId\":" + resident + "}");
    }

    private long consentId(long session) {
        return jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind='M04.LATE_RETURN_CONSENT' AND resident_id=? AND entity_id=?",
                Long.class,
                resident,
                session);
    }

    private String registrationStatus(long session) {
        return jdbc.queryForObject(
                "SELECT status FROM activity_registration WHERE session_id=? AND resident_id=?",
                String.class,
                session,
                resident);
    }

    private ResultActions postTo(String path, String body) throws Exception {
        return mvc.perform(post(path)
                .header("Idempotency-Key", "late-return-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static long id(ResultActions result) throws Exception {
        var uri =
                result.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
    }
}
