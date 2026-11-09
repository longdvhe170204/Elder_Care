package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.internal.item.service.CostQueries;
import com.eldercare.backend.billing.web.response.CostPeriodBody;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
class CostPeriodController {
    private final CostQueries queries;

    CostPeriodController(CostQueries queries) {
        this.queries = queries;
    }

    @GetMapping("/api/v1/costs/periods")
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CostPeriodBody.class))))
    List<CostPeriodBody> periods() {
        return queries.periods().stream().map(CostPeriodBody::of).toList();
    }
}
