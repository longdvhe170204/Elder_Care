package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationTimers;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 013 T016 (US3; FR-017, FR-017a, FR-017b, BR-M12-04, CFG-M10-08, CFG-M12-05, CFG-M12-06; quickstart Q17 → Q20, Q22):
 * vòng đời xác nhận người nhận khác; duyệt thay của Quản lý viện chỉ khi đủ điều kiện, không sau từ chối, không tự lập
 * rồi tự duyệt; hết hạn. Việc theo phút của job M12 (chờ A, T006) cài thành phương thức có test ở đây.
 */
class FR_017a_ApproveOnBehalfIT extends ReturnTestBase {

    @Autowired
    RecipientConfirmationService confirmations;

    @Autowired
    RecipientConfirmationTimers timers;

    private String portal(long id, String command) {
        return "/api/v1/family-portal/recipient-confirmations/" + id + "/" + command;
    }

    @Test
    void FR_017_create_asksRepresentativeToConfirm_andListsByStatus() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");

        long id = confirmation(resident, "Trần Thị N", "999", phone);

        assertThat(confirmationStatus(id)).isEqualTo("PENDING_CONFIRMATION");
        getJson(tokenA, "/api/v1/recipient-confirmations?residentId=" + resident + "&status=PENDING_CONFIRMATION")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("PENDING_CONFIRMATION"))
                .andExpect(jsonPath("$[0].recipientName").value("Trần Thị N"))
                .andExpect(jsonPath("$[0].belongingIds[0]").value(phone));
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("RECIPIENT_CONFIRMATION_REQUESTED");
            assertThat(n.level()).contains(NotificationLevel.MEDIUM);
            assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.Representative(resident));
        });
        assertThat(representative).isPositive();
    }

    /**
     * Test luồng BF-14 2026-11-23 (chủ dự án duyệt đổi api-013): Quản lý viện xem được các lần liên hệ đã ghi trước khi
     * duyệt thay; duyệt sớm bị chặn với lời báo nói lúc nào được duyệt, không hiện mã FR, CFG.
     */
    @Test
    void FR_017a_listShowsContactAttempts_andEarlyApprovalSaysWhenAllowed() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012346");
        long id = confirmation(resident, "Trần Thị M", "998", phone);

        postJson(
                        tokenA,
                        "/api/v1/recipient-confirmations/" + id + "/contact-evidence",
                        "{\"familyMemberId\": " + representative + ", \"channel\": \"PHONE_CALL\", \"at\": \""
                                + time.now() + "\", \"result\": \"Không nghe máy\"}")
                .andExpect(status().isCreated());

        getJson(managerToken, "/api/v1/recipient-confirmations?residentId=" + resident)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactAttempts.length()").value(1))
                .andExpect(jsonPath("$[0].contactAttempts[0].familyMemberId").value(representative))
                .andExpect(jsonPath("$[0].contactAttempts[0].channel").value("PHONE_CALL"))
                .andExpect(jsonPath("$[0].contactAttempts[0].result").value("Không nghe máy"));

        String detail = postJson(
                        managerToken,
                        "/api/v1/recipient-confirmations/" + id + "/approve-on-behalf",
                        "{\"reason\": \"Không liên hệ được\", \"basis\": \"REPRESENTATIVE_UNREACHABLE\"}")
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(detail).contains("Được duyệt thay từ").doesNotContain("FR-").doesNotContain("CFG-");

        String noRepresentative = postJson(
                        managerToken,
                        "/api/v1/recipient-confirmations/" + id + "/approve-on-behalf",
                        "{\"reason\": \"Không có người đại diện\", \"basis\": \"NO_REPRESENTATIVE\"}")
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(noRepresentative).contains("đang có người đại diện").doesNotContain("FR-");
    }
}
