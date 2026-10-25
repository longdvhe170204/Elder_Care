package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.ApplicationStatus;
import com.cambae.backend.m02.internal.application.service.AdmissionApplicationService;
import com.cambae.backend.m02.internal.application.service.AdmissionApplicationService.Detail;
import com.cambae.backend.m02.internal.application.service.AdmissionConditionService;
import com.cambae.backend.m02.internal.application.service.AdmissionOpinionService;
import com.cambae.backend.m02.internal.application.service.M02ActorNames;
import com.cambae.backend.m02.internal.waitinglist.service.WaitingListService;
import com.cambae.backend.m02.web.request.AdmissionApplicationInputBody;
import com.cambae.backend.m02.web.request.RecordAdmissionOpinionBody;
import com.cambae.backend.m02.web.request.UpdateAdmissionApplicationBody;
import com.cambae.backend.m02.web.response.AdmissionApplicationBody;
import com.cambae.backend.m02.web.response.AdmissionOpinionBody;
import com.cambae.backend.m02.web.response.ConditionListBody;
import com.cambae.backend.m02.web.response.ProgressBody;
import com.cambae.backend.m15.account.api.dto.Target;
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
 * Lượt đăng ký tiếp nhận (004 US1; contracts/api-004.yaml, tag {@code applications}). Mọi logic và kiểm quyền ở service.
 * Hoàn tất tiếp nhận, Hủy tiếp nhận là endpoint của api-001.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "applications")
class AdmissionApplicationController {

    private static final String PROBLEM = "application/problem+json";

    private final AdmissionApplicationService applications;
    private final AdmissionOpinionService opinions;
    private final AdmissionConditionService conditions;
    private final M02ActorNames names;
    private final WaitingListService waitingList;

    AdmissionApplicationController(
            AdmissionApplicationService applications,
            AdmissionOpinionService opinions,
            AdmissionConditionService conditions,
            M02ActorNames names,
            WaitingListService waitingList) {
        this.applications = applications;
        this.opinions = opinions;
        this.conditions = conditions;
        this.names = names;
        this.waitingList = waitingList;
    }

    @GetMapping("/admission-applications")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<AdmissionApplicationBody> list(
            @RequestParam(name = "status", required = false) Set<ApplicationStatus> status,
            @RequestParam(name = "cursor", required = false) String cursor) {
        // Hợp đồng trả mảng trơn: cursor là mã của dòng cuối trang trước, trang kế gồm các lượt có mã nhỏ hơn
        return applications.list(Target.UNSPECIFIED, status, cursor).stream()
                .map(AdmissionApplicationBody::of)
                .toList();
    }

    @PostMapping("/admission-applications")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "APPLICATION_ALREADY_EXISTS, RESIDENT_NOT_ADMISSION_IN_PROGRESS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AdmissionApplicationBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody AdmissionApplicationInputBody body) {
        AdmissionApplication created =
                applications.create(AdmissionApplicationService.targetForResident(body.residentId()), body.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(AdmissionApplicationBody.of(created, List.of(), names::of));
    }

    @GetMapping("/admission-applications/{id}")
    ResponseEntity<AdmissionApplicationBody> get(@PathVariable long id) {
        Target target = applications.targetOf(id);
        Detail detail = applications.get(target, id);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(detail.application().version()))
                .body(AdmissionApplicationBody.of(
                        detail.application(),
                        detail.opinions(),
                        names::of,
                        waitingList.entryIdOfApplication(id).orElse(null)));
    }

    @PostMapping("/admission-applications/{id}/update")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "APPLICATION_CLOSED, APPLICATION_DATE_LOCKED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "VERSION_MISMATCH",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> update(
            @PathVariable long id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody UpdateAdmissionApplicationBody body) {
        Target target = applications.targetOf(id);
        AdmissionApplication updated = applications.update(target, id, ifMatch, body.content(), body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @PostMapping("/admission-applications/{id}/admission-opinion")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi"),
        @ApiResponse(
                responseCode = "409",
                description = "APPLICATION_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AdmissionOpinionBody> recordOpinion(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordAdmissionOpinionBody body) {
        Target target = applications.targetOf(id);
        var saved = opinions.record(target, id, body.opinion(), body.source().toSource(), body.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdmissionOpinionBody.of(saved, names::of));
    }

    @GetMapping("/admission-applications/{id}/admission-conditions")
    ConditionListBody conditions(@PathVariable long id) {
        Target target = applications.targetOf(id);
        return ConditionListBody.of(conditions.conditions(target, id), names::of);
    }

    @GetMapping("/admission-applications/{id}/progress")
    ProgressBody progress(@PathVariable long id) {
        Target target = applications.targetOf(id);
        return ProgressBody.of(conditions.progressOf(target, id));
    }
}
