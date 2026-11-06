package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementCommandInput;
import com.eldercare.backend.billing.internal.procurement.service.ProcurementRequestService;
import com.eldercare.backend.billing.web.request.ProcurementRequestCommandWrite;
import com.eldercare.backend.billing.web.request.ProcurementRequestWrite;
import com.eldercare.backend.billing.web.response.ProcurementProblemBody;
import com.eldercare.backend.billing.web.response.ProcurementRequestResponse;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 019 T008 (api-019 tag procurement-requests; FR-008 → FR-008b): mỗi lệnh một đường dẫn theo bảng FR-008. */
@RestController
@RequestMapping("/api/v1/procurement-requests")
public class ProcurementRequestController {
    private final ProcurementRequestService requests;

    public ProcurementRequestController(ProcurementRequestService requests) {
        this.requests = requests;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public List<ProcurementRequestResponse> list(
            @RequestParam(required = false) String status, @RequestParam(required = false) Long departmentId) {
        return requests.list(departmentId, status(status)).stream()
                .map(ProcurementRequestResponse::of)
                .toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProcurementProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProcurementProblemBody.class)))
    public ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") String key, @RequestBody ProcurementRequestWrite body) {
        long id = requests.create(body.content());
        return ResponseEntity.created(URI.create("/api/v1/procurement-requests/" + id))
                .build();
    }

    @PutMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProcurementProblemBody.class)))
    public void update(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ProcurementRequestWrite body) {
        requests.update(requests.target(id), id, ifMatch, body.content());
    }

    @PostMapping("/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProcurementProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProcurementProblemBody.class)))
    public void command(
            @PathVariable long id,
            @PathVariable
                    @Parameter(
                            schema =
                                    @Schema(
                                            allowableValues = {
                                                "submit",
                                                "withdraw",
                                                "cancel",
                                                "approve",
                                                "reject",
                                                "record-purchased",
                                                "confirm-match",
                                                "return-for-reconciliation",
                                                "record-discrepancy",
                                                "resolve-discrepancy"
                                            }))
                    String command,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody(required = false) ProcurementRequestCommandWrite body) {
        var target = requests.target(id);
        ProcurementCommandInput input = body == null ? ProcurementCommandInput.EMPTY : body.input();
        switch (command) {
            case "submit" -> requests.submit(target, id);
            case "withdraw" -> requests.withdraw(target, id);
            case "cancel" -> requests.cancel(target, id, input);
            case "approve" -> requests.approve(target, id, input);
            case "reject" -> requests.reject(target, id, input);
            case "record-purchased" -> requests.recordPurchased(target, id, input);
            case "confirm-match" -> requests.confirmMatch(target, id);
            case "return-for-reconciliation" -> requests.returnForReconciliation(target, id, input);
            case "record-discrepancy" -> requests.recordDiscrepancy(target, id, input);
            case "resolve-discrepancy" -> requests.resolveDiscrepancy(target, id, input);
            default -> throw new NotFoundException();
        }
    }

    private static ProcurementStatus status(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ProcurementStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Trạng thái không hợp lệ");
        }
    }
}
