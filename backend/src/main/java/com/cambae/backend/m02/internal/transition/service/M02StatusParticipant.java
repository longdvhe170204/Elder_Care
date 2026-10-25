package com.cambae.backend.m02.internal.transition.service;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.m02.internal.application.service.AdmissionContractConditions;
import com.cambae.backend.m02.internal.application.service.AdmissionTransition;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Bean của 004 tham gia lệnh trạng thái của 001 (contracts mục 1.1; research T-03, T-08, T-09, T-13), chạy đầu tiên
 * ({@code @Order(1)} sau M01). Khung: kiểm tra nội dung {@code details} mà 004 sở hữu. Điều kiện ({@code checkConditions},
 * {@code waiveConditions}), cảnh báo và tác động ({@code applyEffects}) được thêm ở user story của từng lệnh.
 *
 * <p>Trường bắt buộc theo spec: lượt vắng (FR-048: loại, dự kiến trở lại, người đón có bản ghi đón, người bàn giao),
 * chuyển viện (người hoặc đơn vị đưa đi, người bàn giao), kết thúc lưu trú (hồ sơ kết thúc, thời điểm hiệu lực), qua đời
 * (FR-069: thời điểm, địa điểm, người phát hiện, người xác nhận). Bằng chứng qua đời bắt buộc khi Hành chính thực hiện
 * nên kiểm ở điều kiện, không ở đây vì cần vai trò người thực hiện.
 */
@Component
@Order(1)
class M02StatusParticipant implements StatusTransitionParticipant {

