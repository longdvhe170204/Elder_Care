package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 004 T042 (FR-041, FR-045, FR-022, FR-040): khi duyệt, kiểm tra điều kiện của từng loại thay đổi; vi phạm thì chặn duyệt
 * (409) và liệt kê mọi điều kiện vi phạm, yêu cầu giữ Chờ duyệt. Điều kiện còn thỏa tại ngày hiệu lực thì mới áp dụng.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_041_CheckOnApprovalIT extends ChangeTestBase {

    private long inactiveService() {
        long id = service("INJECTION", "Dịch vụ ngừng", null, 100_000);
        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE id = ?", id);
        return id;
    }

    private long serviceWithoutPrice() {
        String name = "Dịch vụ chưa có giá " + System.nanoTime();
        jdbc.update(
                "INSERT INTO service (type, name, unit, billing_period) VALUES ('OTHER', ?, 'lần', 'OCCURRENCE')",
                name);
        return jdbc.queryForObject("SELECT id FROM service WHERE name = ?", Long.class, name);
    }

    @Test
    void FR_041_aViolation_blocksApproval_andListsEveryViolatedCondition() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);
        long stopped = inactiveService();
        long unpriced = serviceWithoutPrice();
        long id = createChange(request(
                        s.contractId(),
                        "[\"SERVICE\", \"DURATION\"]",
                        "{\"services\": [{\"serviceId\": " + stopped + ", \"action\": \"ADD\"},"
                                + " {\"serviceId\": " + unpriced + ", \"action\": \"ADD\"},"
                                + " {\"serviceId\": 987654, \"action\": \"REMOVE\"}], \"endDate\": \"" + today()
                                + "\"}",
                        effective))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked))
                .contains(
                        "SERVICE_NOT_EFFECTIVE",
                        "SERVICE_PRICE_MISSING",
                        "SERVICE_NOT_IN_CONTRACT",
                        "END_DATE_BEFORE_EFFECTIVE");
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(addenda(s.contractId())).isEmpty();
    }

    @Test
    void FR_041_careLevelEqualToTheCurrentOne_isBlocked() throws Exception {
        Setup s = activeSetup();
        long id = createChange(
                        request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"BASIC\"}", today().plusDays(1)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("CARE_LEVEL_UNCHANGED");
    }

    @Test
    void FR_041_careLevelWithoutACareServiceForTheNewLevel_isBlocked() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(
                        s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"SPECIAL\"}", today().plusDays(1)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("CARE_SERVICE_MISSING");
    }

    @Test
    void FR_022_durationChange_thatOverlapsAnotherActiveContract_isBlocked() throws Exception {
        long residentId = residentWithRepresentative();
        var first = body(residentId);
        first.put("residenceType", "SHORT_TERM_RESIDENTIAL");
        first.put("endDate", today().plusDays(10).toString());
        long contractId = activeContract(residentId, first);
        // Hợp đồng Hiệu lực kế tiếp bắt đầu ngay sau hợp đồng đầu (dựng thẳng vào bảng)
        long next = contracts.active(residentId, com.cambae.backend.m01.api.dto.CareLevel.BASIC, today().plusDays(20));
        jdbc.update("UPDATE contract SET status = 'ACTIVE' WHERE id = ?", next);
        long id = createChange(request(
                        contractId,
                        "[\"DURATION\"]",
                        "{\"endDate\": \"" + today().plusDays(30) + "\"}",
                        today().plusDays(1)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("CONTRACT_OVERLAP");
    }

    @Test
    void FR_045_contractNoLongerActiveOnTheEffectiveDate_failsTheApplication() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(2);
        long id = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));
        jdbc.update(
                "UPDATE contract SET status = 'TERMINATED', termination_reason = 'Chấm dứt thử' WHERE id = ?",
                s.contractId());

        applyDueOn(effective);

        assertThat(requestStatus(id)).isEqualTo("APPLY_FAILED");
        assertThat(versionCount(s.contractId())).isEqualTo(1);
    }
}
