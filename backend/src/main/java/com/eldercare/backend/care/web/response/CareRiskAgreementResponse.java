package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.careplan.domain.CareRiskAgreement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Schema(name = "RiskAgreement")
public record CareRiskAgreementResponse(
        long id,
        Long sourceAgreementId,
        String choice,
        String discussedRisks,
        boolean residentParticipates,
        List<RiskParticipantResponse> participants,
        long assessingDoctorId,
        RiskAssessmentResponse selfDeterminationAssessment,
        RiskDecisionMakerResponse decisionMaker,
        List<RiskOpinionResponse> opinions,
        LocalDate reviewDate) {
    public static CareRiskAgreementResponse of(CareRiskAgreement row, Function<Long, String> name) {
        var assessment = row.selfDeterminationAssessment();
        var maker = row.decisionMaker();
        return new CareRiskAgreementResponse(
                row.id(),
                row.sourceAgreementId(),
                row.choice(),
                row.discussedRisks(),
                row.residentParticipates(),
                row.participants().stream()
                        .map(p -> new RiskParticipantResponse(p.type(), p.id()))
                        .toList(),
                row.assessingDoctorId(),
                new RiskAssessmentResponse(
                        assessment.canSelfDetermine(),
                        assessment.supportMethod(),
                        assessment.reviewDate(),
                        row.assessedAt()),
                new RiskDecisionMakerResponse(maker.type(), maker.familyMemberId(), maker.doctorId(), maker.reason()),
                row.opinions().stream()
                        .map(o -> new RiskOpinionResponse(
                                o.opinion().source(), o.opinion().content(), name.apply(o.recordedBy()), o.at()))
                        .toList(),
                row.reviewDate());
    }
}
