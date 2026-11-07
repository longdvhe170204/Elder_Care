package com.eldercare.backend.incident.web;

import com.eldercare.backend.incident.internal.infection.service.ContactListService;
import com.eldercare.backend.incident.web.request.ConfirmContactListRequest;
import com.eldercare.backend.incident.web.request.ContactBody;
import com.eldercare.backend.incident.web.request.IncidentReasonRequest;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lệnh trên danh sách tiếp xúc theo bảng FR-061: thêm, bỏ, loại người, xác nhận. */
@RestController
@RequestMapping("/api/v1/contact-lists/{id}")
class ContactListCommandController {
    private final ContactListService service;

    ContactListCommandController(ContactListService service) {
        this.service = service;
    }

    @PostMapping("/contacts")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ResponseEntity<Void> add(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ContactBody contact) {
        service.add(service.listTarget(id), id, contact.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/contacts/{contactId}/remove")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    ResponseEntity<Void> remove(
            @PathVariable long id,
            @PathVariable long contactId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        service.remove(service.listTarget(id), id, contactId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/contacts/{contactId}/exclude")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    ResponseEntity<Void> exclude(
            @PathVariable long id,
            @PathVariable long contactId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody IncidentReasonRequest request) {
        service.exclude(service.listTarget(id), id, contactId, request.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ResponseEntity<Void> confirm(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) ConfirmContactListRequest request) {
        boolean noContacts = request != null && Boolean.TRUE.equals(request.noContacts());
        service.confirm(service.listTarget(id), id, noContacts, request == null ? null : request.reason());
        return ResponseEntity.ok().build();
    }
}
