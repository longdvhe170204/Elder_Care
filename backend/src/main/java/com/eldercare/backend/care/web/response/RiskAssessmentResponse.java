package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;

@Schema(requiredProperties = {"canSelfDetermine", "supportMethod", "reviewDate"})
public record RiskAssessmentResponse(
        boolean canSelfDetermine, String supportMethod, LocalDate reviewDate, Instant at) {}
