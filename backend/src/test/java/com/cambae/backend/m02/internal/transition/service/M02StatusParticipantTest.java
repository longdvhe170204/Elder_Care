package com.cambae.backend.m02.internal.transition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.dto.StatusCommand;
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
    private final M02StatusParticipant participant = new M02StatusParticipant(null, null, null, null, null, null);

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
    void FR_048_grantTemporaryAbsence_rejectsMissingOrWrongFields() {
        String ok = "\"expectedReturnAt\":\"2030-03-06T01:00:00Z\",\"pickedUpById\":11,\"pickupRecordId\":12,"
                + "\"handoverStaffId\":13";
        assertInvalid(StatusCommand.GRANT_TEMPORARY_ABSENCE, "{" + ok + "}", "absenceType");
        assertInvalid(
                StatusCommand.GRANT_TEMPORARY_ABSENCE, "{\"absenceType\":\"HOSPITAL\"," + ok + "}", "absenceType");
        assertInvalid(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                "{\"absenceType\":\"OTHER\",\"pickedUpById\":11,\"pickupRecordId\":12,\"handoverStaffId\":13}",
                "expectedReturnAt");
        assertInvalid(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                "{\"absenceType\":\"OTHER\",\"expectedReturnAt\":\"khong-phai-gio\",\"pickedUpById\":11,"
                        + "\"pickupRecordId\":12,\"handoverStaffId\":13}",
                "expectedReturnAt");
        assertInvalid(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                "{\"absenceType\":\"OTHER\",\"expectedReturnAt\":\"2030-03-06T01:00:00Z\",\"pickupRecordId\":12,"
                        + "\"handoverStaffId\":13}",
                "pickedUpById");
        assertInvalid(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                "{\"absenceType\":\"OTHER\"," + ok + ",\"takesMedication\":\"co\"}",
                "takesMedication");
        assertInvalid(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                "{\"absenceType\":\"OTHER\"," + ok + ",\"standardReason\":\"LY_DO_LA\"}",
                "standardReason");
    }

    @Test
    void FR_048_transferOut_needsUnitAndHandoverStaff_returnDateOptional() {
        assertThatCode(() -> participant.validateDetails(
                        StatusCommand.TRANSFER_OUT, json("{\"transferUnit\":\"Bệnh viện A\",\"handoverStaffId\":5}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> participant.validateDetails(
                        StatusCommand.TRANSFER_OUT,
                        json("{\"transferUnit\":\"Bệnh viện A\",\"handoverStaffId\":5,"
                                + "\"expectedReturnAt\":\"2030-03-09T00:00:00Z\"}")))
                .doesNotThrowAnyException();
        assertInvalid(StatusCommand.TRANSFER_OUT, "{\"handoverStaffId\":5}", "transferUnit");
        assertInvalid(StatusCommand.TRANSFER_OUT, "{\"transferUnit\":\"  \",\"handoverStaffId\":5}", "transferUnit");
        assertInvalid(StatusCommand.TRANSFER_OUT, "{\"transferUnit\":\"Bệnh viện A\"}", "handoverStaffId");
        assertInvalid(
                StatusCommand.TRANSFER_OUT,
                "{\"transferUnit\":\"Bệnh viện A\",\"handoverStaffId\":5,\"expectedReturnAt\":42}",
                "expectedReturnAt");
    }

    @Test
    void FR_049_recordReturn_returnedAtIsOptionalButMustBeAnInstant() {
        assertThatCode(() -> participant.validateDetails(StatusCommand.RECORD_RETURN, json("{}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> participant.validateDetails(
                        StatusCommand.RECORD_RETURN, json("{\"returnedAt\":\"2030-03-06T09:30:00Z\"}")))
                .doesNotThrowAnyException();
        assertInvalid(StatusCommand.RECORD_RETURN, "{\"returnedAt\":\"hom qua\"}", "returnedAt");
    }

    @Test
    void FR_062_endResidency_needsEndingAndEffectiveTime() {
        assertThatCode(() -> participant.validateDetails(
                        StatusCommand.END_RESIDENCY,
                        json("{\"residencyEndingId\":7,\"effectiveAt\":\"2030-04-01T00:00:00Z\"}")))
                .doesNotThrowAnyException();
        assertInvalid(StatusCommand.END_RESIDENCY, "{\"effectiveAt\":\"2030-04-01T00:00:00Z\"}", "residencyEndingId");
        assertInvalid(StatusCommand.END_RESIDENCY, "{\"residencyEndingId\":7}", "effectiveAt");
    }

    @Test
    void FR_069_recordDeath_needsTimePlaceDiscovererConfirmer_causeAndEvidenceOptional() {
        String required =
                "\"deathAt\":\"2030-03-04T22:00:00Z\",\"place\":\"Phòng 201\",\"discoveredBy\":\"Điều dưỡng A\","
                        + "\"confirmedById\":9";
        assertThatCode(() -> participant.validateDetails(StatusCommand.RECORD_DEATH, json("{" + required + "}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> participant.validateDetails(
                        StatusCommand.RECORD_DEATH,
                        json("{" + required + ",\"cause\":\"Suy tim\",\"evidenceFileId\":31}")))
                .doesNotThrowAnyException();
        assertInvalid(
                StatusCommand.RECORD_DEATH, "{\"place\":\"P\",\"discoveredBy\":\"A\",\"confirmedById\":9}", "deathAt");
        assertInvalid(
                StatusCommand.RECORD_DEATH,
                "{\"deathAt\":\"2030-03-04T22:00:00Z\",\"discoveredBy\":\"A\",\"confirmedById\":9}",
                "place");
        assertInvalid(
                StatusCommand.RECORD_DEATH,
                "{\"deathAt\":\"2030-03-04T22:00:00Z\",\"place\":\"P\",\"confirmedById\":9}",
                "discoveredBy");
        assertInvalid(
                StatusCommand.RECORD_DEATH,
                "{\"deathAt\":\"2030-03-04T22:00:00Z\",\"place\":\"P\",\"discoveredBy\":\"A\"}",
                "confirmedById");
        assertInvalid(StatusCommand.RECORD_DEATH, "{" + required + ",\"evidenceFileId\":\"x\"}", "evidenceFileId");
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

    @Test
    void completeAdmission_hasNoDetailsToValidate() {
        assertThatCode(() -> participant.validateDetails(StatusCommand.COMPLETE_ADMISSION, json("{}")))
                .doesNotThrowAnyException();
    }
}
