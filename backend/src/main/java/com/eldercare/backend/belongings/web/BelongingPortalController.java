package com.eldercare.backend.belongings.web;

import com.eldercare.backend.belongings.internal.belonging.service.BelongingPortalQueries;
import com.eldercare.backend.belongings.web.response.PortalBelongingResponse;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Đồ gửi trên cổng người thân (013 T028; api-013 tag {@code family-portal}; FR-030, BR-M12-09). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "family-portal")
class BelongingPortalController {

    private final BelongingPortalQueries queries;

    BelongingPortalController(BelongingPortalQueries queries) {
        this.queries = queries;
    }

    @GetMapping("/family-portal/residents/{residentId}/belongings")
    @ApiResponse(
            responseCode = "403",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(responseCode = "200", description = "OK")
    List<PortalBelongingResponse> belongings(@PathVariable long residentId) {
        return queries.view(residentId).stream()
                .map(PortalBelongingResponse::of)
                .toList();
    }
}
