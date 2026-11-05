package com.eldercare.backend.belongings.web;

import com.eldercare.backend.belongings.internal.belonging.service.BelongingPortalQueries;
import com.eldercare.backend.belongings.web.response.PortalBelongingResponse;
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
    List<PortalBelongingResponse> belongings(@PathVariable long residentId) {
        return queries.view(residentId).stream()
                .map(PortalBelongingResponse::of)
                .toList();
    }
}
