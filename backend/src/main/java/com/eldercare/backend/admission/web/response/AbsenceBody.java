package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.absence.domain.Absence;
import com.eldercare.backend.admission.internal.absence.domain.AbsenceType;
import com.eldercare.backend.admission.internal.absence.service.AbsenceNames;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Lượt vắng (contracts/api-004.yaml, schema {@code Absence}; FR-048). Thời điểm là giá trị hiện hành, đã áp đính chính (FR-061).
 * {@code holdBed}: giường còn được giữ, tới khi có lệnh Giải phóng giường (FR-055, Q-395).
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

    public static AbsenceBody of(Absence absence, AbsenceNames names) {
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
                absence.holdBed(),
                absence.open() ? Status.OPEN : Status.CLOSED);
    }
}
