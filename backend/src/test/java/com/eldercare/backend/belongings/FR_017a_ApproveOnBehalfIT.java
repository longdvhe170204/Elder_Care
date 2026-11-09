package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationTimers;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.util.List;
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

    private String contact(long member, String channel, String result) {
        return "{\"familyMemberId\": " + member + ", \"channel\": \"" + channel + "\", \"at\": \""
                + time.now().minus(Duration.ofMinutes(5)) + "\", \"result\": \"" + result + "\"}";
    }

    private void twoContacts(long confirmation, long member) throws Exception {
        String path = "/api/v1/recipient-confirmations/" + confirmation + "/contact-evidence";
        postJson(tokenA, path, contact(member, "PHONE_CALL", "không nghe máy")).andExpect(status().isCreated());
        postJson(tokenA, path, contact(member, "TEXT_MESSAGE", "chưa trả lời")).andExpect(status().isCreated());
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

    @Test
    void FR_017_representativeReadsConfirmationBeforeDeciding_othersForbidden() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long id = confirmation(resident, "Trần Thị N", "999", phone);
        String path = "/api/v1/family-portal/recipient-confirmations/" + id;

        getJson(portalSession(representative), path)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
                .andExpect(jsonPath("$.recipientName").value("Trần Thị N"))
                .andExpect(jsonPath("$.recipientIdDocument").value("999"))
                .andExpect(jsonPath("$.belongingIds[0]").value(phone));
        getJson(portalSession(TestUsers.nextId()), path).andExpect(status().isForbidden());
        getJson(tokenA, path).andExpect(status().isForbidden());
    }

    /** Thông báo của đúng yêu cầu này (schema test giữ yêu cầu của test khác nên việc quét toàn bảng cũng báo chúng). */
    private List<com.eldercare.backend.common.notification.NotificationRequest> noticesOf(long id, String eventType) {
        return notices().stream()
                .filter(n -> n.eventType().equals(eventType))
                .filter(n -> n.sourceRecord().id().equals(String.valueOf(id)))
                .toList();
    }

    private String officeBearer() {
        return tokenB;
    }
}
