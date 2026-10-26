package com.cambae.backend.m04.api.spi;

import java.time.Instant;
import java.util.*;

/** Liều thuốc chỉ để hiển thị, không là căn cứ lập công việc (006 mục 1.3, 005 FR-029). */
public interface MedicationChecklistPort {
    List<DoseView> inRange(Collection<Long> residentIds, Instant from, Instant to);
    /** Giữ đủ các trường của LieuHienThiView 006; mã danh mục do nguồn 006 quyết định. */
    record DoseView(
            String doseKey,
            long residentId,
            long prescriptionId,
            Instant plannedAt,
            Instant windowFrom,
            Instant windowTo,
            String status,
            boolean sharedOnFloor,
            boolean awaitingHandoverConfirmation,
            boolean awaitingRecordingAfterReturn,
            boolean onlineRequired,
            String medicationSource,
            Optional<Long> deliveryId,
            Optional<MedicationSummary> medication) {}

    record MedicationSummary(String description) {}
}
