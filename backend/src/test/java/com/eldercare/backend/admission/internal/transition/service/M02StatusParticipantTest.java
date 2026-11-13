package com.eldercare.backend.admission.internal.transition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 004 T009 (contracts mục 1.1; FR-048, FR-069, FR-002): bean tham gia lệnh trạng thái của 001 chạy đầu tiên, quan tâm
 * bảy lệnh và kiểm tra nội dung {@code details} mà 004 sở hữu; lỗi là 422 {@code INVALID_VALUE}.
 */
class M02StatusParticipantTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    // Các phép kiểm tra nội dung details không dùng các bean tác động (lượt đăng ký, hồ sơ chờ, hợp đồng)
    private final M02StatusParticipant participant = new M02StatusParticipant(null, null, null, null, null, null, null);

    private static JsonNode json(String text) {
        return JSON.readTree(text);
    }

    private void assertInvalid(StatusCommand command, String details, String messagePart) {
        assertThatThrownBy(() -> participant.validateDetails(command, json(details)))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining(messagePart);
    }

    @Test
    void T_03_participantRunsFirst_andCoversSevenCommands() {
        assertThat(AnnotationUtils.findAnnotation(M02StatusParticipant.class, Order.class)
                        .value())
                .isEqualTo(1);
        assertThat(participant.interestedCommands())
                .containsExactlyInAnyOrder(
                        StatusCommand.COMPLETE_ADMISSION,
                        StatusCommand.CANCEL_ADMISSION,
                        StatusCommand.GRANT_TEMPORARY_ABSENCE,
                        StatusCommand.TRANSFER_OUT,
                        StatusCommand.RECORD_RETURN,
                        StatusCommand.END_RESIDENCY,
                        StatusCommand.RECORD_DEATH);
    }

    @Test
    void FR_048_grantTemporaryAbsence_acceptsCompleteDetails() {
        assertThatCode(() -> participant.validateDetails(StatusCommand.GRANT_TEMPORARY_ABSENCE, json("""
                                {"absenceType":"HOME_VISIT","expectedReturnAt":"2030-03-06T01:00:00Z",
                                 "pickedUpById":11,"pickupRecordId":12,"handoverStaffId":13,
                                 "takesMedication":true,"standardReason":"HOME_VISIT_END_OF_LIFE_WISH"}""")))
                .doesNotThrowAnyException();
    }

    @Test
    void FR_002_cancelAdmission_waitingListFieldsComeTogetherAndAreOptional() {
        assertThatCode(() -> participant.validateDetails(StatusCommand.CANCEL_ADMISSION, json("{}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> participant.validateDetails(
                        StatusCommand.CANCEL_ADMISSION,
                        json("{\"waitingListEntryId\":4,\"waitingListCommand\":\"DECLINE\"}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> participant.validateDetails(
                        StatusCommand.CANCEL_ADMISSION,
                        json("{\"waitingListEntryId\":4,\"waitingListCommand\":\"CANCEL_WAITING\"}")))
                .doesNotThrowAnyException();
        assertInvalid(StatusCommand.CANCEL_ADMISSION, "{\"waitingListEntryId\":4}", "waitingListCommand");
        assertInvalid(StatusCommand.CANCEL_ADMISSION, "{\"waitingListCommand\":\"DECLINE\"}", "waitingListEntryId");
        assertInvalid(
                StatusCommand.CANCEL_ADMISSION,
                "{\"waitingListEntryId\":4,\"waitingListCommand\":\"HUY\"}",
                "waitingListCommand");
    }
}
