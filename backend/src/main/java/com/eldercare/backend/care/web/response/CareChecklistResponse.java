package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.spi.*;
import java.time.Instant;
import java.util.*;

/**
 * FR-030: thỏa thuận rủi ro theo schema {@code RiskAgreement} của api-005 (cùng bản của kế hoạch chăm sóc, gồm người
 * tham gia, ý kiến; sửa 2026-11-10 khi đối chiếu hợp đồng).
 */
public record CareChecklistResponse(
        long shiftId,
        Instant loadedAt,
        List<CareTaskResponse> tasks,
        List<ResidentAgreements> riskAgreements,
        List<NonPerformanceReasonTypeResponse> nonPerformanceReasonTypes,
        List<MedicationChecklistPort.DoseView> medicationDoses,
        List<AlertReadPort.AlertView> openAlerts,
        Map<String, Object> handoverTasks) {
    public record ResidentAgreements(long residentId, List<CareRiskAgreementResponse> agreements) {}
}
