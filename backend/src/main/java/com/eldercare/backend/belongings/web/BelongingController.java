package com.eldercare.backend.belongings.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingHandoverService;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingQueries;
import com.eldercare.backend.belongings.internal.belonging.service.IntakeService;
import com.eldercare.backend.belongings.internal.consent.service.SelfCustodyConsentService;
import com.eldercare.backend.belongings.internal.ending.service.BelongingEndingService;
import com.eldercare.backend.belongings.internal.incident.service.BelongingIncidentLinks;
import com.eldercare.backend.belongings.web.request.BelongingIntakeRequest;
import com.eldercare.backend.belongings.web.request.HandoverRequest;
import com.eldercare.backend.belongings.web.response.BelongingResponse;
import com.eldercare.backend.belongings.web.response.EndingConditionResponse;
import com.eldercare.backend.belongings.web.response.HandoverResponse;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Đồ gửi của người cao tuổi, Tiếp nhận, chi tiết, bàn giao (013 T011, T015; api-013 tag {@code belongings}). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "belongings")
class BelongingController {

    private static final String ENTITY = "M12.BELONGING";

    private final IntakeService intake;
    private final BelongingHandoverService handovers;
    private final BelongingIncidentLinks incidentLinks;
    private final BelongingQueries queries;
    private final BelongingEndingService endings;
    private final SelfCustodyConsentService consents;

    BelongingController(
            IntakeService intake,
            BelongingHandoverService handovers,
            BelongingIncidentLinks incidentLinks,
            BelongingQueries queries,
            BelongingEndingService endings,
            SelfCustodyConsentService consents) {
        this.intake = intake;
        this.handovers = handovers;
        this.incidentLinks = incidentLinks;
        this.queries = queries;
        this.endings = endings;
        this.consents = consents;
    }

    /** Điều kiện (b) kết thúc lưu trú và mục "xử lý đồ gửi" kèm căn cứ (FR-025, FR-026). */
    @GetMapping("/residents/{residentId}/belongings/ending-condition")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    EndingConditionResponse endingCondition(@PathVariable long residentId) {
        return EndingConditionResponse.of(endings.ending(new Target(ENTITY, null, residentId, null)));
    }

    @GetMapping("/residents/{residentId}/belongings")
    List<BelongingResponse> ofResident(
            @PathVariable long residentId, @RequestParam(name = "status", required = false) String status) {
        BelongingStatus wanted;
        try {
            wanted = status == null ? null : BelongingStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Trạng thái không hợp lệ");
        }
        return queries.ofResident(new Target(ENTITY, null, residentId, null), wanted).stream()
                .map(BelongingResponse::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/belongings")
    @ApiResponse(
            responseCode = "409",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ResponseStatus(HttpStatus.CREATED)
    BelongingResponse intake(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody BelongingIntakeRequest body) {
        var command = body.toCommand(residentId);
        if (command == null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "INVALID_VALUE",
                    "Thiếu loại đồ, mô tả, tình trạng, người giao hoặc thời điểm");
        }
        return BelongingResponse.of(intake.intake(new Target(ENTITY, null, residentId, null), command));
    }

    @GetMapping("/belongings/{id}")
    BelongingResponse.Detail detail(@PathVariable long id) {
        return BelongingResponse.Detail.of(
                queries.detail(id), consents.effectiveOf(id).orElse(null));
    }

    @PostMapping("/belongings/handovers")
    @ApiResponse(
            responseCode = "409",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    HandoverResponse handover(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody HandoverRequest body) {
        var command = body.toCommand();
        if (command == null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Thiếu loại bàn giao hoặc đồ, phiên bản");
        }
        long residentId = queries.residentOf(body.belongingIds());
        return HandoverResponse.of(handovers.handover(new Target(ENTITY, null, residentId, null), command));
    }

    /** Liên kết sự cố ghi sau với bản ghi Báo thất lạc, Ghi hư hỏng; một lần (FR-021, FR-024). */
    @PostMapping("/belongings/handovers/{id}/link-incident")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    void linkIncident(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody LinkIncidentRequest body) {
        if (body == null || body.incidentId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Thiếu mã sự cố");
        }
        incidentLinks.link(new Target(ENTITY, null, incidentLinks.residentOf(id), null), id, body.incidentId());
    }

    record LinkIncidentRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long incidentId) {}

    @GetMapping("/staff/{staffId}/held-belongings")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<BelongingResponse> heldBelongings(@PathVariable long staffId) {
        return queries.heldValuableBy(staffId).stream()
                .map(BelongingResponse::of)
                .toList();
    }
}
