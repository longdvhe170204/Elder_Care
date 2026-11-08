package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** BR-M09-10, FR-019 → FR-025, FR-045: đổi ca người nhận đồng ý, trưởng tầng duyệt, một yêu cầu mở mỗi (ca, nhân viên). */
class BR_M09_10_ShiftSwapIT extends SwapTestBase {

    private String statusOf(long requestId) {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, requestId);
    }

    @Test
    void FR_022_mutualSwapIsAppliedAfterRecipientAgreesAndFloorManagerApproves() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long y = shift(base.plus(Duration.ofDays(1)).plus(Duration.ofHours(7)), 8, null, recipient);
        long id = createMutual(giver, x, recipient, y);
        assertThat(statusOf(id)).isEqualTo("AWAITING_RECIPIENT");

        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");

        send("/api/v1/shift-swaps/" + id + "/approve", head, null).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("APPLIED");
        assertThat(inShift(x, recipient)).isTrue();
        assertThat(inShift(x, giver)).isFalse();
        assertThat(inShift(y, giver)).isTrue();
        assertThat(inShift(y, recipient)).isFalse();
    }

    @Test
    void FR_024_takeOverMovesOnlyTheGiverShift() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isOk());
        send("/api/v1/shift-swaps/" + id + "/approve", manager, null).andExpect(status().isOk());
        assertThat(inShift(x, recipient)).isTrue();
        assertThat(inShift(x, giver)).isFalse();
    }

    @Test
    void BR_M09_03_overlapWithRecipientsOtherShiftBlocksTheRequest() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        shift(base.plus(Duration.ofHours(10)), 8, null, recipient);
        send(
                        "/api/v1/shift-swaps",
                        giver,
                        Map.of(
                                "kind", "TAKE_OVER",
                                "shiftXId", x,
                                "recipientId", recipient.staffId(),
                                "reason", "[Lý do riêng tư]"))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_019_oneOpenRequestPerShiftAndStaff() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        createTakeOver(giver, x, recipient);
        send(
                        "/api/v1/shift-swaps",
                        giver,
                        Map.of(
                                "kind", "TAKE_OVER",
                                "shiftXId", x,
                                "recipientId", third.staffId(),
                                "reason", "[Lý do riêng tư]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SHIFT_IN_ANOTHER_REQUEST"));
    }

    @Test
    void FR_019_recipientAlreadyInShiftIsRejected() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver, recipient);
        send(
                        "/api/v1/shift-swaps",
                        giver,
                        Map.of(
                                "kind", "TAKE_OVER",
                                "shiftXId", x,
                                "recipientId", recipient.staffId(),
                                "reason", "[Lý do riêng tư]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECIPIENT"));
    }

    @Test
    void FR_019_recipientNeedsASharedSystemRole() throws Exception {
        TestUsers.TestUser chef = person(Role.KITCHEN_STAFF);
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        send(
                        "/api/v1/shift-swaps",
                        giver,
                        Map.of(
                                "kind", "TAKE_OVER",
                                "shiftXId", x,
                                "recipientId", chef.staffId(),
                                "reason", "[Lý do riêng tư]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROLE_MISMATCH"));
    }

    @Test
    void FR_045_declineReasonIsVisibleOnlyToTheGiver() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        send("/api/v1/shift-swaps/" + id + "/decline", recipient, Map.of("reason", "[Lý do từ chối riêng tư]"))
                .andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("RECIPIENT_DECLINED");
        String forGiver = fetch("/api/v1/shift-swaps", giver).andReturn().getResponse().getContentAsString();
        String forRecipient = fetch("/api/v1/shift-swaps", recipient).andReturn().getResponse().getContentAsString();
        assertThat(forGiver).contains("[Lý do từ chối riêng tư]");
        assertThat(forRecipient).doesNotContain("[Lý do từ chối riêng tư]");
        // Khóa đã nhả: lập lại yêu cầu mới cho cùng ca được.
        createTakeOver(giver, x, third);
    }

    @Test
    void FR_022_giverCanCancelWhileOpen_andLockIsReleased() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        send("/api/v1/shift-swaps/" + id + "/cancel", giver, Map.of("reason", "[Rút lại]"))
                .andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        createTakeOver(giver, x, third);
    }

    @Test
    void FR_024_leadMovesToRecipientWhenEligible() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, giver.staffId(), giver);
        long id = createTakeOver(giver, x, recipient);
        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isOk());
        send("/api/v1/shift-swaps/" + id + "/approve", head, null).andExpect(status().isOk());
        Long lead = jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, x);
        assertThat(lead).isEqualTo(recipient.staffId());
    }

    @Test
    void FR_023_genericApproveEndpointIsBlockedForShiftSwaps() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isOk());
        send("/api/v1/shift-swaps/" + id + "/approve", third, null).andExpect(status().isForbidden());
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void FR_025_expiresWhenTheEarliestShiftHasStarted() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        clock.advanceBy(Duration.ofDays(4));
        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isConflict());
    }
}
