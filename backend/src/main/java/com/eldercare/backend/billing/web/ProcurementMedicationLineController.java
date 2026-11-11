package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.internal.procurement.service.MedicationLinesAwaitingIntake;
import com.eldercare.backend.billing.web.response.ProcurementMedicationLineResponse;
import com.eldercare.backend.billing.web.response.ProcurementProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Q-410 (api-019 tag procurement-requests): dòng thuốc đã mua chờ tiếp nhận cho màn tiếp nhận thuốc cung ứng của 006. */
@RestController
@RequestMapping("/api/v1/procurement-medication-lines")
public class ProcurementMedicationLineController {
    private final MedicationLinesAwaitingIntake lines;

    public ProcurementMedicationLineController(MedicationLinesAwaitingIntake lines) {
        this.lines = lines;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProcurementProblemBody.class)))
    public List<ProcurementMedicationLineResponse> list() {
        return lines.list().stream().map(ProcurementMedicationLineResponse::of).toList();
    }
}
