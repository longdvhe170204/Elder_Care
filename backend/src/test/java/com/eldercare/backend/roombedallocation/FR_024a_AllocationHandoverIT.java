package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.roombedallocation.api.AllocationHandoverApi;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn cảnh báo thật003 và consumer008 qua HTTP; không import fake API bàn giao giường. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class FR_024a_AllocationHandoverIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvc mvc;

    @Autowired
    AllocationCommandStore commands;

    @Autowired
    TestClockControl clock;

    long allocation, resident;
    long committedShift;
    Instant requested, began;

    @BeforeEach
    void allocationFixture() {
        requested = time.now().minusSeconds(30).truncatedTo(ChronoUnit.MICROS);
        began = requested.minusSeconds(3600);
        allocation = TestUsers.nextId();
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source,awaiting_doctor_confirmation) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT',TRUE)",
                allocation,
                resident,
                bed,
                Timestamp.from(began));
    }

    private AllocationHandoverApi source() {
        return context.getBean(AllocationHandoverApi.class);
    }

    @AfterEach
    void resetClock() {
        if (committedShift != 0) {
            jdbc.update("UPDATE work_shift SET status='CANCELLED' WHERE id=?", committedShift);
            jdbc.update(
                    "UPDATE bed_allocation SET status='CLOSED',end_at=?,awaiting_doctor_confirmation=FALSE WHERE id=?",
                    Timestamp.from(time.now()),
                    allocation);
        }
        clock.reset();
    }

    private void requestedAt() {
        jdbc.update(
                "UPDATE bed_allocation SET doctor_confirmation_requested_at=? WHERE id=?",
                Timestamp.from(requested),
                allocation);
    }

    @Test
    void FR_024a_sourceUsesRealFlagTimeAndAllocationIdentity() {
        var api = source();
        requestedAt();
        assertThat(api.atFloor(floor, requested.plusSeconds(1))).singleElement().satisfies(row -> {
            assertThat(row.id()).isEqualTo(allocation);
            assertThat(row.residentId()).isEqualTo(resident);
            assertThat(row.roomId()).isEqualTo(room);
            assertThat(row.description()).isNotBlank();
            assertThat(row.createdAt()).isEqualTo(requested).isNotEqualTo(began);
        });
        assertThat(api.atFloor(floor, requested.minusNanos(1000))).isEmpty();
        assertThat(api.atFloor(TestUsers.nextId(), requested.plusSeconds(1))).isEmpty();
    }

    @Test
    void FR_024a_clearedOrInactiveAllocationIsNotAnOpenHandoverAlert() {
        var api = source();
        jdbc.update("UPDATE bed_allocation SET awaiting_doctor_confirmation=FALSE WHERE id=?", allocation);
        assertThat(api.atFloor(floor, requested.plusSeconds(1))).isEmpty();
        jdbc.update(
                "UPDATE bed_allocation SET awaiting_doctor_confirmation=TRUE,status='FUTURE' WHERE id=?", allocation);
        assertThat(api.atFloor(floor, requested.plusSeconds(1))).isEmpty();
    }

    @Test
    void FR_024a_legacyFlagDoesNotInventACreationTimeFromAllocationStart() {
        assertThat(source().atFloor(floor, requested.plusSeconds(1)))
                .singleElement()
                .satisfies(row -> assertThat(row.createdAt()).isNull());
    }

    @Test
    void FR_024a_settingAnAlreadyPendingFlagKeepsItsOriginalRequestTime() {
        var api = source();
        jdbc.update("UPDATE bed_allocation SET awaiting_doctor_confirmation=FALSE WHERE id=?", allocation);
        commands.awaitingDoctor(allocation);
        Instant first = api.atFloor(floor, time.now()).getFirst().createdAt();
        assertThat(first).isNotNull();
        clock.advanceBy(java.time.Duration.ofSeconds(10));
        commands.awaitingDoctor(allocation);
        assertThat(api.atFloor(floor, time.now()))
                .singleElement()
                .satisfies(row -> assertThat(row.createdAt()).isEqualTo(first));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void FR_039_staffingSourceEndpointReadsTheRealAllocationProvider() throws Exception {
        source();
        requestedAt();
        long staff = user.staffId().orElseThrow();
        long template = TestUsers.nextId(),
                schedule = TestUsers.nextId(),
                shift = TestUsers.nextId(),
                handover = TestUsers.nextId();
        committedShift = shift;
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES(?,'[Kiểm nguồn bàn giao]','NURSING_HOME_MANAGER',?)",
                staff,
                user.accountId());
        jdbc.update(
                "INSERT INTO shift_template(id,name,start_time,end_time,scope_type) VALUES(?,'[Ca kiểm nguồn]','07:00','15:00','FLOOR')",
                template);
        jdbc.update(
                "INSERT INTO shift_schedule(id,month,scope_type,scope_id,status) VALUES(?,'2026-10','FLOOR',?,'PUBLISHED')",
                schedule,
                floor);
        jdbc.update(
                "INSERT INTO work_shift(id,schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,status,requires_handover) VALUES(?,?,?,?,?,'FLOOR',?,'IN_PROGRESS',TRUE)",
                shift,
                schedule,
                template,
                Timestamp.from(began),
                Timestamp.from(requested.plusSeconds(3600)),
                floor);
        jdbc.update(
                "INSERT INTO shift_handover(id,shift_id,handover_staff_id,created_by,created_at) VALUES(?,?,?,?,?)",
                handover,
                shift,
                staff,
                user.accountId(),
                Timestamp.from(requested));
        mvc.perform(get("/api/v1/handovers/{id}/source-records", handover))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sourceType == 'M03.ALLOCATION' && @.sourceId == '" + allocation
                                + "' && @.residentId == " + resident + ")]")
                        .isNotEmpty())
                .andExpect(jsonPath("$[?(@.sourceType == 'M03.ALLOCATION' && @.sourceId == '" + allocation
                                + "')].dataUnavailable")
                        .value(org.hamcrest.Matchers.contains(false)));
    }
}
