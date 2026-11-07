package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.incident.api.events.WishReconfirmationCompleted;
import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * BR-M05-16, FR-050c, FR-050g (US11 #4, #6): kết quả xác nhận lại là bản ghi nhóm 3 gắn cảnh báo; "nguyện vọng mới"
 * ghi phiên bản qua {@code EndOfLifeWishApi.recordVersion} cùng lệnh (001 từ chối thì không lưu gì); kết quả đầu tiên
 * dừng nhắc gọi; không liên lạc được thì làm theo nguyện vọng Hiệu lực, chưa có thì Bác sĩ quyết định kèm lý do.
 */
@RecordApplicationEvents
class BR_M05_16_WishReconfirmationIT extends CriticalCareTestBase {
    private static final String NEW_WISH = """
            ,"newWish":{"choice":"TRANSFER_TO_HOSPITAL","firstContact":"Trần Thị Đại Diện","respondentType":"PATIENT_REPRESENTATIVE",
            "respondentFamilyMemberId":%d,"relationship":"Con gái","assessingDoctorId":%d,
            "selfDeterminationAssessment":{"canSelfDetermine":false,"supportMethod":"Giải thích bằng lời","reviewDate":"2027-01-01"},
            "signerType":"PATIENT_REPRESENTATIVE","representativeSelection":"CHOSEN_BY_FAMILY","signerFamilyMemberId":%d,"signedAt":"%s","signedCopyFileId":%d}}""";

    @Autowired
    ApplicationEvents events;

    @Test
    void FR_050c_keptResultIsRecordedAndStopsCallsOnce() throws Exception {
        long alertId = flag(doctor).alertId();

        post(nurse, RECONFIRM, alertId, reconfirmation("KEPT")).andExpect(status().isCreated());
        post(doctor, RECONFIRM, alertId, reconfirmation("UNREACHABLE")).andExpect(status().isCreated());

        assertThat(count("M05.WISH_RECONFIRMATION", alertId)).isEqualTo(2);
        var row = jdbc.queryForMap(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, '$.channel')) AS channel,"
                        + " JSON_EXTRACT(content, '$.contactedFamilyMemberId') AS contacted, actor_id"
                        + " FROM record_event WHERE kind = 'M05.WISH_RECONFIRMATION' AND entity_id = ? ORDER BY id"
                        + " LIMIT 1",
                alertId);
        assertThat(row.get("channel")).isEqualTo("PHONE_CALL");
        assertThat(row.get("contacted").toString()).isEqualTo(Long.toString(representativeId));
        assertThat(row.get("actor_id")).isEqualTo(nurse.accountId());
        assertThat(events.stream(WishReconfirmationCompleted.class))
                .singleElement()
                .satisfies(event -> assertThat(event.alertId()).isEqualTo(alertId));
        verify(wishes, never()).recordVersion(any(), any());
    }

    @Test
    void BR_M05_16_changedResultRecordsNewWishVersionInSameCommand() throws Exception {
        long alertId = flag(doctor).alertId();
        String body = changed();

        post(nurse, RECONFIRM, alertId, reconfirmation("CHANGED")).andExpect(status().isUnprocessableContent());
        when(wishes.recordVersion(any(), eq(WishRecordSource.CRITICAL_RECONFIRMATION)))
                .thenReturn(777L);

        post(nurse, RECONFIRM, alertId, body).andExpect(status().isCreated());

        var command = ArgumentCaptor.forClass(RecordEndOfLifeWish.class);
        verify(wishes).recordVersion(command.capture(), eq(WishRecordSource.CRITICAL_RECONFIRMATION));
        assertThat(command.getValue().residentId()).isEqualTo(resident);
        assertThat(command.getValue().choice()).isEqualTo(WishChoice.TRANSFER_TO_HOSPITAL);
        assertThat(command.getValue().signerType()).isEqualTo(WishSignerType.PATIENT_REPRESENTATIVE);
        assertThat(command.getValue().assessment().supportMethod()).isEqualTo("Giải thích bằng lời");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(content, '$.newWishVersionId') FROM record_event"
                                + " WHERE kind = 'M05.WISH_RECONFIRMATION' AND entity_id = ?",
                        String.class,
                        alertId))
                .isEqualTo("777");
    }

    @Test
    void BR_M05_16_wishRejectedBy001_keepsNothing() throws Exception {
        long alertId = flag(doctor).alertId();
        when(wishes.recordVersion(any(), any()))
                .thenThrow(new BusinessException(
                        HttpStatus.UNPROCESSABLE_CONTENT, "SELF_DETERMINATION_ASSESSMENT_REQUIRED", "Thiếu nhận định"));

        post(nurse, RECONFIRM, alertId, changed())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("SELF_DETERMINATION_ASSESSMENT_REQUIRED"));

        assertThat(count("M05.WISH_RECONFIRMATION", alertId)).isZero();
        assertThat(events.stream(WishReconfirmationCompleted.class)).isEmpty();
    }

    @Test
    void BR_M05_16_unreachableWithoutWish_doctorDecidesWithReason() throws Exception {
        long alertId = flag(doctor).alertId();
        post(nurse, RECONFIRM, alertId, reconfirmation("UNREACHABLE")).andExpect(status().isCreated());

        post(doctor, CHOICE, alertId, "{\"choice\":\"STAY_PALLIATIVE\"}").andExpect(status().isUnprocessableContent());
        post(nurse, CHOICE, alertId, "{\"choice\":\"STAY_PALLIATIVE\",\"reason\":\"Theo chuyên môn\"}")
                .andExpect(status().isForbidden());
        post(doctor, CHOICE, alertId, "{\"choice\":\"STAY_PALLIATIVE\",\"reason\":\"Theo chuyên môn\"}")
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, '$.reason')) FROM record_event"
                                + " WHERE kind = 'M05.CHOICE_EXECUTION' AND entity_id = ?",
                        String.class,
                        alertId))
                .isEqualTo("Theo chuyên môn");
    }

    @Test
    void BR_M05_16_unreachableWithActiveWish_followsTheWish() throws Exception {
        activeWish(WishChoice.RETURN_HOME);
        long alertId = flag(doctor).alertId();
        post(nurse, RECONFIRM, alertId, reconfirmation("UNREACHABLE")).andExpect(status().isCreated());

        post(nurse, CHOICE, alertId, "{\"choice\":\"STAY_PALLIATIVE\"}").andExpect(status().isConflict());
        post(nurse, CHOICE, alertId, "{\"choice\":\"RETURN_HOME\"}").andExpect(status().isCreated());
    }

    private String changed() {
        String base = reconfirmation("CHANGED");
        return base.substring(0, base.length() - 1)
                + NEW_WISH.formatted(
                        representativeId,
                        doctor.accountId(),
                        representativeId,
                        time.now().minusSeconds(30),
                        TestUsers.nextId());
    }
}
