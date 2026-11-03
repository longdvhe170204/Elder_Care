package com.eldercare.backend.care.web.response;

import java.time.*;

public record RiskAssessmentResponse(
        boolean canSelfDetermine, String supportMethod, LocalDate reviewDate, Instant at) {}
