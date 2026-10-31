package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;

/** Projection HTTP theo CarePlan đã duyệt. */
@Schema(name = "CarePlan")
public record CarePlanResponse(
        long id,
        long residentId,
        int revision,
        CarePlanStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String createdBy,
        String approvedBy,
        Instant approvedAt,
        boolean selfApproved,
        String changeReason,
        List<CarePlanItemResponse> items,
        List<CareRiskAgreementResponse> riskAgreements,
        boolean noEffectivePlan,
        Set<String> allowedCommands) {}
