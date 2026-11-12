package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T036: hợp đồng chuyển thường, lệnh thật và ràng buộc quyền/lý do. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class FR_028_BedTransferHttpIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Autowired
    FakeAllocationResidencySource residency;

    long resident, allocation, target, transferReason;

    @BeforeEach
    void transferFixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        allocation = TestUsers.nextId();
        target = TestUsers.nextId();
        transferReason = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now().minusSeconds(3600)),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G-HTTP')", target, room);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT')",
                allocation,
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(3600)));
        jdbc.update(
                "INSERT INTO bed_transfer_reason(id,code,name,medical_safety) VALUES(?,?,?,FALSE)",
                transferReason,
                "TR" + transferReason,
                "Theo nhu cầu " + transferReason);
    }

    private org.springframework.test.web.servlet.ResultActions transfer(String extra, String reason) throws Exception {
        return mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/transfer")
                .contentType("application/json")
                .content("{\"newBedId\":" + target + ",\"transferReasonId\":" + transferReason + ",\"reason\":\""
                        + reason + "\"" + extra + "}"));
    }

    @Test
    void FR_028_normalTransferReturnsBothAllocationsAndUpdatesBothBeds() throws Exception {
        transfer("", "Chuyển theo nhu cầu")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousAllocation.id").value(allocation))
                .andExpect(jsonPath("$.previousAllocation.status").value("CLOSED"))
                .andExpect(jsonPath("$.newAllocation.status").value("ACTIVE"))
                .andExpect(jsonPath("$.newAllocation.location.bedId").value(target));
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("OCCUPIED");
    }

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeAssignments assignments;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    private void floorManagerOf(long managedFloor) {
        var manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        user.set(manager.user());
        assignments.assign(
                manager.staffId(),
                new com.eldercare.backend.administration.account.api.spi.AssignmentView(
                        com.eldercare.backend.administration.account.api.spi.AssignmentKind.FLOOR_MANAGER,
                        managedFloor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(60),
                        null));
        duties.floorManager(managedFloor, manager.staffId());
    }

    /** UC-21 (BR-M03-20 cùng cách kiểm): Trưởng tầng chuyển giường trong tầng mình, tầng khác bị chặn. */
    @Test
    void UC_21_floorManagerTransfersOnlyWithinOwnFloor() throws Exception {
        floorManagerOf(TestUsers.nextId());
        transfer("", "Chuyển theo nhu cầu").andExpect(status().isForbidden());
        floorManagerOf(floor);
        transfer("", "Chuyển theo nhu cầu")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAllocation.location.bedId").value(target));
    }
}
