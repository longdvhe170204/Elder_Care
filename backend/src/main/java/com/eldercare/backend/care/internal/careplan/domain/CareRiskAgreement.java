package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.resident.api.dto.SelfDeterminationAssessment;
import java.time.*;
import java.util.*;

/** R-15: một dòng kind RISK_AGREEMENT trong care_plan_item, các danh sách con ở record_event. */
@DataGroup(DataGroup.Group.BUSINESS)
public record CareRiskAgreement(
        long id,
        long carePlanId,
        Long sourceAgreementId,
        String choice,
        String discussedRisks,
        boolean residentParticipates,
        List<RiskParticipant> participants,
        long assessingDoctorId,
        SelfDeterminationAssessment selfDeterminationAssessment,
        Instant assessedAt,
        RiskDecisionMaker decisionMaker,
        List<RiskOpinionEntry> opinions,
        LocalDate reviewDate,
        Long recordedBy,
        Instant recordedAt) {
    public CareRiskAgreement {
        participants = List.copyOf(participants);
        opinions = List.copyOf(opinions);
    }

    public long lineage() {
        return sourceAgreementId == null ? id : sourceAgreementId;
    }
}
