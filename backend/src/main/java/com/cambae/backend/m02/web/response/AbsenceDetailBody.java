package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.absence.domain.AbsenceDay;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.absence.domain.BedHoldChoice;
import com.cambae.backend.m02.internal.absence.domain.BedHoldDecision;
import com.cambae.backend.m02.internal.absence.domain.ExpectedReturnChange;
import com.cambae.backend.m02.internal.absence.service.AbsenceNames;
import com.cambae.backend.m02.internal.absence.service.AbsenceService.Detail;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Lượt vắng kèm kết quả theo ngày, quyết định giữ giường và lịch sử dự kiến trở lại (contracts/api-004.yaml, schema {@code
 * AbsenceDetail}, {@code allOf} của {@code Absence}; FR-048, FR-052, FR-055).
 */
@Schema(name = "AbsenceDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AbsenceDetailBody(
        long id,
        long residentId,
        AbsenceType absenceType,
        Instant departedAt,
        Instant expectedReturnAt,
        Instant returnedAt,
        String pickedUpBy,
        String handoverStaff,
        boolean takesMedication,
        boolean holdBed,
        AbsenceBody.Status status,
        List<ExpectedReturnChangeBody> expectedReturnHistory,
        List<AbsenceDayBody> absenceDays,
        List<DecisionBody> decisions,
        List<String> hiddenFields) {

    public record ExpectedReturnChangeBody(Instant oldValue, Instant newValue, String reason, Instant at) {}

    public record AbsenceDayBody(
            LocalDate date,
            int sequenceNo,
            BigDecimal coefficient,
            boolean holdBed,
            String policyLineSource,
            boolean pendingDecision,
            boolean noPolicyLine) {}

    public record DecisionBody(
            BedHoldChoice choice, BigDecimal coefficient, LocalDate reviewDate, String decidedBy, Instant at) {}

    public static AbsenceDetailBody of(Detail detail, AbsenceNames names) {
        AbsenceBody base = AbsenceBody.of(detail.absence(), detail.holdBed(), names);
        return new AbsenceDetailBody(
                base.id(),
                base.residentId(),
                base.absenceType(),
                base.departedAt(),
                base.expectedReturnAt(),
                base.returnedAt(),
                base.pickedUpBy(),
                base.handoverStaff(),
                base.takesMedication(),
                base.holdBed(),
                base.status(),
                detail.expectedReturnHistory().stream()
                        .map(AbsenceDetailBody::history)
                        .toList(),
                detail.days().stream().map(AbsenceDetailBody::day).toList(),
                detail.decisions().stream().map(d -> decision(d, names)).toList(),
                List.of());
    }

    private static ExpectedReturnChangeBody history(ExpectedReturnChange change) {
        return new ExpectedReturnChangeBody(change.oldValue(), change.newValue(), change.reason(), change.at());
    }

    private static AbsenceDayBody day(AbsenceDay day) {
        return new AbsenceDayBody(
                day.date(),
                day.sequenceNo(),
                day.coefficient(),
                day.holdBed(),
                day.policyLineSource(),
                day.pendingDecision(),
                day.noPolicyLine());
    }

    private static DecisionBody decision(BedHoldDecision decision, AbsenceNames names) {
        return new DecisionBody(
                decision.choice(),
                decision.coefficient(),
                decision.reviewDate(),
                names.account(decision.decidedBy()),
                decision.decidedAt());
    }
}
