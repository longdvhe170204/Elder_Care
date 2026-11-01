package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.internal.checklist.domain.ChecklistRiskAgreement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** FR-030: thỏa thuận trả các trường bắt buộc của RiskAgreement; phần chi tiết ở kế hoạch. */
public record CareChecklistResponse(
        long shiftId,
        Instant loadedAt,
        List<CareTaskResponse> tasks,
        List<ResidentAgreements> riskAgreements,
        List<NonPerformanceReasonTypeResponse> nonPerformanceReasonTypes,
        List<MedicationChecklistPort.DoseView> medicationDoses,
        List<AlertReadPort.AlertView> openAlerts,
        Map<String, Object> handoverTasks) {
    public record ResidentAgreements(long residentId, List<Agreement> agreements) {}

    public record Agreement(
            long id,
            String choice,
            String discussedRisks,
            boolean residentParticipates,
            long assessingDoctorId,
            RiskAssessmentResponse selfDeterminationAssessment,
            RiskDecisionMakerResponse decisionMaker,
            LocalDate reviewDate) {
        public static Agreement of(ChecklistRiskAgreement a) {
            var assessment = a.assessment();
            var maker = a.decisionMaker();
            return new Agreement(
                    a.id(),
                    a.choice(),
                    a.discussedRisks(),
                    a.residentParticipates(),
                    a.assessingDoctorId(),
                    new RiskAssessmentResponse(
                            assessment.canSelfDetermine(),
                            assessment.supportMethod(),
                            assessment.reviewDate(),
                            a.assessedAt()),
                    new RiskDecisionMakerResponse(
                            maker.type(), maker.familyMemberId(), maker.doctorId(), maker.reason()),
                    a.reviewDate());
        }
    }
}
