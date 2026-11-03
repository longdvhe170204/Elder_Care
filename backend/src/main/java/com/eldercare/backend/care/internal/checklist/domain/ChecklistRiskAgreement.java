package com.eldercare.backend.care.internal.checklist.domain;

import com.eldercare.backend.care.internal.careplan.domain.RiskDecisionMaker;
import com.eldercare.backend.resident.api.dto.SelfDeterminationAssessment;
import java.time.*;

/** Chỉ các trường thẻ yêu cầu; không giả lập danh sách người tham gia/ý kiến chưa tải. */
public record ChecklistRiskAgreement(
        long id,
        long residentId,
        String choice,
        String discussedRisks,
        boolean residentParticipates,
        long assessingDoctorId,
        SelfDeterminationAssessment assessment,
        Instant assessedAt,
        RiskDecisionMaker decisionMaker,
        LocalDate reviewDate) {}
