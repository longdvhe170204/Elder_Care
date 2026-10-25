package com.cambae.backend.m01.web;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.dto.CareLevelAtDate;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.AssessmentView;
import com.cambae.backend.m01.internal.assessment.service.CareLevelQueryService;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.StatusFilter;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.web.request.CorrectionRequestBody;
import com.cambae.backend.m01.web.request.CreateAssessmentBody;
import com.cambae.backend.m01.web.request.ReasonBody;
import com.cambae.backend.m01.web.request.UpdateAssessmentBody;
import com.cambae.backend.m01.web.response.AssessmentBody;
import com.cambae.backend.m01.web.response.AssessmentDetailBody;
import com.cambae.backend.m01.web.response.AssessmentDetailBody.Reference;
import com.cambae.backend.m01.web.response.ConfirmAssessmentResultBody;
import com.cambae.backend.m01.web.response.CorrectionResultBody;
import com.cambae.backend.m01.web.response.HealthItemBody;
import com.cambae.backend.m01.web.response.InitialHealthRecordBody;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lần đánh giá và mức chăm sóc (001 T041; contracts/api-001.yaml, tag {@code assessments} và hai endpoint mức chăm sóc
 * của tag {@code status}). Không có endpoint sửa mức trực tiếp (FR-052); đính chính chuyển tới 000.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "assessments")
class AssessmentController {

    private static final String PROBLEM = "application/problem+json";

    private final AssessmentService assessments;
    private final CareLevelQueryService careLevels;
    private final InitialHealthRecordService records;
    private final HealthItemService items;
    private final CorrectionApi corrections;

    AssessmentController(
            AssessmentService assessments,
            CareLevelQueryService careLevels,
            InitialHealthRecordService records,
            HealthItemService items,
            CorrectionApi corrections) {
        this.assessments = assessments;
        this.careLevels = careLevels;
        this.records = records;
        this.items = items;
        this.corrections = corrections;
    }

    @GetMapping("/residents/{id}/assessments")
    List<AssessmentBody> list(@PathVariable long id) {
        return assessments.list(ResidentProfileService.target(id)).stream()
                .map(AssessmentBody::of)
                .toList();
    }

    @PostMapping("/residents/{id}/assessments")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo bản nháp"),
        @ApiResponse(
                responseCode = "409",
                description = "DRAFT_ALREADY_EXISTS, INITIAL_ASSESSMENT_EXISTS, STATUS_NOT_ASSESSABLE, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AssessmentDetailBody> create(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateAssessmentBody body) {
        Target target = ResidentProfileService.target(id);
        AssessmentView created = assessments.create(target, body.type(), body.basis());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(detail(target, created));
    }

    @GetMapping("/assessments/{id}")
    ResponseEntity<AssessmentDetailBody> get(@PathVariable long id) {
        Target target = assessments.targetOf(id);
        AssessmentView view = assessments.get(target, id);
        return ResponseEntity.ok().eTag(VersionEtag.of(view.version())).body(detail(target, view));
    }

    @PutMapping("/assessments/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "NOT_A_DRAFT, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AssessmentDetailBody> saveDraft(
            @PathVariable long id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateAssessmentBody body) {
        Target target = assessments.targetOf(id);
        AssessmentView saved = assessments.saveDraft(target, id, ifMatch, body.content());
        return ResponseEntity.ok().eTag(VersionEtag.of(saved.version())).body(detail(target, saved));
    }

    @PostMapping("/assessments/{id}/confirm")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CONDITION_NOT_MET (thiếu nhận xét hoặc mức), NOT_A_DRAFT, STATUS_NOT_ASSESSABLE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ConfirmAssessmentResultBody confirm(
            @PathVariable long id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        return ConfirmAssessmentResultBody.of(assessments.confirm(assessments.targetOf(id), id, ifMatch));
    }

    @PostMapping("/assessments/{id}/discard-draft")
    AssessmentBody discardDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        return AssessmentBody.of(assessments.discard(assessments.targetOf(id), id, body.reason()));
    }

    @PostMapping("/assessments/{id}/correct")
    ResponseEntity<CorrectionResultBody> correct(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CorrectionRequestBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CorrectionResultBody.of(corrections.create(
                        AssessmentService.RECORD_TYPE,
                        id,
                        body.type().correctionType(),
                        body.correctedContent(),
                        body.reason())));
    }

    @GetMapping("/residents/{id}/care-level")
    @Tag(name = "status")
    CareLevelAtDate careLevel(
            @PathVariable long id,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        return careLevels.at(ResidentProfileService.target(id), date).orElseThrow(NotFoundException::new);
    }

    @GetMapping("/residents/{id}/care-level-history")
    @Tag(name = "status")
    List<CareLevelAtDate> careLevelHistory(@PathVariable long id) {
        return careLevels.history(ResidentProfileService.target(id));
    }

    /** Dữ liệu tham khảo cho bác sĩ (FR-024): hồ sơ sức khỏe ban đầu, mục Hiệu lực; chỉ số gần nhất chờ 007. */
    private AssessmentDetailBody detail(Target target, AssessmentView view) {
        InitialHealthRecordBody initial =
                records.get(target).map(InitialHealthRecordBody::of).orElse(null);
        List<HealthItemBody> active = items.list(target, StatusFilter.ACTIVE).stream()
                .map(HealthItemBody::of)
                .toList();
        return AssessmentDetailBody.of(AssessmentBody.of(view), new Reference(initial, active, List.of()));
    }
}
