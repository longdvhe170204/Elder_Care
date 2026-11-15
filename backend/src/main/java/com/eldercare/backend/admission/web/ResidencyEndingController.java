package com.eldercare.backend.admission.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.admission.internal.absence.service.AbsenceNames;
import com.eldercare.backend.admission.internal.ending.domain.EndingStatus;
import com.eldercare.backend.admission.internal.ending.service.EndingConditionService;
import com.eldercare.backend.admission.internal.ending.service.ResidencyEndingService;
import com.eldercare.backend.admission.internal.ending.service.ResidencyEndingService.HandoverInput;
import com.eldercare.backend.admission.internal.ending.service.ResidencyEndingService.NewEndingInput;
import com.eldercare.backend.admission.web.request.ChangeExpectedEndDateBody;
import com.eldercare.backend.admission.web.request.ReasonBody;
import com.eldercare.backend.admission.web.request.RequestEndingExceptionBody;
import com.eldercare.backend.admission.web.request.ResidencyEndingInputBody;
import com.eldercare.backend.admission.web.request.ResidentHandoverBody;
import com.eldercare.backend.admission.web.response.ApprovalRequestRefBody;
import com.eldercare.backend.admission.web.response.ConditionListBody;
import com.eldercare.backend.admission.web.response.ResidencyEndingBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ kết thúc lưu trú (004 US7; contracts/api-004.yaml, tag {@code residency-ending}). Hoàn tất hồ sơ là lệnh Kết thúc lưu
 * trú của 001 ({@code POST /residents/{id}/end-residency}) nên không có endpoint hoàn tất ở đây. Mọi logic và kiểm quyền ở
 * service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "residency-ending")
class ResidencyEndingController {

    private static final String PROBLEM = "application/problem+json";

    private final ResidencyEndingService endings;
    private final EndingConditionService conditions;
    private final AbsenceNames names;

    ResidencyEndingController(ResidencyEndingService endings, EndingConditionService conditions, AbsenceNames names) {
        this.endings = endings;
        this.conditions = conditions;
        this.names = names;
    }

    @PostMapping("/residency-endings")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền lập loại hồ sơ này",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "RESIDENT_NOT_IN_RESIDENCE, RESIDENCY_ENDING_ALREADY_OPEN",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Chuyển cơ sở thiếu nơi chuyển đến",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ResidencyEndingBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ResidencyEndingInputBody body) {
        var ending = endings.create(
                ResidencyEndingService.targetForResident(body.residentId()),
                new NewEndingInput(
                        body.residentId(),
                        body.endCase().endCase(),
                        Boolean.TRUE.equals(body.medicalReason()),
                        body.expectedEndDate(),
                        body.destination(),
                        body.requestedBy()),
                body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResidencyEndingBody.of(ending, names));
    }

    @GetMapping("/residency-endings")
    List<ResidencyEndingBody> list(
            @RequestParam(name = "residentId", required = false) Long residentId,
            @RequestParam(name = "status", required = false) Set<EndingStatus> status) {
        return endings.list(Target.UNSPECIFIED, residentId, status).stream()
                .map(detail -> ResidencyEndingBody.of(detail, names))
                .toList();
    }

    @GetMapping("/residency-endings/{id}")
    ResidencyEndingBody get(@PathVariable long id) {
        return ResidencyEndingBody.of(endings.get(endings.targetOf(id), id), names);
    }

    @GetMapping("/residency-endings/{id}/conditions")
    ConditionListBody conditions(@PathVariable long id) {
        Target target = endings.targetOf(id);
        return ConditionListBody.ofEnding(conditions.conditions(target, id), conditions::approverOf);
    }

    @PostMapping("/residency-endings/{id}/change-date")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "Hồ sơ không còn Đang chuẩn bị",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> changeDate(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ChangeExpectedEndDateBody body) {
        endings.changeDate(endings.targetOf(id), id, body.expectedEndDate(), body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/residency-endings/{id}/cancel")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    ResponseEntity<Void> cancel(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        endings.cancel(endings.targetOf(id), id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/residency-endings/{id}/handover")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi"),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu người nhận hoặc nhân viên bàn giao",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> handover(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ResidentHandoverBody body) {
        endings.recordHandover(
                endings.targetOf(id),
                id,
                new HandoverInput(
                        body.recipientType(),
                        body.familyMemberId(),
                        body.facilityName(),
                        body.handoverAt(),
                        body.handoverStaffId(),
                        body.pickupRecordId()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/residency-endings/{id}/request-exception")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập yêu cầu"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền lập ngoại lệ cho điều kiện này",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ApprovalRequestRefBody> requestException(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RequestEndingExceptionBody body) {
        var request = endings.requestException(endings.targetOf(id), id, body.conditionCode(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApprovalRequestRefBody.of(request));
    }
}
