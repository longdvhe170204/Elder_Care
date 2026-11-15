package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.internal.careplan.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;

/** Request đúng RiskAgreement đã duyệt; metadata nhận định/ý kiến do server ghi. */
@Schema(
        name = "RiskAgreementWrite",
        requiredProperties = {
            "choice",
            "discussedRisks",
            "residentParticipates",
            "assessingDoctorId",
            "selfDeterminationAssessment",
            "decisionMaker",
            "reviewDate"
        })
public record CareRiskAgreementWrite(
        Long id,
        Long sourceAgreementId,
        String choice,
        String discussedRisks,
        boolean residentParticipates,
        List<RiskParticipantWrite> participants,
        long assessingDoctorId,
        RiskAssessmentWrite selfDeterminationAssessment,
        RiskDecisionMakerWrite decisionMaker,
        List<RiskOpinionWrite> opinions,
        LocalDate reviewDate) {
    public CareRiskAgreement row(long planId, long actor, Instant at) {
        if (participants != null && participants.stream().anyMatch(Objects::isNull)
                || opinions != null && opinions.stream().anyMatch(Objects::isNull))
            throw new com.eldercare.backend.common.error.InvalidValueException(
                    "Người tham gia và ý kiến không được chứa dòng null");
        return new CareRiskAgreement(
                id == null ? 0 : id,
                planId,
                sourceAgreementId,
                choice,
                discussedRisks,
                residentParticipates,
                participants == null
                        ? List.of()
                        : participants.stream()
                                .map(p -> new RiskParticipant(p.type(), p.id(), true))
                                .toList(),
                assessingDoctorId,
                selfDeterminationAssessment == null ? null : selfDeterminationAssessment.toAssessment(),
                at,
                decisionMaker == null
                        ? null
                        : new RiskDecisionMaker(
                                decisionMaker.type(),
                                decisionMaker.familyMemberId(),
                                decisionMaker.doctorId(),
                                decisionMaker.reason()),
                opinions == null
                        ? List.of()
                        : opinions.stream()
                                .map(p ->
                                        new RiskOpinionEntry(new RiskOpinion(p.source(), p.content(), true), actor, at))
                                .toList(),
                reviewDate,
                actor,
                at);
    }
}
