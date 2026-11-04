package com.eldercare.backend.incident.web;

import com.eldercare.backend.incident.internal.infection.service.ContactListService;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lập danh sách tiếp xúc Nháp theo FR-061. */
@RestController
@RequestMapping("/api/v1/incidents/{id}/contact-list")
class ContactListController {
    private final ContactListService service;

    ContactListController(ContactListService service) {
        this.service = service;
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập bản Nháp", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ResponseEntity<Void> create(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        service.createDraft(id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
