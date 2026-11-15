package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * T034, T036 nhánh nhập bù (FR-027, Q-43): lùi quá CFG-M03-07 thì 202 kèm yêu cầu {@code M03.NHAP_BU_PHAN_BO}, chỉ người
 * có {@code BED_ALLOCATION} APPROVE (Quản lý viện) duyệt; duyệt thì áp dụng trong transaction áp dụng của 000, từ chối thì
 * không ghi. Không {@code @Transactional}: áp dụng chạy sau khi quyết định commit.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_027_BackfillApprovalIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    ApprovalRequestApi approvals;

    long resident, allocation, target, transferReason;
    com.eldercare.backend.administration.account.api.CurrentUser office;

    @BeforeEach
    void backfillFixture() {
        office = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user();
        user.set(office);
        resident = TestUsers.nextId();
        allocation = TestUsers.nextId();
        target = TestUsers.nextId();
        transferReason = TestUsers.nextId();
        var since = Timestamp.from(time.now().minus(5, ChronoUnit.DAYS));
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                since,
                since,
                office.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G-NB')", target, room);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT')",
                allocation,
                resident,
                bed,
                since);
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,code,name,status,attributes) VALUES(?,'M03.BED_TRANSFER_REASON','M03',?,?,'ACTIVE',JSON_OBJECT('medicalSafety',FALSE,'fixed',FALSE))",
                transferReason,
                "TR" + transferReason,
                "Theo nhu cầu " + transferReason);
    }

    private ResultActions transfer(Instant at) throws Exception {
        return mvc.perform(post("/api/v1/bed-allocations/" + allocation + "/transfer")
                .contentType("application/json")
                .header("Idempotency-Key", "nb-" + allocation + "-" + at.toEpochMilli())
                .content("{\"newBedId\":" + target + ",\"transferReasonId\":" + transferReason
                        + ",\"reason\":\"Chuyển ban đêm\",\"transferAt\":\"" + at + "\"}"));
    }

    private long lateRequest(Instant at) throws Exception {
        var body = transfer(at)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"approvalRequestId\":(\\d+).*", "$1"));
    }

    private ResultActions decide(long request, String command, String body) throws Exception {
        return mvc.perform(post("/api/v1/approval-requests/" + request + "/" + command)
                .contentType("application/json")
                .header("If-Match", "\"" + approvals.find(request).orElseThrow().version() + "\"")
                .content(body));
    }

    private String allocationStatus(long id) {
        return jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id);
    }

    private int opened() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=? AND bed_id=?",
                Integer.class,
                resident,
                target);
    }

    @Test
    void FR_027b_lateBackfillWaitsForManagerApprovalThenAppliesAtTransferTime() throws Exception {
        var at = time.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        long request = lateRequest(at);

        assertThat(approvals.find(request).orElseThrow().type()).isEqualTo("M03.NHAP_BU_PHAN_BO");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M03.BED_BACKFILL_REQUEST' AND content->>'$.approvalRequestId'=? AND content->>'$.residentId'=? AND entity_type='M03.BED_ALLOCATION' AND entity_id=? AND content->>'$.newBedId'=?",
                        Integer.class,
                        request,
                        resident,
                        allocation,
                        target))
                .isEqualTo(1);
        assertThat(allocationStatus(allocation)).isEqualTo("ACTIVE");
        assertThat(opened()).isZero();

        user.set(new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER).user());
        decide(request, "approve", "{}").andExpect(status().isForbidden());
        assertThat(approvals.find(request).orElseThrow().status()).isEqualTo("PENDING_APPROVAL");
        assertThat(allocationStatus(allocation)).isEqualTo("ACTIVE");

        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());

        decide(request, "approve", "{}").andExpect(status().isOk());

        assertThat(approvals.find(request).orElseThrow().status()).isEqualTo("APPLIED");
        assertThat(allocationStatus(allocation)).isEqualTo("CLOSED");
        assertThat(jdbc.queryForObject("SELECT end_at FROM bed_allocation WHERE id=?", Timestamp.class, allocation)
                        .toInstant())
                .isEqualTo(at);
        var next = jdbc.queryForMap(
                "SELECT status,backfilled,actual_recorded_at,opened_by_id FROM bed_allocation WHERE resident_id=? AND bed_id=?",
                resident,
                target);
        assertThat(next.get("status")).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                                "SELECT start_at FROM bed_allocation WHERE resident_id=? AND bed_id=?",
                                Timestamp.class,
                                resident,
                                target)
                        .toInstant())
                .isEqualTo(at);
        assertThat(next.get("backfilled")).isEqualTo(true);
        assertThat(next.get("actual_recorded_at")).isNotNull();
        assertThat(((Number) next.get("opened_by_id")).longValue()).isEqualTo(office.accountId());
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
    }
}
