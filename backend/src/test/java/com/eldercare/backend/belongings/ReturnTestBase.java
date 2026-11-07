package com.eldercare.backend.belongings;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền US3 của 013 (trả đồ, xác nhận người nhận khác, duyệt thay): người thân giả (012), phân công giả để Điều dưỡng ở
 * trong phạm vi người cao tuổi, đồng hồ giả lập.
 */
@Import(ReturnTestBase.Assignments.class)
abstract class ReturnTestBase extends HandoverTestBase {

    @TestConfiguration(proxyBeanMethods = false)
    static class Assignments {
        @Bean
        @Primary
        FakeAssignments belongingsFakeAssignments() {
            return new FakeAssignments();
        }
    }

    @Autowired
    FakeAssignments assignments;

    @Autowired
    TestClockControl clock;

    TestUsers.TestUser managerUser;
    TestUsers.TestUser nurseUser;
    String managerToken;
    String nurseToken;

    @BeforeEach
    void setUpReturn() {
        managerUser = users.staff(Role.NURSING_HOME_MANAGER);
        nurseUser = users.staff(Role.NURSING_STAFF);
        managerToken = TestUsers.bearer(users.session(managerUser.accountId()));
        nurseToken = TestUsers.bearer(users.session(nurseUser.accountId()));
    }

    @AfterEach
    void resetReturn() {
        assignments.unassign(nurseUser.staffId());
        clock.reset();
    }

    private final java.util.Map<Long, String> portalSessions = new java.util.HashMap<>();

    /** Phiên cổng của người thân; một tài khoản mỗi người thân trong một test. */
    String portalSession(long familyMemberId) {
        return portalSessions.computeIfAbsent(familyMemberId, this::familySession);
    }

    /** Điều dưỡng được phân công trực tiếp cho người cao tuổi (phạm vi "được phân công"). */
    void assignNurse(long residentId) {
        assignments.assign(
                nurseUser.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
    }

    /** Người đại diện có quyền đón, giấy tờ đã đăng ký. */
    long representative(long residentId, String documentNumber) {
        long id = TestUsers.nextId();
        family.representative(id, residentId, FamilyRight.PICKUP_ALLOWED).idDocument(id, "CCCD", documentNumber);
        return id;
    }

    ResultActions giveBack(String token, String body) throws Exception {
        return postJson(token, "/api/v1/belongings/returns", body);
    }

    String returnItem(long belongingId, boolean withPhoto) {
        return "{\"belongingId\": " + belongingId + ", \"version\": " + version(belongingId)
                + ", \"condition\": {\"levelId\": " + levelId("SIGNS_OF_USE") + ", \"description\": \"Nguyên vẹn\"}"
                + (withPhoto ? ", \"photoFileIds\": [" + photo() + "]" : "") + "}";
    }

    String familyRecipient(long familyMemberId, String name, String documentType, String documentNumber) {
        return "{\"type\": \"FAMILY_MEMBER\", \"familyMemberId\": " + familyMemberId + ", \"fullName\": \"" + name
                + "\", \"presentedIdDocument\": {\"type\": \"" + documentType + "\", \"number\": \"" + documentNumber
                + "\"}}";
    }

    String otherRecipient(String name, String documentNumber) {
        return "{\"type\": \"OTHER_PERSON\", \"fullName\": \"" + name
                + "\", \"presentedIdDocument\": {\"type\": \"CCCD\", \"number\": \"" + documentNumber + "\"}}";
    }

    String returnBody(List<String> items, String recipient, String extra) {
        return "{\"items\": [" + String.join(", ", items) + "], \"recipient\": " + recipient + ", \"signatureFileId\": "
                + photo() + extra + "}";
    }

    /** Lập yêu cầu xác nhận người nhận khác bằng lệnh thật của Hành chính A; trả mã. */
    long confirmation(long resident, String name, String documentNumber, long... belongingIds) throws Exception {
        String ids = java.util.Arrays.stream(belongingIds)
                .mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(", "));
        return id(postJson(
                        tokenA,
                        "/api/v1/recipient-confirmations",
                        "{\"residentId\": " + resident + ", \"recipientName\": \"" + name
                                + "\", \"recipientIdDocument\": \"" + documentNumber + "\", \"belongingIds\": [" + ids
                                + "]}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    String confirmationStatus(long confirmationId) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, confirmationId);
    }

    int blockedCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM belonging_handover WHERE resident_id = ? AND kind = 'RETURN_BLOCKED'",
                Integer.class,
                residentId);
    }

    String lastBlockReason(long residentId) {
        return jdbc.queryForObject(
                "SELECT block_reason FROM belonging_handover WHERE resident_id = ? AND kind = 'RETURN_BLOCKED'"
                        + " ORDER BY id DESC LIMIT 1",
                String.class,
                residentId);
    }
}
