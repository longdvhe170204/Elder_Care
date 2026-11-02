package com.eldercare.backend.admission.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.admission.internal.absence.service.AbsenceNames;
import com.eldercare.backend.admission.internal.absence.service.AbsenceService;
import com.eldercare.backend.admission.web.request.CorrectionRequestBody;
import com.eldercare.backend.admission.web.request.ExtendExpectedReturnBody;
import com.eldercare.backend.admission.web.request.ReasonBody;
import com.eldercare.backend.admission.web.response.AbsenceBody;
import com.eldercare.backend.admission.web.response.AbsenceDetailBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
 * Lượt vắng (004 US6; contracts/api-004.yaml, tag {@code absences}). Lượt vắng mở và đóng bằng lệnh Cho tạm vắng, Chuyển viện,
 * Ghi nhận trở về của 001 nên không có {@code POST /absences}. Mọi logic và kiểm quyền ở service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "absences")
class AbsenceController {

    private static final String PROBLEM = "application/problem+json";

    private final AbsenceService absences;
    private final AbsenceNames names;
    private final CorrectionApi corrections;

    AbsenceController(AbsenceService absences, AbsenceNames names, CorrectionApi corrections) {
        this.absences = absences;
        this.names = names;
        this.corrections = corrections;
    }

    @GetMapping("/absences")
    List<AbsenceBody> list(@RequestParam long residentId) {
        return absences.ofResident(AbsenceService.targetForResident(residentId)).stream()
                .map(absence -> AbsenceBody.of(absence, names))
                .toList();
    }

    @GetMapping("/absences/{id}")
    AbsenceDetailBody get(@PathVariable long id) {
        return AbsenceDetailBody.of(absences.get(absences.targetOf(id), id), names);
    }

    @PostMapping("/absences/{id}/extend-expected-return")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "Lượt vắng đã đóng",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> extend(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ExtendExpectedReturnBody body) {
        Target target = absences.targetOf(id);
        absences.extendExpectedReturn(target, id, body.expectedReturnAt(), body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/absences/{id}/release-bed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã giải phóng"),
        @ApiResponse(
                responseCode = "403",
                description = "Chỉ Quản lý viện",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lượt vắng đã đóng, giường đã giải phóng",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    AbsenceDetailBody releaseBed(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        return AbsenceDetailBody.of(absences.releaseBed(absences.targetOf(id), id, body.reason()), names);
    }

    @PostMapping("/absences/{id}/correct")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã đính chính hoặc đã lập yêu cầu"),
        @ApiResponse(
                responseCode = "409",
                description = "Không đính chính được",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> correct(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CorrectionRequestBody body) {
        corrections.create(
                AbsenceService.RECORD_TYPE, id, body.type().correctionType(), body.correctedContent(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
