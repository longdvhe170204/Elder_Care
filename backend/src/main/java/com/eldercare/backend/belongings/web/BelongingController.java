package com.eldercare.backend.belongings.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingHandoverService;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingQueries;
import com.eldercare.backend.belongings.internal.belonging.service.IntakeService;
import com.eldercare.backend.belongings.internal.incident.service.BelongingIncidentLinks;
import com.eldercare.backend.belongings.web.request.BelongingIntakeRequest;
import com.eldercare.backend.belongings.web.request.HandoverRequest;
import com.eldercare.backend.belongings.web.response.BelongingResponse;
import com.eldercare.backend.belongings.web.response.HandoverResponse;
import com.eldercare.backend.common.error.BusinessException;
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

    BelongingController(
            IntakeService intake,
            BelongingHandoverService handovers,
            BelongingIncidentLinks incidentLinks,
            BelongingQueries queries) {
        this.intake = intake;
        this.handovers = handovers;
        this.incidentLinks = incidentLinks;
        this.queries = queries;
    }

    @GetMapping("/residents/{residentId}/belongings")
    List<BelongingResponse> ofResident(
            @PathVariable long residentId, @RequestParam(name = "status", required = false) BelongingStatus status) {
        return queries.ofResident(new Target(ENTITY, null, residentId, null), status).stream()
                .map(BelongingResponse::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/belongings")
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
        return BelongingResponse.Detail.of(queries.detail(id));
    }

    @PostMapping("/belongings/handovers")
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
    @PostMapping("/belonging-handovers/{id}/link-incident")
    void linkIncident(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody LinkIncidentRequest body) {
        if (body == null || body.incidentId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Thiếu mã sự cố");
        }
        incidentLinks.link(new Target(ENTITY, null, incidentLinks.residentOf(id), null), id, body.incidentId());
    }

    record LinkIncidentRequest(Long incidentId) {}

    @GetMapping("/staff/{staffId}/held-belongings")
    List<BelongingResponse> heldBelongings(@PathVariable long staffId) {
        return queries.heldValuableBy(staffId).stream()
                .map(BelongingResponse::of)
                .toList();
    }
}
