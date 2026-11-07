package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 019 T018 lượt 2 (US4 kịch bản 6; FR-013, Q-314; module-interfaces mục 3): Phụ trách bộ phận lập yêu cầu thanh lý kèm lý
 * do, trạng thái chưa đổi; Quản lý viện duyệt qua yêu cầu phê duyệt chung của 000 (không tự duyệt); tài sản Đã thanh lý,
 * không đổi trạng thái được nữa.
 */
class FR_013_AssetDisposalIT extends AssetTestBase {

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.administration.approval.api.ApprovalRequestApi approvals;

    @org.springframework.beans.factory.annotation.Autowired
    java.util.List<com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler> handlers;

    long disposalRequest(long assetId) {
        return jdbc.queryForObject("SELECT disposal_request_id FROM asset WHERE id=?", Long.class, assetId);
    }

    void approve(long requestId) throws Exception {
        String etag = mvc.perform(get("/api/v1/approval-requests/" + requestId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .header("If-Match", etag)
                        .header("Idempotency-Key", "ar-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void US4_6_disposalGoesThroughTheSharedApprovalRequestAndIsFinal() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "request-disposal", "{}").andExpect(status().isUnprocessableEntity());
        as(careLead);
        command(id, "request-disposal", "{\"reason\":\"Hỏng không sửa được\"}").andExpect(status().isForbidden());
        as(medicalLead);
        command(id, "request-disposal", "{\"reason\":\"Hỏng không sửa được\"}").andExpect(status().isOk());
        assertThat(stateOf(id)).as("chưa đổi trạng thái khi mới lập").isEqualTo("READY");
        long request = disposalRequest(id);
        assertThat(jdbc.queryForMap("SELECT type, status FROM approval_request WHERE id=?", request))
                .containsEntry("type", "M03.ASSET_DISPOSAL")
                .containsEntry("status", "PENDING_APPROVAL");
        command(id, "request-disposal", "{\"reason\":\"Lần nữa\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));

        as(manager);
        approve(request);
        // Bước áp dụng của 000 chạy ở transaction riêng (ApplyTransaction, REQUIRES_NEW), không thấy dữ liệu chưa
        // commit
        // của test; gọi handler của 019 như bước áp dụng gọi, trong transaction của test.
        var view = approvals.find(request).orElseThrow();
        assertThat(view.status()).isEqualTo("APPROVED");
        handlers.stream()
                .filter(h -> h.typeCode().equals("M03.ASSET_DISPOSAL"))
                .findFirst()
                .orElseThrow()
                .apply(view, view.actualEffectiveDate());
        assertThat(jdbc.queryForMap("SELECT status, disposed_at IS NOT NULL AS disposed FROM asset WHERE id=?", id))
                .containsEntry("status", "DISPOSED")
                .containsEntry("disposed", 1L);
        as(medicalLead);
        command(id, "report-damage", "{\"description\":\"x\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        command(id, "request-disposal", "{\"reason\":\"x\"}").andExpect(status().isConflict());
    }
}
