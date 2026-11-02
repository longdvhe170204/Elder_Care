package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.absence.domain.AbsenceType;
import com.eldercare.backend.admission.internal.absence.domain.ExpectedReturnChange;
import com.eldercare.backend.admission.internal.absence.service.AbsenceNames;
import com.eldercare.backend.admission.internal.absence.service.AbsenceService.Detail;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Lượt vắng kèm lịch sử dự kiến trở lại (contracts/api-004.yaml, schema {@code AbsenceDetail}, {@code allOf} của {@code Absence};
 * FR-048, FR-049; Q-395 bỏ kết quả theo ngày và quyết định giữ giường).
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
        List<String> hiddenFields) {

    public record ExpectedReturnChangeBody(Instant oldValue, Instant newValue, String reason, Instant at) {}

    public static AbsenceDetailBody of(Detail detail, AbsenceNames names) {
        AbsenceBody base = AbsenceBody.of(detail.absence(), names);
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
                List.of());
    }

    private static ExpectedReturnChangeBody history(ExpectedReturnChange change) {
        return new ExpectedReturnChangeBody(change.oldValue(), change.newValue(), change.reason(), change.at());
    }
}
