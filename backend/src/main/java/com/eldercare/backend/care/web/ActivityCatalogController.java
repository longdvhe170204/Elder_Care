package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.care.internal.activity.service.ActivityCatalogService;
import com.eldercare.backend.care.web.request.ActivityTypeWrite;
import com.eldercare.backend.care.web.response.ActivityTypeResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activity-types")
public class ActivityCatalogController {
    private final ActivityCatalogService catalog;

    public ActivityCatalogController(ActivityCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public List<ActivityTypeResponse> list() {
        return catalog.list(Target.UNSPECIFIED).stream()
                .map(ActivityTypeResponse::of)
                .toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public void write(@RequestHeader("Idempotency-Key") String key, @RequestBody ActivityTypeWrite body) {
        catalog.write(
                Target.UNSPECIFIED,
                body.id(),
                body.name(),
                body.groupActivity(),
                body.offSite(),
                body.interestGroupIds(),
                body.status());
    }
}
