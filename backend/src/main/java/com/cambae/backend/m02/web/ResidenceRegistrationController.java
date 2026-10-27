package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService;
import com.cambae.backend.m02.web.request.CreateRegistrationBody;
import com.cambae.backend.m02.web.request.RegistrationCommandBody;
import com.cambae.backend.m02.web.response.RegistrationTaskBody;
import com.cambae.backend.m02.web.response.ResidenceRegistrationBody;
import com.cambae.backend.m15.account.api.dto.Target;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Khai báo tạm trú, lưu trú (018; contracts/api-018.yaml, tag {@code residence-registrations}). Phase 3 (US1): danh sách
 * cần làm, khai báo của người cao tuổi, lập khai báo, xác nhận thường trú cùng xã/phường; Phase 4 (US2): ghi nộp, ghi kết
 * quả; khai báo xóa (T014) và lập khai báo gia hạn (T012) thêm ở Phase 5, 6. Mọi logic và kiểm quyền ở service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "residence-registrations")
class ResidenceRegistrationController {

    private static final String PROBLEM = "application/problem+json";
    private static final String CONFIRM_LOCAL = "confirm-local-permanent-resident";
    private static final String RECORD_SUBMITTED = "record-submitted";
    private static final String RECORD_ACCEPTED = "record-accepted";
    private static final String RECORD_REJECTED = "record-rejected";

    private final ResidenceRegistrationService registrations;

    ResidenceRegistrationController(ResidenceRegistrationService registrations) {
        this.registrations = registrations;
    }

    @GetMapping("/residence-registrations/to-do")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    List<RegistrationTaskBody> toDo() {
        return registrations.toDo(Target.UNSPECIFIED).stream()
                .map(RegistrationTaskBody::of)
                .toList();
    }

    @GetMapping("/residents/{residentId}/residence-registrations")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    List<ResidenceRegistrationBody> ofResident(@PathVariable long residentId) {
        return registrations.ofResident(ResidenceRegistrationService.targetForResident(residentId), residentId).stream()
                .map(ResidenceRegistrationBody::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/residence-registrations")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "NOT_RESIDENTIAL_CARE, NO_REGISTRATION_OBLIGATION, REGISTRATION_IN_PROGRESS_EXISTS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidenceRegistrationBody create(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody CreateRegistrationBody body) {
        if (body.isRenewal()) {
            throw new InvalidValueException("Lập khai báo gia hạn chưa được cài (018 T012, Phase 5)");
        }
        return ResidenceRegistrationBody.of(registrations.createRegistration(
                ResidenceRegistrationService.targetForResident(residentId), residentId));
    }

    /**
     * Lệnh theo bảng FR-005. Đã cài: {@code confirm-local-permanent-resident} (T008), {@code record-submitted}, {@code
     * record-accepted}, {@code record-rejected} (T010). {@code record-deregistered} (T014) còn trả 422 {@code
     * INVALID_COMMAND} tạm, ngoài hợp đồng, tới khi được cài.
     */
    @PostMapping("/residence-registrations/{id}/{command}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "STATE_CHANGED, NOT_SUBMITTED, EVIDENCE_REQUIRED, EXPIRY_DATE_REQUIRED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    void command(
            @PathVariable long id,
            @PathVariable
                    @Parameter(
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {
                                                CONFIRM_LOCAL,
                                                RECORD_SUBMITTED,
                                                RECORD_ACCEPTED,
                                                RECORD_REJECTED,
                                                "record-deregistered"
                                            }))
                    String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody RegistrationCommandBody body) {
        Target target = registrations.targetOf(id);
        switch (command) {
            case CONFIRM_LOCAL -> registrations.confirmLocalPermanentResident(target, id, body.basisNote());
            case RECORD_SUBMITTED ->
                registrations.recordSubmitted(
                        target,
                        id,
                        new ResidenceRegistrationService.Submission(
                                body.submittedDate(), body.channel(), body.receivingAuthority(), body.dossierCode()));
            case RECORD_ACCEPTED ->
                registrations.recordAccepted(
                        target,
                        id,
                        new ResidenceRegistrationService.Acceptance(body.evidenceFileId(), body.expiryDate()));
            case RECORD_REJECTED -> registrations.recordRejected(target, id, body.reason());
            default ->
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "INVALID_COMMAND",
                        "Lệnh chưa được cài hoặc không có: " + command);
        }
    }
}
