package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

    @Test
    void DBR_23_aCorrection_pointsToExactlyOneOriginalRecord() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 9, 0);
        long absenceId = transferOut(residentId);
        clockAt(d0.plusDays(29), 15, 0);
        recordReturn(residentId, null);
        user.set(office.user());
        int beforeCorrection = m02AuditRows(residentId);

        mvc.perform(post("/api/v1/absences/" + absenceId + "/correct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EDIT_CONTENT","correctedContent":{"returnedAt":"%s"},"reason":"Ghi nhầm ngày về"}""".formatted(at(d0.plusDays(31), 15, 0))))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M02.ABSENCE' AND original_record_id = ?",
                        Integer.class,
                        absenceId))
                .isEqualTo(1);
        assertThat(m02AuditRows(residentId)).as("đính chính có nhật ký").isGreaterThan(beforeCorrection);
    }

    @Test
    void DBR_25_absenceDay_followsVietnamTime_atHalfPastMidnight() throws Exception {
        long residentId = inResidence();
        LocalDate vietnamDay = d0.plusDays(2);
        // 00:30 giờ Việt Nam là 17:30 UTC của ngày hôm trước; ngày rời viện phải là ngày Việt Nam
        clockAt(vietnamDay, 0, 30);
        long absenceId = grant(residentId, "HOME_VISIT", at(vietnamDay.plusDays(3), 10, 0));

        ResidencyDayView before = days.daysBetween(residentId, vietnamDay.minusDays(1), vietnamDay.minusDays(1))
                .getFirst();
        ResidencyDayView leaving =
                days.daysBetween(residentId, vietnamDay, vietnamDay).getFirst();

        assertThat(before.absenceId())
                .as("ngày trước đó theo giờ Việt Nam không vắng")
                .isNull();
        assertThat(leaving.absenceId()).isEqualTo(absenceId);
    }
}
