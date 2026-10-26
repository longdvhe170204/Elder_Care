package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m15.approval.api.RequestSubmitted;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T075 (US5 #6, #9; FR-043, Q-338; research T-06; 000 T105): Quản lý viện duyệt mọi yêu cầu thay đổi lưu trú; Hành chính
 * chỉ duyệt "thay đổi nhỏ" (chỉ thêm hoặc bớt dịch vụ thuộc gói, ảnh hưởng chi phí 0), tự tính lúc đọc và kiểm lại lúc duyệt
 * trên hợp đồng hiện hành; Hành chính không có trong danh sách người duyệt với yêu cầu khác.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043_MinorChangeIT extends ChangeTestBase {

    private String addInPackage(long serviceId) {
        return "{\"services\": [{\"serviceId\": " + serviceId + ", \"action\": \"ADD\", \"inPackage\": true}]}";
    }

    private long create(Setup s, String types, String newValue) throws Exception {
        long id = createChange(request(s.contractId(), types, newValue, today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        submit(id);
        return id;
    }

    @Test
    void US5_9_officeCreatesAndSelfApprovesAMinorChange() throws Exception {
        Setup s = activeSetup();
        long extra = service("OTHER", "Dịch vụ thuộc gói", null, 100_000);
        JsonNode created =
                createChange(request(s.contractId(), "[\"SERVICE\"]", addInPackage(extra), today().plusDays(3)));
        assertThat(created.get("minorChange").asBoolean()).isTrue();
        long id = created.get("approvalRequestId").asLong();
        submit(id);

        user.set(office.user());
        approveRaw(id).andExpect(status().isOk());

        assertThat(requestStatus(id)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT self_approved FROM approval_request WHERE id = ?", Boolean.class, id))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(details, '$.selfApproved') FROM audit_log WHERE entity_type ="
                                + " 'M15.APPROVAL_REQUEST' AND entity_id = ? AND action = 'REQUEST_APPROVED' ORDER BY id"
                                + " DESC LIMIT 1",
                        String.class,
                        String.valueOf(id)))
                .as("nhật ký ghi tự duyệt")
                .isEqualTo("true");
        assertThat(addenda(s.contractId())).hasSize(1);
    }

    @Test
    void US5_9_officeIsAmongTheApprovers_onlyOfAMinorChange() throws Exception {
        Setup s = activeSetup();
        long extra = service("OTHER", "Dịch vụ thuộc gói", null, 100_000);
        long minor = create(s, "[\"SERVICE\"]", addInPackage(extra));
        long other = create(s, "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}");

        assertThat(approversOf(minor)).contains(office.accountId(), manager.accountId());
        assertThat(approversOf(other)).contains(manager.accountId()).doesNotContain(office.accountId());
    }

    @Test
    void US5_6_officeCannotApproveARequestThatIsNotMinor() throws Exception {
        Setup s = activeSetup();
        long careLevel = create(s, "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}");
        long outsidePackage = create(
                s,
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + service("INJECTION", "Tiêm", null, 90_000)
                        + ", \"action\": \"ADD\"}]}");

        user.set(office.user());
        approveRaw(careLevel).andExpect(status().isForbidden());
        approveRaw(outsidePackage).andExpect(status().isForbidden());
        assertThat(requestStatus(careLevel)).isEqualTo("PENDING_APPROVAL");

        user.set(manager.user());
        approveRaw(careLevel).andExpect(status().isOk());
        approveRaw(outsidePackage).andExpect(status().isOk());
    }

    @Test
    void US5_6_aChangeOfResidenceType_andABedChangeAtTheSamePrice_areNotMinor() throws Exception {
        Setup s = activeSetup();
        JsonNode residenceType = createChange(request(
                s.contractId(),
                "[\"RESIDENCE_TYPE\"]",
                "{\"residenceType\": \"SHORT_TERM_RESIDENTIAL\", \"endDate\": \"" + today().plusDays(90) + "\"}",
                today().plusDays(3)));
        JsonNode bed = createChange(request(s.contractId(), "[\"BED\"]", "{\"newBedId\": 9}", today().plusDays(3)));

        assertThat(residenceType.get("minorChange").asBoolean()).isFalse();
        assertThat(bed.get("minorChange").asBoolean()).isFalse();
    }

    @Test
    void FR_043_theContractChangesBetweenSubmissionAndApproval_officeIsBlockedAgain() throws Exception {
        Setup s = activeSetup();
        // Gửi duyệt khi suất ăn còn thuộc gói: yêu cầu bớt suất ăn là thay đổi nhỏ
        long removeMeal =
                create(s, "[\"SERVICE\"]", "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"REMOVE\"}]}");
        assertThat(getChange(removeMeal).get("minorChange").asBoolean()).isTrue();
        // Rồi Quản lý viện bớt suất ăn và thêm lại ngoài gói: hợp đồng hiện hành đổi
        long dropMeal = createChange(request(
                        s.contractId(),
                        "[\"SERVICE\"]",
                        "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"REMOVE\"}]}",
                        today()))
                .get("approvalRequestId")
                .asLong();
        submit(dropMeal);
        managerApproves(dropMeal);
        long addBack = createChange(request(
                        s.contractId(),
                        "[\"SERVICE\"]",
                        "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"ADD\", \"inPackage\": false}]}",
                        today()))
                .get("approvalRequestId")
                .asLong();
        submit(addBack);
        managerApproves(addBack);

        assertThat(getChange(removeMeal).get("minorChange").asBoolean())
                .as("tính lại trên hợp đồng hiện hành")
                .isFalse();
        user.set(office.user());
        approveRaw(removeMeal).andExpect(status().isForbidden());
        user.set(manager.user());
        approveRaw(removeMeal).andExpect(status().isOk());
    }

    @Test
    void FR_043_managerApprovesEveryKind() throws Exception {
        Setup s = activeSetup();
        long careLevel = create(s, "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}");

        managerApproves(careLevel);

        assertThat(requestStatus(careLevel)).isEqualTo("APPROVED");
    }

    private java.util.List<Long> approversOf(long requestId) {
        return events.stream(RequestSubmitted.class)
                .filter(e -> e.request().id() == requestId)
                .findFirst()
                .map(RequestSubmitted::approvers)
                .orElseThrow();
    }
}
