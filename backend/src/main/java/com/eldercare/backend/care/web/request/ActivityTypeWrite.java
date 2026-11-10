package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ActivityTypeWrite(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        Boolean groupActivity,
        Boolean offSite,
        List<Long> interestGroupIds,
        String status) {}
