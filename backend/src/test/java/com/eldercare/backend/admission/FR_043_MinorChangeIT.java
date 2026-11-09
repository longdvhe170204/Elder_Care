package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.events.RequestSubmitted;
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

    private java.util.List<Long> approversOf(long requestId) {
        return events.stream(RequestSubmitted.class)
                .filter(e -> e.request().id() == requestId)
                .findFirst()
                .map(RequestSubmitted::approvers)
                .orElseThrow();
    }
}
