package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.care.internal.activity.service.ActivityCatalogService;
import com.eldercare.backend.care.web.request.InterestGroupWrite;
import com.eldercare.backend.care.web.response.InterestGroupResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Danh mục nhóm sở thích (014 FR-051; api-014 {@code /interest-groups}, thêm 2026-11-15, chủ dự án duyệt). */
@RestController
@RequestMapping("/api/v1/interest-groups")
public class InterestGroupController {
    private final ActivityCatalogService catalog;

    public InterestGroupController(ActivityCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public List<InterestGroupResponse> list() {
        return catalog.listInterestGroups(Target.UNSPECIFIED).stream()
                .map(InterestGroupResponse::of)
                .toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public void write(@RequestHeader("Idempotency-Key") String key, @RequestBody InterestGroupWrite body) {
        catalog.writeInterestGroup(Target.UNSPECIFIED, body.id(), body.name(), body.status());
    }
}
