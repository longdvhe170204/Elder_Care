package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.internal.adjustment.service.CostAdjustmentService;
import com.eldercare.backend.billing.web.request.CostAdjustmentRequest;
import com.eldercare.backend.billing.web.response.CostProblemBody;
import com.eldercare.backend.billing.web.response.PostFinalizationChangeBody;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/costs")
class CostAdjustmentController {
    private final CostAdjustmentService service;

    CostAdjustmentController(CostAdjustmentService service) {
        this.service = service;
    }

    @PostMapping("/items/{id}/adjust")
    @ApiResponses({
        @ApiResponse(responseCode = "201", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    ResponseEntity<Void> adjust(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CostAdjustmentRequest body) {
        service.adjust(id, body.amount(), body.adjustmentType(), body.reason(), body.sourceChangeId());
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/post-finalization-changes")
    List<PostFinalizationChangeBody> changes(@RequestParam(required = false) Long residentId) {
        return service.changes(residentId).stream()
                .map(PostFinalizationChangeBody::of)
                .toList();
    }
}
