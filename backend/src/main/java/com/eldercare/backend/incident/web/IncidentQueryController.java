package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.domain.IncidentDetails;
import com.eldercare.backend.incident.internal.incident.service.IncidentQueryService;
import com.eldercare.backend.incident.web.request.RecordIncidentRequest;
import com.eldercare.backend.incident.web.response.ActivationResultResponse.ActivationIncident;
import com.eldercare.backend.incident.web.response.ActivationResultResponse.ActivationNotifiedRecipient;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Đọc sự cố {@code GET /incidents}, {@code GET /incidents/{id}} (FR-045, FR-048b; T037). */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentQueryController {
    private final IncidentQueryService queries;
    private final CurrentUser currentUser;

    private static final String STORED_CANCELLED = "CANCELLED";
    private static final String VOIDED = "VOIDED";

    public IncidentQueryController(IncidentQueryService queries, CurrentUser currentUser) {
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public List<ActivationIncident> list(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) Long floorAreaId) {
        List<String> stored = status == null
                ? null
                : status.stream().map(IncidentQueryController::stored).toList();
        return queries.list(stored, residentId, floorAreaId, currentUser).stream()
                .map(IncidentQueryController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ActivationIncident detail(@PathVariable long id) {
        return toResponse(queries.detail(id, currentUser));
    }

    /** Schema {@code Incident}; người ghi ngoài phạm vi chỉ có nhận dạng (FR-048) nên không đọc được qua đây. */
    public static ActivationIncident toResponse(IncidentDetails details) {
        var incident = details.incident();
        var content = new RecordIncidentRequest(
                incident.residentId(),
                incident.incidentTypeId(),
                incident.context(),
                incident.occurredAt(),
                incident.discoveredAt(),
                incident.floorAreaId(),
                incident.location(),
                incident.ongoingActivity(),
                incident.description(),
                incident.initialLevel(),
                incident.lowerLevelReason(),
                incident.initialResponse(),
                false);
        List<ActivationNotifiedRecipient> notified = details.notified().stream()
                .map(view -> new ActivationNotifiedRecipient(
                        view.recipientType() + ":" + view.recipientId(),
                        view.channel(),
                        view.sentAt().orElse(null),
                        view.viewedAt().orElse(null)))
                .toList();
        return new ActivationIncident(
                incident.id(),
                content,
                incident.currentTypeId(),
                published(incident.status()),
                incident.level(),
                incident.wasEmergency(),
                incident.escalationLevel(),
                details.assigneeName(),
                incident.receivedAt(),
                incident.outOfScope(),
                incident.noOnDutyDoctor(),
                details.progressions(),
                details.levelChanges(),
                details.handlers(),
                notified,
                incident.resolution(),
                details.allowedCommands().stream().sorted().toList(),
                List.of());
    }

    /** api-007 {@code IncidentStatus}: Đã hủy lưu {@code CANCELLED}, hợp đồng gọi {@code VOIDED}. */
    private static String published(String status) {
        return STORED_CANCELLED.equals(status) ? VOIDED : status;
    }

    private static String stored(String status) {
        return VOIDED.equals(status) ? STORED_CANCELLED : status;
    }
}
