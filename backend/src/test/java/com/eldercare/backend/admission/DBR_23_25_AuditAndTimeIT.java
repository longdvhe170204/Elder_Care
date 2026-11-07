package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T074 (data-model mục 15): DBR-23: mỗi bước thay đổi dữ liệu nhóm 2, 3 của M02 có dòng nhật ký, bản đính chính trỏ
 * đúng một bản ghi gốc. DBR-25: ngày của lượt vắng theo Asia/Ho_Chi_Minh, không theo UTC; ngoài ra {@code TimeArchTest}
 * cấm đọc đồng hồ hệ thống. M02 không có bản ghi ngoại tuyến nên phần thời điểm thiết bị của DBR-25 không áp dụng.
 */
@AutoConfigureMockMvc
@WithMockUser
class DBR_23_25_AuditAndTimeIT extends AbsenceTestBase {

    @Autowired
    ResidencyDayApi days;

    private int m02AuditRows(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE resident_id = ? AND entity_type LIKE 'M02.%'",
                Integer.class, residentId);
    }

    @Test
    void DBR_23_contractAndAbsenceSteps_eachWriteAnAuditRow() throws Exception {
        long residentId = residentWithRepresentative();
        int start = m02AuditRows(residentId);

        long contractId = createDraft(residentId);
        int afterDraft = m02AuditRows(residentId);
        assertThat(afterDraft).as("lập hợp đồng Nháp").isGreaterThan(start);

        send(contractId);
        int afterSend = m02AuditRows(residentId);
        assertThat(afterSend).as("gửi ký").isGreaterThan(afterDraft);

        recordSignedRaw(contractId, signedBody(residentId, today())).andExpect(status().isOk());
        int afterSign = m02AuditRows(residentId);
        assertThat(afterSign).as("ghi nhận đã ký").isGreaterThan(afterSend);

        setStatus(residentId, "IN_RESIDENCE");
        clockAt(d0, 10, 0);
        grant(residentId, "HOME_VISIT", at(d0.plusDays(3), 10, 0));
        int afterGrant = m02AuditRows(residentId);
        assertThat(afterGrant).as("mở lượt vắng").isGreaterThan(afterSign);

        clockAt(d0.plusDays(1), 10, 0);
        recordReturn(residentId, null);
        assertThat(m02AuditRows(residentId)).as("đóng lượt vắng").isGreaterThan(afterGrant);
    }
}
