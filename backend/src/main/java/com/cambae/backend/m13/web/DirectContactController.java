package com.cambae.backend.m13.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m13.internal.directcontact.service.DirectContactService;
import com.cambae.backend.m13.web.request.DirectContactResultBody;
import com.cambae.backend.m13.web.response.DirectContactBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
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
 * Danh sách "cần liên hệ trực tiếp" và kết quả liên hệ theo contracts/api-009.yaml (thẻ {@code direct-contact}; 009
 * T028). Quyền theo bảng FR-039: Hành chính thực hiện, Quản lý viện xem.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "direct-contact")
public class DirectContactController {

    private final DirectContactService contacts;

    public DirectContactController(DirectContactService contacts) {
        this.contacts = contacts;
    }

    @GetMapping("/direct-contacts")
    @ApiResponse(responseCode = "200", description = "OK")
    List<DirectContactBody> list(@RequestParam(required = false) DirectContactBody.Status status) {
        return contacts.list(Optional.ofNullable(status).map(Enum::name)).stream()
                .map(DirectContactBody::of)
                .toList();
    }

    @PostMapping("/direct-contacts/{id}/results")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> record(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody DirectContactResultBody body) {
        contacts.record(
                id,
                body.result(),
                body.occurredAt().toInstant(),
                Optional.ofNullable(body.reason()),
                Optional.ofNullable(body.note()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
