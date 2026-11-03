package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T053/T055: chỉ miễn cách ly, giữ các điều kiện còn lại và rollback cả hai giường. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@org.springframework.test.context.event.RecordApplicationEvents
class BR_M03_21_ControlledTransferIT extends StructureTestSupport {
    @Autowired
    org.springframework.test.context.event.ApplicationEvents publishedEvents;

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus infection;

    long resident, allocation, target, targetRoom, doctor;

    @BeforeEach
    void allocationFixture() {
        infection.reset();
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        doctor = new TestUsers(jdbc, time).staff(Role.DOCTOR).user().accountId();
        resident = TestUsers.nextId();
        allocation = TestUsers.nextId();
        targetRoom = TestUsers.nextId();
        target = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now().minusSeconds(60)),
                Timestamp.from(time.now().minusSeconds(60)),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy,isolated) VALUES(?,?,?,?,?,'UNRESTRICTED',TRUE)",
                targetRoom,
                floor,
                type,
                "R" + targetRoom,
                "Phòng cách ly");
        jdbc.update("INSERT INTO room_allowed_care_level VALUES(?,'BASIC')", targetRoom);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'T1')", target, targetRoom);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source,awaiting_doctor_confirmation) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT',TRUE)",
                allocation,
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(60)));
    }

    @Test
    void BR_M03_21_officeListsDoctorsWithoutReadingStaffAccountAdministrationData() throws Exception {
        mvc.perform(get("/api/v1/bed-allocations/prescribing-doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + doctor + ")]").isNotEmpty());
        mvc.perform(get("/api/v1/accounts").param("type", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void FR_024a_onlyDoctorReadsPendingStays() throws Exception {
        mvc.perform(get("/api/v1/room-isolation/pending-stays")).andExpect(status().isForbidden());
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc.perform(get("/api/v1/room-isolation/pending-stays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + allocation + ")]").isNotEmpty());
    }

    @Test
    void FR_024a_liftRetainsFlagInsideCordonAndClearsItWhenUnblocked() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        jdbc.update("UPDATE room SET isolated=TRUE WHERE id=?", room);
        infection.cordon(
                time.now().minusSeconds(1),
                new com.eldercare.backend.roombedallocation.api.spi.CordonedArea(
                        java.util.Set.of(), java.util.Set.of(area)));
        mvc.perform(post("/api/v1/rooms/" + room + "/lift-isolation")
                        .contentType("application/json")
                        .content("{\"reason\":\"Gỡ phòng, vùng còn chặn\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT awaiting_doctor_confirmation FROM bed_allocation WHERE id=?",
                        Boolean.class,
                        allocation))
                .isTrue();
        infection.reset();
        mvc.perform(post("/api/v1/rooms/" + room + "/isolate")
                        .contentType("application/json")
                        .content("{\"reason\":\"Đợt mới\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/rooms/" + room + "/lift-isolation")
                        .contentType("application/json")
                        .content("{\"reason\":\"Hết chặn\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT awaiting_doctor_confirmation FROM bed_allocation WHERE id=?",
                        Boolean.class,
                        allocation))
                .isFalse();
    }

    @Test
    void FR_032_absentResidentKeepsHoldAndRemainingDeadlineOnTarget() throws Exception {
        var deadline = time.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.update("UPDATE resident SET status='TEMPORARILY_ABSENT' WHERE id=?", resident);
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=?,hold_until=? WHERE id=?",
                resident,
                Timestamp.from(deadline),
                bed);
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/infection-control-transfer")
                        .contentType("application/json")
                        .content(body(doctor)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("HELD");
        assertThat(jdbc.queryForObject("SELECT hold_until FROM bed WHERE id=?", Timestamp.class, target)
                        .toInstant())
                .isEqualTo(deadline);
    }

    private String body(long doctorId) {
        return "{\"newBedId\":" + target + ",\"prescribingDoctorId\":" + doctorId
                + ",\"reason\":\"Chỉ định kiểm soát lây nhiễm\"}";
    }

    @Test
    void BR_M03_21_prescribedTransferBypassesIsolationOnlyAndClosesOldAllocation() throws Exception {
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/infection-control-transfer")
                        .contentType("application/json")
                        .content(body(doctor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousAllocation.status").value("CLOSED"))
                .andExpect(jsonPath("$.newAllocation.status").value("ACTIVE"))
                .andExpect(jsonPath("$.newAllocation.previousAllocationId").value(allocation));
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForObject(
                        "SELECT prescribing_doctor_id FROM bed_allocation WHERE resident_id=? AND status='ACTIVE'",
                        Long.class,
                        resident))
                .isEqualTo(doctor);
        var transfers = publishedEvents.stream(com.eldercare.backend.roombedallocation.api.events.BedTransferred.class)
                .filter(event -> event.residentId() == resident)
                .toList();
        assertThat(transfers).hasSize(1);
        assertThat(transfers.getFirst().previousAllocationId()).isEqualTo(allocation);
        assertThat(transfers.getFirst().previousFloorId()).isEqualTo(floor);
        assertThat(transfers.getFirst().nextFloorId()).isEqualTo(floor);
        assertThat(jdbc.queryForObject(
                        "SELECT doctor_confirmation_requested_at FROM bed_allocation WHERE id=?",
                        Timestamp.class,
                        transfers.getFirst().nextAllocationId()))
                .isNotNull();
        assertThat(jdbc.queryForObject(
                        "SELECT previous_allocation_id FROM bed_allocation WHERE id=?",
                        Long.class,
                        transfers.getFirst().nextAllocationId()))
                .isEqualTo(allocation);
    }

    @Test
    void BR_M03_21_invalidDoctorLeavesBothBedsAndAllocationUnchanged() throws Exception {
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/infection-control-transfer")
                        .contentType("application/json")
                        .content(body(user.accountId())))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, allocation))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("EMPTY");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LOCKED", "INACTIVE", "TEMPORARILY_LOCKED"})
    void BR_M03_21_nonActiveDoctorCannotBeSelectedOrPrescribe(String status) throws Exception {
        jdbc.update(
                "UPDATE account SET status=?,lock_cause=?,locked_until=? WHERE id=?",
                status,
                status.equals("LOCKED") ? "MANUAL" : null,
                status.equals("TEMPORARILY_LOCKED") ? Timestamp.from(time.now().plusSeconds(3600)) : null,
                doctor);
        mvc.perform(get("/api/v1/bed-allocations/prescribing-doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + doctor + ")]").isEmpty());
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/infection-control-transfer")
                        .contentType("application/json")
                        .content(body(doctor)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, allocation))
                .isEqualTo("ACTIVE");
    }

    @Test
    void FR_024a_nonActiveDoctorCannotConfirmOrListPendingStays() throws Exception {
        var actor = new TestUsers(jdbc, time).staff(Role.DOCTOR).user();
        user.set(actor);
        jdbc.update(
                "UPDATE account SET status='TEMPORARILY_LOCKED',locked_until=? WHERE id=?",
                Timestamp.from(time.now().plusSeconds(3600)),
                actor.accountId());
        mvc.perform(get("/api/v1/room-isolation/pending-stays")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/confirm-isolated-stay")
                        .contentType("application/json")
                        .content("{\"reason\":\"Ở lại\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_010_genderConditionStillBlocksPrescribedTransfer() throws Exception {
        jdbc.update("UPDATE room SET gender_policy='MALE' WHERE id=?", targetRoom);
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/infection-control-transfer")
                        .contentType("application/json")
                        .content(body(doctor)))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, allocation))
                .isEqualTo("ACTIVE");
    }

    @Test
    void FR_024a_onlyActiveDoctorCanConfirmStay() throws Exception {
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/confirm-isolated-stay")
                        .contentType("application/json")
                        .content("{\"reason\":\"Ở lại\"}"))
                .andExpect(status().isForbidden());
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/confirm-isolated-stay")
                        .contentType("application/json")
                        .content("{\"reason\":\"Ở lại\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT awaiting_doctor_confirmation FROM bed_allocation WHERE id=?",
                        Boolean.class,
                        allocation))
                .isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT confirming_doctor_id FROM bed_allocation WHERE id=?", Long.class, allocation))
                .isEqualTo(user.accountId());
    }
}
