package com.eldercare.backend.staffing.handover.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.staffing.handover.internal.domain.*;
import com.eldercare.backend.staffing.handover.internal.service.*;
import com.eldercare.backend.staffing.handover.web.request.HandoverRequests;
import com.eldercare.backend.staffing.handover.web.response.HandoverBody;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T029: lệnh theo hợp đồng; quyền và transaction kiểm ở HandoverService. */
@RestController
@RequestMapping("/api/v1")
class HandoverController {
    private final HandoverService service;
    private final FieldVisibilityApi fields;
    private final ObjectProvider<CurrentUser> users;

    HandoverController(HandoverService service, FieldVisibilityApi fields, ObjectProvider<CurrentUser> users) {
        this.service = service;
        this.fields = fields;
        this.users = users;
    }

    @GetMapping("/shifts/{id}/handovers")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = HandoverBody.class))),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<HandoverBody> forShift(@PathVariable long id) {
        return response(service.forShift(id));
    }

    @PostMapping("/shifts/{id}/handovers")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = HandoverBody.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<HandoverBody> create(@PathVariable long id, @RequestHeader("Idempotency-Key") UUID key) {
        var h = service.createHandover(id);
        return ResponseEntity.created(URI.create("/api/v1/handovers/" + h.id())).body(body(h));
    }

    @GetMapping("/handovers/{id}")
    ResponseEntity<HandoverBody> get(@PathVariable long id) {
        return response(service.record(id));
    }

    private ResponseEntity<HandoverBody> response(HandoverRecord h) {
        return ResponseEntity.ok().eTag(VersionEtag.of(h.version())).body(body(h));
    }

    @GetMapping("/handovers/{id}/source-records")
    List<HandoverBody.Source> sources(@PathVariable long id, @RequestParam(required = false) Boolean afterPreparation) {
        var h = service.record(id);
        return service.sources(id, Boolean.TRUE.equals(afterPreparation)).stream()
                .map(r -> {
                    boolean health = health(h, r.residentId());
                    return new HandoverBody.Source(
                            r.group(),
                            r.sourceType(),
                            r.sourceId(),
                            r.residentId(),
                            r.roomId(),
                            health ? r.description() : null,
                            health ? r.currentStatus() : null,
                            health && r.critical(),
                            r.hasItem(),
                            r.dataUnavailable());
                })
                .toList();
    }

    @PostMapping("/handovers/{id}/items")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = HandoverBody.Item.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> add(
            @PathVariable long id,
            @RequestHeader("If-Match") String match,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody HandoverRequests.Item input) {
        long child = service.addHandoverItem(id, match, input.input());
        return ResponseEntity.created(URI.create("/api/v1/handovers/" + id + "/items/" + child))
                .build();
    }

    @PutMapping("/handovers/{id}/items/{itemId}")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = HandoverBody.Item.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> update(
            @PathVariable long id,
            @PathVariable long itemId,
            @RequestHeader("If-Match") String match,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody HandoverRequests.Item input) {
        service.updateItem(id, itemId, match, input.input());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/handovers/{id}/items/{itemId}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> delete(
            @PathVariable long id, @PathVariable long itemId, @RequestHeader("If-Match") String match) {
        service.deleteItem(id, itemId, match);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/handovers/{id}/assessments")
    @ApiResponse(responseCode = "200", content = @Content)
    ResponseEntity<Void> assess(
            @PathVariable long id,
            @RequestHeader("If-Match") String match,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody HandoverRequests.Assessments input) {
        service.assess(id, match, input.general(), input.residents());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/handovers/{id}/complete")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> complete(@PathVariable long id, @RequestHeader("Idempotency-Key") UUID key) {
        service.complete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/handovers/{id}/confirm")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> confirm(@PathVariable long id, @RequestHeader("Idempotency-Key") UUID key) {
        service.confirm(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/handovers/{id}/comments")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> comment(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody HandoverRequests.Comment input) {
        service.comment(id, input.content());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/handovers/{id}/supplement-and-resubmit")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> resubmit(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody HandoverRequests.Resubmit input) {
        service.resubmit(
                id,
                input.replyMap(),
                input.additionalItems() == null
                        ? List.of()
                        : input.additionalItems().stream()
                                .map(HandoverRequests.Item::input)
                                .toList());
        return ResponseEntity.ok().build();
    }

    private boolean health(HandoverRecord h, Long resident) {
        return fields.canView(
                        users.getObject(),
                        InfoGroup.HEALTH,
                        new Target(
                                "M09.HANDOVER",
                                Long.toString(h.id()),
                                resident,
                                service.target(h.id()).floorAreaId()))
                == Visibility.FULL;
    }

    @SuppressWarnings("unchecked")
    private HandoverBody body(HandoverRecord h) {
        var hidden = new ArrayList<String>();
        var items = new ArrayList<HandoverBody.Item>();
        Map<Long, HandoverSource> current;
        try {
            current = service.currentStates(h.id());
        } catch (com.eldercare.backend.common.error.BusinessException ex) {
            current = Map.of();
        }
        for (var i : service.items(h.id())) {
            boolean health = health(h, i.residentId());
            String prefix = "items[" + items.size() + "].";
            if (!health)
                hidden.addAll(List.of(
                        prefix + "description",
                        prefix + "note",
                        prefix + "statusAtFinalization",
                        prefix + "currentStatus"));
            var source = current.get(i.id());
            items.add(new HandoverBody.Item(
                    i.id(),
                    i.group(),
                    i.sourceType(),
                    i.sourceId(),
                    i.residentId(),
                    i.roomId(),
                    health ? i.description() : null,
                    health ? i.note() : null,
                    i.resolved(),
                    i.section(),
                    health ? i.statusAtFinalization() : null,
                    health && source != null ? source.currentStatus() : null,
                    i.noteAuthor()));
        }
        var assessments = new ArrayList<Map<String, Object>>();
        for (var a : service.assessments(h.id())) {
            var copy = new LinkedHashMap<>(a);
            if (!health(h, ((Number) a.get("residentId")).longValue())) {
                copy.remove("content");
                hidden.add("residentAssessments[" + assessments.size() + "].content");
            }
            assessments.add(copy);
        }
        var comments = new ArrayList<HandoverBody.Comment>();
        boolean seeFeedback = health(h, null);
        for (var c : service.comments(h.id())) {
            var replies = new ArrayList<Map<String, Object>>();
            int ci = comments.size();
            for (var r : (List<Map<String, Object>>) c.get("replies")) {
                var copy = new LinkedHashMap<>(r);
                if (!seeFeedback) {
                    copy.remove("content");
                    hidden.add("comments[" + ci + "].replies[" + replies.size() + "].content");
                }
                replies.add(copy);
            }
            if (!seeFeedback) hidden.add("comments[" + ci + "].content");
            comments.add(new HandoverBody.Comment(
                    ((Number) c.get("id")).longValue(),
                    seeFeedback ? (String) c.get("content") : null,
                    (String) c.get("sender"),
                    (Instant) c.get("at"),
                    replies));
        }
        return new HandoverBody(
                h.id(),
                h.shiftId(),
                h.nextShiftId(),
                h.handoverStaffId(),
                h.status(),
                h.finalizedAt(),
                h.confirmedAt(),
                service.general(h.id()),
                assessments,
                items,
                comments,
                service.allowedCommands(h.id()),
                hidden);
    }
}
