package com.eldercare.backend.care.web.response;

import java.util.List;

public record CarePlanItemChangeResponse(
        CarePlanItemResponse before, CarePlanItemResponse after, List<String> changedFields) {}
