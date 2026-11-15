package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.internal.item.service.ManualCostService;
import com.eldercare.backend.billing.web.request.ManualCostItemRequest;
import com.eldercare.backend.billing.web.response.CostProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-021, BR-M11-04: lập khoản nhập tay. */
@RestController
@RequestMapping("/api/v1/costs")
class ManualCostController {
    private final ManualCostService service;

    ManualCostController(ManualCostService service) {
        this.service = service;
    }

    @PostMapping("/items/manual")
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
    ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ManualCostItemRequest body) {
        service.create(
                body.residentId(),
                body.description(),
                body.incurredOn(),
                body.quantity(),
                body.unit(),
                body.unitPrice(),
                body.reason(),
                body.fileIds());
        return ResponseEntity.status(201).build();
    }
}
