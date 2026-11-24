package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.api.dto.FamilyNotice;
import com.eldercare.backend.incident.internal.incident.service.IncidentFamilyNoticeService;
import com.eldercare.backend.incident.web.request.RecordIncidentRequest.FamilyNoticeBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ghi nhận việc đã báo người nhà của một sự cố: ai, lúc nào, cách nào (BF-06). */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentFamilyNoticeController {
    private final IncidentFamilyNoticeService notices;
    private final CurrentUser currentUser;

    public IncidentFamilyNoticeController(IncidentFamilyNoticeService notices, CurrentUser currentUser) {
        this.notices = notices;
        this.currentUser = currentUser;
    }

    @GetMapping("/{id}/family-notice")
    public com.eldercare.backend.incident.internal.incident.domain.FamilyNoticeStatus status(@PathVariable long id) {
        return notices.status(id, currentUser);
    }

    @PostMapping("/{id}/family-notice")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã ghi nhận việc báo người nhà"),
        @ApiResponse(
                responseCode = "409",
                content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/problem+json",
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                                implementation = com.eldercare.backend.incident.web.response.M05ProblemBody.class)))
    })
    public ResponseEntity<Void> record(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody FamilyNoticeBody request) {
        notices.record(
                id,
                request == null
                        ? null
                        : new FamilyNotice(FamilyNotice.INFORMED, request.at(), request.method(), request.note()),
                currentUser);
        return ResponseEntity.ok().build();
    }
}
