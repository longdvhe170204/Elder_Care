package com.cambae.backend.m09.credential.web;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.credential.internal.service.CredentialService;
import com.cambae.backend.m09.credential.web.request.*;
import com.cambae.backend.m09.credential.web.response.*;
import com.cambae.backend.m09.web.response.M09ProblemBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T006: chỉ cài các endpoint danh mục, đọc và thêm có trong api-008. */
@RestController
@RequestMapping("/api/v1")
class CredentialController {
    private final CredentialService credentials;
    private final BusinessTime time;

    CredentialController(CredentialService credentials, BusinessTime time) {
        this.credentials = credentials;
        this.time = time;
    }

    @GetMapping("/credential-types")
    List<CredentialTypeBody> types() {
        return credentials.types().stream().map(CredentialTypeBody::of).toList();
    }

    @PostMapping("/credential-types")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = CredentialTypeBody.class))))
    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content)
    ResponseEntity<Void> addType(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey, @Valid @RequestBody CredentialTypeInput input) {
        var result = credentials.addType(
                input.name(),
                input.group(),
                input.hasExpiry(),
                input.requiredForRoles() == null ? Set.of() : input.requiredForRoles(),
                Boolean.TRUE.equals(input.hasPracticeScopes()),
                !Boolean.FALSE.equals(input.active()));
        return ResponseEntity.created(URI.create("/api/v1/credential-types/" + result.id()))
                .build();
    }

    @GetMapping("/practice-scopes")
    List<PracticeScopeBody> scopes() {
        return credentials.practiceScopes().stream()
                .map(s -> new PracticeScopeBody(s.id(), s.name(), s.active(), s.code()))
                .toList();
    }

    @GetMapping("/staff/{id}/credentials")
    List<CredentialBody> list(@PathVariable long id) {
        return credentials.list(id).stream()
                .map(c -> CredentialBody.of(c, time))
                .toList();
    }

    @PostMapping("/staff/{id}/credentials")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = CredentialBody.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> add(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CredentialInputBody input) {
        var c = credentials.add(id, input.input());
        return ResponseEntity.created(URI.create("/api/v1/staff/" + id + "/credentials"))
                .build();
    }

    @PostMapping("/credentials/{id}/renew")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = CredentialBody.class))))
    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content)
    ResponseEntity<Void> renew(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CredentialInputBody input) {
        var c = credentials.renew(id, input.input());
        return ResponseEntity.created(URI.create("/api/v1/staff/" + c.staffId() + "/credentials"))
                .build();
    }

    @PostMapping("/credentials/{id}/revoke")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    ResponseEntity<Void> revoke(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CredentialCommands.Revoke input) {
        credentials.revoke(id, input.effectiveFrom(), input.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/credentials/{id}/correct")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema =
                                                    @Schema(
                                                            allOf = {
                                                                CredentialBody.class,
                                                                CredentialCommands.CorrectionReason.class
                                                            }))))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    ResponseEntity<Void> correct(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CredentialCommands.Correct input) {
        credentials.correct(id, input.input(), input.reason());
        return ResponseEntity.ok().build();
    }
}
