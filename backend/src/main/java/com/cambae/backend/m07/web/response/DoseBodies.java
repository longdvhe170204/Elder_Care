package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.service.CalculatedDoseSlot;
import com.cambae.backend.m07.internal.service.DoseReviewService.ReviewView;
import com.cambae.backend.m07.internal.service.DoseViewService.DoseView;
import com.cambae.backend.m07.internal.service.DoseViewService.ResultView;
import com.cambae.backend.m07.internal.service.DoseViewService.UsableBatch;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Phản hồi liều theo api-006 ({@code Dose}, {@code DoseResult}; FR-018 → FR-033). */
public final class DoseBodies {

    private DoseBodies() {}

    public record PersonBody(
            @Schema(allowableValues = {"STAFF", "FAMILY_MEMBER", "FREE"})
            String type,

            Long id,
            String fullName,
            String relationship) {}

    public record ResultBody(
            Instant administeredAt,

            @Schema(allowableValues = {"REGULAR", "LEAVE_MEDICATION", "REPORTED_BY_OTHERS"})
            String basis,

            PersonBody reportedBy,
            String adverseReaction,
            String reason,
            String note,
            Boolean recordedAfterDisconnect) {

        static ResultBody of(ResultView r) {
            if (r == null) {
                return null;
            }
            PersonBody reporter = r.reportedByType() == null
                    ? null
                    : new PersonBody(r.reportedByType(), r.reportedById(), r.reportedByName(), r.relationship());
            return new ResultBody(
                    r.administeredAt(),
                    r.basis(),
                    reporter,
                    r.adverseReaction(),
                    r.reason(),
                    r.note(),
                    r.recordedAfterDisconnect());
        }
    }

    public record UsableBatchBody(Long batchId, LocalDate expiryDate) {}

    /** api-006 {@code Dose}. */
    public record DoseBody(
            String doseKey,
            Long residentId,
            Long prescriptionId,
            Instant scheduledTime,
            Instant windowFrom,
            Instant windowTo,

            @Schema(
                    allowableValues = {
                        "UPCOMING",
                        "DUE",
                        "LATE",
                        "MISSED",
                        "ADMINISTERED",
                        "REFUSED",
                        "NOT_ADMINISTERED",
                        "SUSPENDED",
                        "LEAVE_MEDICATION",
                        "CANCELLED"
                    })
            String status,

            @Schema(
                    description = "Sau đính chính",
                    allowableValues = {
                        "UPCOMING",
                        "DUE",
                        "LATE",
                        "MISSED",
                        "ADMINISTERED",
                        "REFUSED",
                        "NOT_ADMINISTERED",
                        "SUSPENDED",
                        "LEAVE_MEDICATION",
                        "CANCELLED"
                    })
            String currentStatus,

            Boolean materialized,
            Boolean floorShared,
            Boolean pendingHandoverConfirmation,
            Boolean awaitingRecordAfterReturn,
            Boolean requiresOnline,

            @Schema(allowableValues = {"FACILITY", "FAMILY_SENT"})
            String medicationSource,

            List<UsableBatchBody> usableBatches,
            Long handoverId,
            ResultBody result,
            String closedBy,
            Boolean needsReview,
            List<String> hiddenFields) {

        public static DoseBody of(DoseView v) {
            var d = v.dose();
            return new DoseBody(
                    d.doseKey(),
                    d.residentId(),
                    d.prescriptionId(),
                    d.scheduledTime(),
                    d.windowFrom(),
                    d.windowTo(),
                    d.status() == null ? null : d.status().name(),
                    v.currentStatus(),
                    v.materialized(),
                    d.floorShared(),
                    v.pendingHandoverConfirmation(),
                    v.awaitingRecordAfterReturn(),
                    v.requiresOnline(),
                    d.medicationSource() == null ? null : d.medicationSource().name(),
                    v.usableBatches().stream().map(DoseBodies::batch).toList(),
                    v.handoverId(),
                    ResultBody.of(v.result()),
                    v.closedBy(),
                    v.needsReview(),
                    List.of());
        }

        /** Liều trong lịch tính từ đơn (research D-06): chưa vật hóa thì chưa có kết quả, lần giao. */
        public static DoseBody of(CalculatedDoseSlot s) {
            return new DoseBody(
                    s.doseKey(),
                    s.residentId(),
                    s.prescriptionId(),
                    s.scheduledTime(),
                    s.windowFrom(),
                    s.windowTo(),
                    s.status() == null ? null : s.status().name(),
                    s.status() == null ? null : s.status().name(),
                    s.materialized(),
                    null,
                    null,
                    null,
                    s.specialControl(),
                    s.medicationSource() == null ? null : s.medicationSource().name(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of());
        }
    }

    private static UsableBatchBody batch(UsableBatch b) {
        return new UsableBatchBody(b.batchId(), b.expiryDate());
    }

    /** Bản ghi chờ xem lại (FR-029). */
    public record DoseReviewBody(
            Long id,

            @Schema(description = "Khi bản ghi là liều") String doseKey,

            Long doseResultId,
            Long prnAdministrationId,
            Long residentId,
            String medicationName,
            Instant administeredAt,
            Long floorAreaId,

            @Schema(allowableValues = {"PENDING_REVIEW", "ACCEPTED", "REQUIRES_CORRECTION"})
            String status,

            @Schema(description = "Họ tên người ghi; người xem lại phải khác người này")
            String recordedBy,

            @Schema(description = "Họ tên người xem lại") String reviewedBy,

            Instant reviewedAt,
            String note) {

        public static DoseReviewBody of(ReviewView v) {
            var r = v.review();
            return new DoseReviewBody(
                    r.getId(),
                    v.doseKey(),
                    r.getDoseResultId(),
                    r.getPrnUsageId(),
                    v.residentId(),
                    v.medicationName(),
                    v.administeredAt(),
                    r.getFloorAreaId(),
                    r.getStatus() == null ? null : r.getStatus().name(),
                    v.recordedBy(),
                    v.reviewedBy(),
                    r.getReviewedAt(),
                    r.getNote());
        }
    }
}
