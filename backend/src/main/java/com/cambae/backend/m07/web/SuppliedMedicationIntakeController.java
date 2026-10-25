package com.cambae.backend.m07.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m07.internal.domain.SuppliedMedicationIntake;
import com.cambae.backend.m07.internal.domain.SuppliedMedicationIntake.IntakeResult;
import com.cambae.backend.m07.internal.service.ActorNames;
import com.cambae.backend.m07.internal.service.SuppliedMedicationIntakeService;
import com.cambae.backend.m07.internal.service.SuppliedMedicationIntakeService.RecordIntake;
import com.cambae.backend.m07.web.request.NewSuppliedMedicationIntakeBody;
import com.cambae.backend.m07.web.response.SuppliedMedicationIntakeResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tiếp nhận thuốc cung ứng (api-006 tag {@code supplied-medication}; FR-054, FR-055). */
@RestController
@Tag(name = "supplied-medication")
public class SuppliedMedicationIntakeController {

    private final SuppliedMedicationIntakeService service;
    private final ActorNames names;

    public SuppliedMedicationIntakeController(SuppliedMedicationIntakeService service, ActorNames names) {
        this.service = service;
        this.names = names;
    }

    @PostMapping("/api/v1/supplied-medication-intakes")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
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
    public void record(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NewSuppliedMedicationIntakeBody body) {
        service.record(
                service.targetOfLine(body.requestLineId()),
                new RecordIntake(
                        body.requestLineId(),
                        body.concentration(),
                        body.dosageForm(),
                        body.receivedQuantity(),
                        body.unit(),
                        body.expiryDate(),
                        body.condition(),
                        IntakeResult.valueOf(body.result().name()),
                        body.discrepancy(),
                        body.reason(),
                        body.delivererName()));
    }

    @GetMapping("/api/v1/supplied-medication-intakes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public List<SuppliedMedicationIntakeResponse> list(@RequestParam("purchaseRequestId") long purchaseRequestId) {
        List<SuppliedMedicationIntake> found = service.intakesOf(purchaseRequestId);
        if (found.isEmpty()) {
            return List.of();
        }
        return service
                .list(
                        SuppliedMedicationIntakeService.residentTarget(
                                found.getFirst().getResidentId()),
                        purchaseRequestId)
                .stream()
                .map(i -> SuppliedMedicationIntakeResponse.from(i, names.of(i.getReceiverId())))
                .toList();
    }
}
