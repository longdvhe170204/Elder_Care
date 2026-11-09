package com.eldercare.backend.incident.web;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.incident.internal.infection.domain.ContactGroup;
import com.eldercare.backend.incident.internal.infection.service.ContactListService;
import com.eldercare.backend.incident.internal.infection.service.ContactTracingService;
import com.eldercare.backend.incident.web.response.ContactListResponse;
import com.eldercare.backend.incident.web.response.ContactLookupResponse;
import com.eldercare.backend.incident.web.response.FamilyContactToNotifyResponse;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tra cứu truy vết (FR-060), lập và xem danh sách tiếp xúc của sự cố (FR-061). */
@RestController
@RequestMapping("/api/v1/incidents/{id}")
class ContactListController {
    private final ContactListService service;
    private final ContactTracingService tracing;

    ContactListController(ContactListService service, ContactTracingService tracing) {
        this.service = service;
        this.tracing = tracing;
    }

    @GetMapping("/contact-lookup")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ContactLookupResponse lookup(
            @PathVariable long id,
            @Parameter(
                            array =
                                    @ArraySchema(
                                            schema =
                                                    @Schema(
                                                            type = "string",
                                                            allowableValues = {
                                                                "SAME_ROOM",
                                                                "ACTIVITY",
                                                                "CARE_STAFF",
                                                                "FAMILY_VISIT"
                                                            })))
                    @RequestParam(required = false)
                    List<String> group) {
        return ContactLookupResponse.from(tracing.lookup(service.target(id), id, groups(group)));
    }

    @GetMapping("/contact-list")
    ContactListResponse view(@PathVariable long id) {
        return ContactListResponse.from(service.view(service.target(id), id));
    }

    @GetMapping("/family-contacts-to-notify")
    List<FamilyContactToNotifyResponse> familyContactsToNotify(@PathVariable long id) {
        return service.familyContactsToNotify(service.target(id), id).stream()
                .map(FamilyContactToNotifyResponse::from)
                .toList();
    }

    @PostMapping("/contact-list")
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
        service.createDraft(service.target(id), id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static Set<ContactGroup> groups(List<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        var result = EnumSet.noneOf(ContactGroup.class);
        for (String value : values) {
            ContactGroup group;
            try {
                group = ContactGroup.valueOf(value);
            } catch (IllegalArgumentException invalid) {
                throw new InvalidValueException("Nhóm tra cứu không hợp lệ: " + value);
            }
            if (group == ContactGroup.OTHER) throw new InvalidValueException("Nhóm tra cứu không hợp lệ: " + value);
            result.add(group);
        }
        return result;
    }
}