    private static final Set<String> TEMPORARY_ABSENCE_TYPES =
            Set.of("HOME_VISIT", "FAMILY_OUTING", "SAME_DAY_MEDICAL_VISIT", "OTHER");
    private static final Set<String> STANDARD_REASONS = Set.of("HOME_VISIT_END_OF_LIFE_WISH");
    private static final Set<String> WAITING_LIST_COMMANDS = Set.of("DECLINE", "CANCEL_WAITING");

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(
                StatusCommand.COMPLETE_ADMISSION,
                StatusCommand.CANCEL_ADMISSION,
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                StatusCommand.TRANSFER_OUT,
                StatusCommand.RECORD_RETURN,
                StatusCommand.END_RESIDENCY,
                StatusCommand.RECORD_DEATH);
    }

    private final AdmissionContractConditions admissionConditions;
    private final AdmissionTransition admissionTransition;

    M02StatusParticipant(AdmissionContractConditions admissionConditions, AdmissionTransition admissionTransition) {
        this.admissionConditions = admissionConditions;
        this.admissionTransition = admissionTransition;
    }

    /** Hoàn tất tiếp nhận: điều kiện (b), (c), (e) của FR-004 (research T-09); lệnh khác bổ sung ở user story của chúng. */
    @Override
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() == StatusCommand.COMPLETE_ADMISSION) {
            return admissionConditions.unmet(context.residentId(), context.at());
        }
        return List.of();
    }

    @Override
    public void validateDetails(StatusCommand command, JsonNode details) {
        switch (command) {
            case GRANT_TEMPORARY_ABSENCE -> {
                requireOneOf(details, "absenceType", TEMPORARY_ABSENCE_TYPES);
                requireInstant(details, "expectedReturnAt");
                requireId(details, "pickedUpById");
                requireId(details, "pickupRecordId");
                requireId(details, "handoverStaffId");
                optionalBoolean(details, "takesMedication");
                optionalOneOf(details, "standardReason", STANDARD_REASONS);
            }
            case TRANSFER_OUT -> {
                requireText(details, "transferUnit");
                requireId(details, "handoverStaffId");
                optionalInstant(details, "expectedReturnAt");
            }
            case RECORD_RETURN -> optionalInstant(details, "returnedAt");
            case END_RESIDENCY -> {
                requireId(details, "residencyEndingId");
                requireInstant(details, "effectiveAt");
            }
            case RECORD_DEATH -> {
                requireInstant(details, "deathAt");
                requireText(details, "place");
                requireText(details, "discoveredBy");
                requireId(details, "confirmedById");
                optionalText(details, "cause");
                optionalId(details, "evidenceFileId");
            }
            case CANCEL_ADMISSION -> {
                optionalId(details, "waitingListEntryId");
                optionalOneOf(details, "waitingListCommand", WAITING_LIST_COMMANDS);
                if (has(details, "waitingListEntryId") && !has(details, "waitingListCommand")) {
                    throw invalid("waitingListCommand", "bắt buộc khi có waitingListEntryId");
                }
                if (has(details, "waitingListCommand") && !has(details, "waitingListEntryId")) {
                    throw invalid("waitingListEntryId", "bắt buộc khi có waitingListCommand");
                }
            }
            default -> {}
        }
    }

    /** Tác động của Hoàn tất tiếp nhận, Hủy tiếp nhận lên lượt đăng ký (FR-002); lệnh khác thêm ở user story của chúng. */
    @Override
    public void applyEffects(TransitionResult result) {
        switch (result.command()) {
            case COMPLETE_ADMISSION -> admissionTransition.completed(result);
            case CANCEL_ADMISSION -> admissionTransition.cancelled(result);
            default -> {}
        }
    }

    /** Hoàn tất tiếp nhận: cảnh báo ý kiến vào ở, không chặn lệnh (FR-004a). */
    @Override
    public List<Warning> warnings(CommandContext context) {
        if (context.command() == StatusCommand.COMPLETE_ADMISSION) {
            return admissionTransition.completionWarnings(context.residentId());
        }
        return List.of();
    }

    private static boolean has(JsonNode details, String field) {
        JsonNode node = details == null ? null : details.get(field);
        return node != null && !node.isNull();
    }

    private static InvalidValueException invalid(String field, String problem) {
        return new InvalidValueException("details." + field + " " + problem);
    }

    private static void requireId(JsonNode details, String field) {
        if (!has(details, field)) {
            throw invalid(field, "là bắt buộc");
        }
        optionalId(details, field);
    }

    private static void optionalId(JsonNode details, String field) {
        if (has(details, field)
                && !(details.get(field).isIntegralNumber() && details.get(field).asLong() > 0)) {
            throw invalid(field, "phải là mã số nguyên dương");
        }
    }

    private static void requireText(JsonNode details, String field) {
        if (!has(details, field)) {
            throw invalid(field, "là bắt buộc");
        }
        optionalText(details, field);
        if (details.get(field).asString().isBlank()) {
            throw invalid(field, "không được để trống");
        }
    }

    private static void optionalText(JsonNode details, String field) {
        if (has(details, field) && !details.get(field).isString()) {
            throw invalid(field, "phải là chuỗi");
        }
    }

    private static void optionalBoolean(JsonNode details, String field) {
        if (has(details, field) && !details.get(field).isBoolean()) {
            throw invalid(field, "phải là true hoặc false");
        }
    }

    private static void requireInstant(JsonNode details, String field) {
        if (!has(details, field)) {
            throw invalid(field, "là bắt buộc");
        }
        optionalInstant(details, field);
    }

    private static void optionalInstant(JsonNode details, String field) {
        if (!has(details, field)) {
            return;
        }
        JsonNode node = details.get(field);
        try {
            if (!node.isString()) {
                throw invalid(field, "phải là thời điểm ISO-8601");
            }
            Instant.parse(node.asString());
        } catch (DateTimeParseException e) {
            throw invalid(field, "phải là thời điểm ISO-8601");
        }
    }

    private static void requireOneOf(JsonNode details, String field, Set<String> allowed) {
        if (!has(details, field)) {
            throw invalid(field, "là bắt buộc");
        }
        optionalOneOf(details, field, allowed);
    }

    private static void optionalOneOf(JsonNode details, String field, Set<String> allowed) {
        if (has(details, field)
                && !(details.get(field).isString()
                        && allowed.contains(details.get(field).asString()))) {
            throw invalid(field, "phải là một trong " + allowed);
        }
    }
}
