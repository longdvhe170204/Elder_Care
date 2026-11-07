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
}
