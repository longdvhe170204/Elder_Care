package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 004 T043 (US5 #7; FR-046): người cao tuổi chỉ có một yêu cầu Đã duyệt (chờ hiệu lực) cho mỗi thuộc tính; yêu cầu thứ hai
 * cùng thuộc tính bị chặn khi duyệt (409) cho tới khi yêu cầu đầu bị hủy hoặc đã áp dụng; thuộc tính khác không vướng.
 * {@code UNIQUE} của cơ sở dữ liệu là lưới an toàn.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_046_DuplicateAttributeIT extends ChangeTestBase {

    private long secondCareLevelRequest(long contractId, LocalDate effective) throws Exception {
        long id = createChange(request(contractId, "[\"CARE_LEVEL\"]", "{\"careLevel\": \"SPECIAL\"}", effective))
                .get("approvalRequestId")
                .asLong();
        submit(id);
        return id;
    }

    @Test
    void FR_046_aSecondApprovedRequestForTheSameAttribute_isBlockedAt409() throws Exception {
        Setup s = activeSetup();
        service("CARE", "Chăm sóc đặc biệt", "SPECIAL", 12_000_000);
        createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(5)));
        long second = secondCareLevelRequest(s.contractId(), today().plusDays(6));

        user.set(manager.user());
        MvcResult blocked = approveRaw(second).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("DUPLICATE_PENDING_ATTRIBUTE");
        assertThat(requestStatus(second)).isEqualTo("PENDING_APPROVAL");
        assertThat(addenda(s.contractId())).hasSize(1);
    }

    @Test
    void FR_046_anotherAttribute_isNotBlocked() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(5)));

        createAndApprove(request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + injection + ", \"action\": \"ADD\"}]}",
                today().plusDays(5)));

        assertThat(addenda(s.contractId())).hasSize(2);
    }

    @Test
    void FR_046_cancellingTheFirstRequest_releasesTheAttribute_andClosesItsAddendum() throws Exception {
        Setup s = activeSetup();
        service("CARE", "Chăm sóc đặc biệt", "SPECIAL", 12_000_000);
        long first = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(5)));
        long second = secondCareLevelRequest(s.contractId(), today().plusDays(6));

        user.set(manager.user());
        generic(first, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        awaitAddendumStatus(first, "NOT_APPLIED");
        approveRaw(second).andExpect(status().isOk());

        assertThat(requestStatus(second)).isEqualTo("APPROVED");
        assertThat(addenda(s.contractId())).hasSize(2);
    }

    @Test
    void FR_046_afterTheFirstIsApplied_theAttributeIsFreeAgain() throws Exception {
        Setup s = activeSetup();
        service("CARE", "Chăm sóc đặc biệt", "SPECIAL", 12_000_000);
        createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(1)));
        long second = secondCareLevelRequest(s.contractId(), today().plusDays(2));
        applyDueOn(today().plusDays(1));

        user.set(manager.user());
        approveRaw(second).andExpect(status().isOk());
    }

    @Test
    void FR_046_twoApprovalsAtTheSameTime_exactlyOneWins_andTheOtherStaysPending() throws Exception {
        Setup s = activeSetup();
        service("CARE", "Chăm sóc đặc biệt", "SPECIAL", 12_000_000);
        long first = createChange(request(
                        s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(first);
        long second = secondCareLevelRequest(s.contractId(), today().plusDays(6));
        String firstTag = requestEtag(first);
        String secondTag = requestEtag(second);
        user.set(manager.user());
        SecurityContext security = TestSecurityContextHolder.getContext();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> a = pool.submit(() -> approveAs(security, start, first, firstTag));
            Future<Integer> b = pool.submit(() -> approveAs(security, start, second, secondTag));
            start.countDown();

            assertThat(List.of(a.get(60, TimeUnit.SECONDS), b.get(60, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }

        assertThat(addenda(s.contractId())).hasSize(1);
        assertThat(List.of(requestStatus(first), requestStatus(second)))
                .containsExactlyInAnyOrder("APPROVED", "PENDING_APPROVAL");
    }

    private int approveAs(SecurityContext security, CountDownLatch start, long requestId, String etag)
            throws Exception {
        TestSecurityContextHolder.setContext(security);
        start.await();
        return mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Đồng ý\"}"))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    @Test
    void FR_046_theDatabaseRejectsTwoPendingRowsForTheSameAttribute() throws Exception {
        Setup s = activeSetup();
        String insert = "INSERT INTO change_request_attribute (approval_request_id, contract_id, resident_id,"
                + " attribute, pending_effective) VALUES (?, ?, ?, 'BED', TRUE)";
        jdbc.update(insert, 9_100_001L, s.contractId(), s.residentId());

        assertThatThrownBy(() -> jdbc.update(insert, 9_100_002L, s.contractId(), s.residentId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(
                "INSERT INTO change_request_attribute (approval_request_id, contract_id, resident_id, attribute)"
                        + " VALUES (?, ?, ?, 'BED')",
                9_100_003L,
                s.contractId(),
                s.residentId());
    }

    private void awaitAddendumStatus(long requestId, String expected) {
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "SELECT status FROM contract_addendum WHERE approval_request_id = ?",
                                String.class,
                                requestId))
                        .isEqualTo(expected));
    }
}
