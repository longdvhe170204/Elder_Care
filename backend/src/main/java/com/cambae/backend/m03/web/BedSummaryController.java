package com.cambae.backend.m03.web;

import com.cambae.backend.m03.api.dto.ResidentGender;
import com.cambae.backend.m03.api.spi.*;
import com.cambae.backend.m03.internal.bed.service.BedCapacityQueries;
import com.cambae.backend.m03.web.response.BedSummaryResponse;
import org.springframework.web.bind.annotation.*;

/** T027: thống kê theo đúng bộ lọc đã duyệt. */
@RestController
public class BedSummaryController {
    private final BedCapacityQueries queries;

    public BedSummaryController(BedCapacityQueries queries) {
        this.queries = queries;
    }

    @GetMapping("/api/v1/beds/summary")
    public BedSummaryResponse summary(
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) CareLevelCode careLevel,
            @RequestParam(required = false) ResidentGender gender,
            @RequestParam(required = false) com.cambae.backend.m03.web.request.BedResidencyType residencyType) {
        var result = queries.summary(
                floorId, careLevel, gender, residencyType == null ? null : ResidencyType.valueOf(residencyType.name()));
        return new BedSummaryResponse(result.byState(), result.usable(), result.emptyMatching());
    }
}
