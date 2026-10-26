package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.absence.service.AbsenceNames;
import com.cambae.backend.m02.internal.absence.service.AbsenceService.Summary;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Lượt vắng (contracts/api-004.yaml, schema {@code Absence}; FR-048). Thời điểm là giá trị hiện hành, đã áp đính chính
 * (FR-061).
 */
@Schema(name = "Absence")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AbsenceBody(
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
        Status status) {

    public enum Status {
        OPEN,
        CLOSED
    }

    public static AbsenceBody of(Summary summary, AbsenceNames names) {
        return of(summary.absence(), summary.holdBed(), names);
    }

    public static AbsenceBody of(Absence absence, boolean holdBed, AbsenceNames names) {
        return new AbsenceBody(
                absence.id(),
                absence.residentId(),
                absence.type(),
                absence.departedAt(),
                absence.expectedReturnAt(),
                absence.returnedAt(),
                absence.pickedUpById() == null ? null : names.familyMember(absence.pickedUpById()),
                names.staff(absence.handoverStaffId()),
                absence.takesMedication(),
                holdBed,
                absence.open() ? Status.OPEN : Status.CLOSED);
    }
}
