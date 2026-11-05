package com.eldercare.backend.family.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService;
import com.eldercare.backend.family.web.request.CreateRelationshipRequest;
import com.eldercare.backend.family.web.request.FamilyMemberRequest;
import com.eldercare.backend.family.web.request.FamilyRegistrationFormRequest;
import com.eldercare.backend.family.web.request.RelationshipCommandRequest;
import com.eldercare.backend.family.web.response.CreatedResponse;
import com.eldercare.backend.family.web.response.FamilyMemberDetailResponse;
import com.eldercare.backend.family.web.response.FamilyMemberResponse;
import com.eldercare.backend.family.web.response.RelationshipResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ người thân, quan hệ, phiếu đăng ký (012 T013; contracts/api-012.yaml, tag {@code family-members}). Phản hồi 201
 * và lệnh trả {@code id}, bản ghi hiện hành nhưng hợp đồng không khai nội dung nên OpenAPI không có. Mọi kiểm tra ở
 * {@link FamilyMemberService}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "family-members")
class FamilyMemberController {

    private static final String PROBLEM = "application/problem+json";

    private final FamilyMemberService service;

    FamilyMemberController(FamilyMemberService service) {
        this.service = service;
    }

    @GetMapping("/family-members")
    List<FamilyMemberResponse> search(@RequestParam(name = "q", required = false) String q) {
        return service.search(q).stream().map(FamilyMemberResponse::of).toList();
    }

    @PostMapping("/family-members")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu họ tên, số điện thoại",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody FamilyMemberRequest body) {
        FamilyMember created = service.createFamilyMember(body.data());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(new CreatedResponse(created.id()));
    }

    @GetMapping("/family-members/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "OK",
                headers = @Header(name = "ETag", schema = @Schema(type = "string")),
                content = @Content)
    })
    ResponseEntity<FamilyMemberDetailResponse> detail(@PathVariable long id) {
        FamilyMemberService.FamilyMemberDetail detail =
                service.familyMember(FamilyMemberService.familyMemberTarget(id), id);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(detail.familyMember().version()))
                .body(FamilyMemberDetailResponse.of(detail));
    }

    @PutMapping("/family-members/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "ID_DOCUMENT_REQUIRED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "VERSION_MISMATCH",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<FamilyMemberResponse> update(
            @PathVariable long id,
            @Parameter(in = ParameterIn.HEADER, required = true) @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody FamilyMemberRequest body) {
        FamilyMember updated =
                service.updateFamilyMember(FamilyMemberService.familyMemberTarget(id), id, ifMatch, body.data());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).body(FamilyMemberResponse.of(updated));
    }

    @GetMapping("/residents/{residentId}/relationships")
    List<RelationshipResponse> relationships(@PathVariable long residentId) {
        return service.relationships(FamilyMemberService.residentTarget(residentId)).stream()
                .map(RelationshipResponse::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/relationships")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "DBR_02_VIOLATED, REPRESENTATIVE_ALREADY_EXISTS, STATE_CHANGED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> createRelationship(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateRelationshipRequest body) {
        long id = service.createRelationship(FamilyMemberService.residentTarget(residentId), body.command())
                .id();
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse(id));
    }

    @PostMapping("/relationships/{id}/{command}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "DBR_02_VIOLATED, REPRESENTATIVE_ALREADY_EXISTS, STATE_CHANGED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> command(
            @PathVariable long id,
            @PathVariable
                    @Schema(
                            allowableValues = {
                                "change-primary-contact",
                                "set-first-representative",
                                "change-contact-order",
                                "end"
                            })
                    String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RelationshipCommandRequest body) {
        Target target = service.relationshipTarget(id);
        switch (command) {
            case "change-primary-contact" -> service.changePrimaryContact(target, id, body.reason());
            case "set-first-representative" ->
                service.setFirstRepresentative(target, id, body.reason(), body.evidenceFileId());
            case "change-contact-order" -> service.changeContactOrder(target, id, body.reason(), body.contactOrder());
            case "end" -> service.end(target, id, body.reason());
            default -> throw new NotFoundException();
        }
        return ResponseEntity.ok(new CreatedResponse(id));
    }

    @PostMapping("/residents/{residentId}/family-registration-forms")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "ID_DOCUMENT_REQUIRED, CONSENT_NOT_INCLUDED, REQUEST_NOT_ALLOWED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> recordRegistrationForm(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody FamilyRegistrationFormRequest body) {
        long id = service.recordRegistrationForm(
                FamilyMemberService.residentTarget(residentId),
                body.representativeId(),
                body.scanFileId(),
                body.commandLines());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse(id));
    }
}
