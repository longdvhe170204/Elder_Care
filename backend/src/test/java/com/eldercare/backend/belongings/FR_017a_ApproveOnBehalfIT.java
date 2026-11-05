package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationTimers;
import com.eldercare.backend.common.Role;
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
    void FR_017_onlyOfficeStaffCreates_andBelongingsMustBeReturnable() throws Exception {
        long resident = resident("IN_RESIDENCE");
        assignNurse(resident);
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        representative(resident, "012345");
        String body = "{\"residentId\": " + resident + ", \"recipientName\": \"Trần Thị N\","
                + " \"recipientIdDocument\": \"999\", \"belongingIds\": [" + phone + "]}";

        postJson(nurseToken, "/api/v1/recipient-confirmations", body).andExpect(status().isForbidden());
        postJson(managerToken, "/api/v1/recipient-confirmations", body).andExpect(status().isForbidden());

        long returned = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        giveBack(tokenA, returnBody(List.of(returnItem(returned, false)), "{\"type\": \"RESIDENT\"}", ""))
                .andExpect(status().isCreated());
        postJson(
                        tokenA,
                        "/api/v1/recipient-confirmations",
                        "{\"residentId\": " + resident + ", \"recipientName\": \"Trần Thị N\","
                                + " \"recipientIdDocument\": \"999\", \"belongingIds\": [" + returned + "]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE"));
    }

    @Test
    void FR_017_createWithSignedFormOfRepresentative_isEffectiveAtOnce() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long stranger = TestUsers.nextId();
        family.relate(stranger, resident);
        String path = "/api/v1/recipient-confirmations";
        String base = "{\"residentId\": " + resident + ", \"recipientName\": \"Trần Thị N\","
                + " \"recipientIdDocument\": \"999\", \"belongingIds\": [" + phone + "], \"signedForm\": ";

        postJson(tokenA, path, base + "{\"representativeId\": " + stranger + ", \"fileId\": " + photo() + "}}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GIVER_NOT_REPRESENTATIVE"));
        long id = id(postJson(
                        tokenA,
                        path,
                        base + "{\"representativeId\": " + representative + ", \"fileId\": " + photo() + "}}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(confirmationStatus(id)).isEqualTo("EFFECTIVE");
        getJson(tokenA, "/api/v1/recipient-confirmations?residentId=" + resident)
                .andExpect(jsonPath("$[0].method").value("SIGNED_FORM"))
                .andExpect(jsonPath("$[0].expiresAt").isNotEmpty());
    }

    @Test
    void Q17_onlyRepresentativeConfirms_throughThePortal_andCreatorIsNotified() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long stranger = TestUsers.nextId();
        family.relate(stranger, resident);
        long id = confirmation(resident, "Trần Thị N", "999", phone);

        postJson(portalSession(stranger), portal(id, "confirm"), "{}").andExpect(status().isForbidden());
        postJson(portalSession(representative), portal(id, "confirm"), "{}").andExpect(status().isOk());

        assertThat(confirmationStatus(id)).isEqualTo("EFFECTIVE");
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("RECIPIENT_CONFIRMATION_CONFIRMED");
            assertThat(n.level()).contains(NotificationLevel.LOW);
            assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.Staff(officeA.staffId(), "CREATOR"));
        });
        postJson(portalSession(representative), portal(id, "confirm"), "{}").andExpect(status().isConflict());
    }

    @Test
    void Q18_rejectedConfirmation_cannotBeApprovedOnBehalf() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long id = confirmation(resident, "Trần Thị N", "999", phone);

        postJson(portalSession(representative), portal(id, "reject"), "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        postJson(portalSession(representative), portal(id, "reject"), "{\"reason\": \"Không biết người này\"}")
                .andExpect(status().isOk());
        assertThat(confirmationStatus(id)).isEqualTo("REJECTED");

        postJson(
                        managerToken,
                        "/api/v1/recipient-confirmations/" + id + "/approve-on-behalf",
                        "{\"reason\": \"Cần giao gấp\", \"basis\": \"B_NO_REPRESENTATIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_REJECTED"));
        assertThat(confirmationStatus(id)).isEqualTo("REJECTED");
        assertThat(notices()).anySatisfy(n -> assertThat(n.eventType()).isEqualTo("RECIPIENT_CONFIRMATION_REJECTED"));
    }

    @Test
    void Q19_afterWaitingAndEnoughContactEvidence_managerIsToldAndApprovesOnBehalf() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long id = confirmation(resident, "Trần Thị N", "999", phone);
        String approve = "/api/v1/recipient-confirmations/" + id + "/approve-on-behalf";
        String body =
                "{\"reason\": \"Không liên hệ được người đại diện\", \"basis\": \"A_REPRESENTATIVE_UNREACHABLE\"}";

        postJson(managerToken, approve, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ON_BEHALF_APPROVAL_NOT_ELIGIBLE"));
        twoContacts(id, representative);
        postJson(managerToken, approve, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ON_BEHALF_APPROVAL_NOT_ELIGIBLE"));
        timers.notifyEligible();
        assertThat(noticesOf(id, "RECIPIENT_CONFIRMATION_ELIGIBLE")).isEmpty();

        clock.advanceBy(Duration.ofHours(4).plusMinutes(1));
        timers.notifyEligible();
        timers.notifyEligible();

        assertThat(noticesOf(id, "RECIPIENT_CONFIRMATION_ELIGIBLE"))
                .as("báo Quản lý viện một lần")
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.level()).contains(NotificationLevel.MEDIUM);
                    assertThat(n.recipientGroups())
                            .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
                });
        postJson(officeBearer(), approve, body).andExpect(status().isForbidden());
        postJson(managerToken, approve, body).andExpect(status().isOk());

        assertThat(confirmationStatus(id)).isEqualTo("EFFECTIVE");
        getJson(tokenA, "/api/v1/recipient-confirmations?residentId=" + resident)
                .andExpect(jsonPath("$[0].method").value("MANAGER_ON_BEHALF"));
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("RECIPIENT_CONFIRMATION_APPROVED_ON_BEHALF");
            assertThat(n.level()).contains(NotificationLevel.MEDIUM);
            assertThat(n.recipientGroups()).contains(new RecipientGroup.FamilyMember(representative, "REPRESENTATIVE"));
        });
    }

    @Test
    void Q19_contactEvidence_mustBeAboutAnEffectiveRepresentative_ofAPendingConfirmation() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long stranger = TestUsers.nextId();
        family.relate(stranger, resident);
        long id = confirmation(resident, "Trần Thị N", "999", phone);
        String path = "/api/v1/recipient-confirmations/" + id + "/contact-evidence";

        postJson(tokenA, path, contact(stranger, "PHONE_CALL", "không nghe máy"))
                .andExpect(status().isUnprocessableEntity());
        postJson(managerToken, path, contact(representative, "PHONE_CALL", "không nghe máy"))
                .andExpect(status().isForbidden());
        postJson(tokenA, path, contact(representative, "PHONE_CALL", "không nghe máy"))
                .andExpect(status().isCreated());
        postJson(portalSession(representative), portal(id, "confirm"), "{}").andExpect(status().isOk());
        postJson(tokenA, path, contact(representative, "TEXT_MESSAGE", "đã trả lời"))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_017a_ground_b_noEffectiveRepresentative_managerIsToldAtOnce_andApprovesImmediately() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long id = confirmation(resident, "Trần Thị N", "999", phone);
        String approve = "/api/v1/recipient-confirmations/" + id + "/approve-on-behalf";

        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.eventType()).isEqualTo("RECIPIENT_CONFIRMATION_ELIGIBLE");
            assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        });
        postJson(
                        managerToken,
                        approve,
                        "{\"reason\": \"Người đại diện duy nhất đã qua đời\", \"basis\": \"A_REPRESENTATIVE_UNREACHABLE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ON_BEHALF_APPROVAL_NOT_ELIGIBLE"));
        postJson(managerToken, approve, "{\"basis\": \"B_NO_REPRESENTATIVE\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        postJson(
                        managerToken,
                        approve,
                        "{\"reason\": \"Người đại diện duy nhất đã qua đời\", \"basis\": \"B_NO_REPRESENTATIVE\"}")
                .andExpect(status().isOk());

        assertThat(confirmationStatus(id)).isEqualTo("EFFECTIVE");
    }

    @Test
    void Q20_managerWhoCreatedTheRequest_cannotApproveItOnBehalf() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        var both = users.staff(Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER);
        String bothToken = TestUsers.bearer(users.session(both.accountId()));
        long id = id(postJson(
                        bothToken,
                        "/api/v1/recipient-confirmations",
                        "{\"residentId\": " + resident + ", \"recipientName\": \"Trần Thị N\","
                                + " \"recipientIdDocument\": \"999\", \"belongingIds\": [" + phone + "]}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        postJson(
                        bothToken,
                        "/api/v1/recipient-confirmations/" + id + "/approve-on-behalf",
                        "{\"reason\": \"Tự duyệt\", \"basis\": \"B_NO_REPRESENTATIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SELF_APPROVAL_NOT_ALLOWED"));
        assertThat(confirmationStatus(id)).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void Q22_effectiveConfirmationNotUsedWithinCfgM1008_expires_andCreatorIsNotified() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long representative = representative(resident, "012345");
        long id = confirmation(resident, "Trần Thị N", "999", phone);
        postJson(portalSession(representative), portal(id, "confirm"), "{}").andExpect(status().isOk());

        clock.advanceBy(Duration.ofHours(3));
        timers.expireDue();
        assertThat(confirmationStatus(id)).isEqualTo("EFFECTIVE");
        clock.advanceBy(Duration.ofHours(1).plusMinutes(1));
        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(phone, true)),
                                otherRecipient("Trần Thị N", "999"),
                                ", \"recipientConfirmationId\": " + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RECIPIENT_CONFIRMATION"));
        timers.expireDue();
        timers.expireDue();

        assertThat(confirmationStatus(id)).isEqualTo("EXPIRED");
        assertThat(noticesOf(id, "RECIPIENT_CONFIRMATION_EXPIRED"))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.level()).contains(NotificationLevel.LOW);
                    assertThat(n.recipientGroups())
                            .containsExactly(new RecipientGroup.Staff(officeA.staffId(), "CREATOR"));
                });
    }

    @Test
    void FR_017b_cancelNeedsReason_andOnlyOfficeStaff() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        representative(resident, "012345");
        long id = confirmation(resident, "Trần Thị N", "999", phone);
        String cancel = "/api/v1/recipient-confirmations/" + id + "/cancel";

        postJson(tokenA, cancel, "{}").andExpect(status().isUnprocessableEntity());
        postJson(managerToken, cancel, "{\"reason\": \"Hủy\"}").andExpect(status().isForbidden());
        postJson(tokenA, cancel, "{\"reason\": \"Người nhận không đến\"}").andExpect(status().isOk());

        assertThat(confirmationStatus(id)).isEqualTo("CANCELLED");
        postJson(tokenA, cancel, "{\"reason\": \"Lần hai\"}").andExpect(status().isConflict());
    }

    @Test
    void FR_017b_belongingLeavingTheList_dropsItFromTheRequest_andNoItemsLeftCancelsIt() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long glasses = intake(resident, "GLASSES", "\"quantity\": 1", location());
        long picker = representative(resident, "012345");
        long id = confirmation(resident, "Trần Thị N", "999", phone, glasses);

        giveBack(
                        tokenA,
                        returnBody(
                                List.of(returnItem(glasses, false)),
                                familyRecipient(picker, "Người thân " + picker, "CCCD", "012345"),
                                ""))
                .andExpect(status().isCreated());
        confirmations.dropBelonging(glasses);
        assertThat(confirmationStatus(id)).isEqualTo("PENDING_CONFIRMATION");
        getJson(tokenA, "/api/v1/recipient-confirmations?residentId=" + resident)
                .andExpect(jsonPath("$[0].belongingIds.length()").value(1))
                .andExpect(jsonPath("$[0].belongingIds[0]").value(phone));

        handover(tokenA, command("REPORT_LOST", item(phone, ""), ", \"reason\": \"Không thấy\""))
                .andExpect(status().isOk());
        confirmations.dropBelonging(phone);

        assertThat(confirmationStatus(id)).isEqualTo("CANCELLED");
        assertThat(notices())
                .anySatisfy(n -> assertThat(n.eventType()).isEqualTo("RECIPIENT_CONFIRMATION_CANCELLED_NO_ITEMS"));
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
