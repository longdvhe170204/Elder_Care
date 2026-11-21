package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.ResidencyChangeOutcomeNotice;
import com.eldercare.backend.admission.api.events.ResidencyChangeSubmittedNotice;
import com.eldercare.backend.common.notification.RecipientGroup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * FR-043g (Q-421, R10, R16): người đại diện được báo khi yêu cầu liên quan tới gia đình được gửi duyệt (bỏ người đã xác
 * nhận), bị Từ chối, bị Hủy; thay đổi nhỏ không báo. Người nhận là nhóm người đại diện của người cao tuổi (009 phân giải
 * mọi người đại diện hiện tại).
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043g_RepresentativeNoticesIT extends ChangeTestBase {

    private long draftNonMinor(Setup s) throws Exception {
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        return createChange(request(
                        s.contractId(),
                        "[\"SERVICE\"]",
                        "{\"services\": [{\"serviceId\": " + extra + ", \"action\": \"ADD\"}]}",
                        today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
    }

    @Test
    void R10_submitted_rejected_notifyRepresentatives() throws Exception {
        Setup s = activeSetup();
        long id = draftNonMinor(s);
        submit(id);

        assertThat(events.stream(ResidencyChangeSubmittedNotice.class).filter(e -> e.requestId() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.notificationRequests().getFirst().recipientGroups())
                        .contains(new RecipientGroup.Representative(s.residentId())));

        user.set(manager.user());
        generic(id, "reject", "{\"comment\":\"Chưa phù hợp\"}").andExpect(status().isOk());
        assertThat(outcomes(id, ResidencyChangeOutcomeNotice.Outcome.REJECTED)).isEqualTo(1);
    }

    @Test
    void R10_cancelled_notifiesRepresentatives() throws Exception {
        Setup s = activeSetup();
        long id = draftNonMinor(s);

        user.set(office.user());
        generic(id, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        assertThat(outcomes(id, ResidencyChangeOutcomeNotice.Outcome.CANCELLED)).isEqualTo(1);
    }

    @Test
    void R10_minorChange_noRepresentativeNotice() throws Exception {
        Setup s = activeSetup();
        String removeMeal = "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"REMOVE\"}]}";
        long id = createChange(request(s.contractId(), "[\"SERVICE\"]", removeMeal, today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        submit(id);
        user.set(office.user());
        generic(id, "cancel", "{\"reason\":\"Không cần\"}").andExpect(status().isOk());

        assertThat(events.stream(ResidencyChangeSubmittedNotice.class).filter(e -> e.requestId() == id))
                .isEmpty();
        assertThat(outcomes(id, ResidencyChangeOutcomeNotice.Outcome.CANCELLED)).isZero();
    }

    @Test
    void R16_submittedNotice_excludesConfirmerAccountWhenAny() throws Exception {
        Setup s = activeSetup();
        long id = draftNonMinor(s);
        submit(id);
        ResidencyChangeSubmittedNotice notice = events.stream(ResidencyChangeSubmittedNotice.class)
                .filter(e -> e.requestId() == id)
                .findFirst()
                .orElseThrow();
        // Người đại diện dựng trong test chưa có tài khoản nên không có ai bị bỏ; có tài khoản thì causedBy là tài
        // khoản đó
        assertThat(notice.notificationRequests().getFirst().notifyCauser()).isFalse();
        assertThat(notice.notificationRequests().getFirst().causedBy())
                .isEqualTo(java.util.Optional.ofNullable(notice.confirmerAccountId()));
    }

    private long outcomes(long requestId, ResidencyChangeOutcomeNotice.Outcome outcome) {
        return events.stream(ResidencyChangeOutcomeNotice.class)
                .filter(e -> e.requestId() == requestId && e.outcome() == outcome)
                .count();
    }
}
