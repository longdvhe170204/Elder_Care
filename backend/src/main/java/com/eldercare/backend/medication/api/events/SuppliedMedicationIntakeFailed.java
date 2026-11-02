package com.eldercare.backend.medication.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lần tiếp nhận thuốc cung ứng Không đạt ({@code TiepNhanThuocKhongDat}, FR-055, module-interfaces mục 1.9): báo người
 * lập yêu cầu và Phụ trách bộ phận hiện tại của bộ phận của yêu cầu, mức Nhẹ, kèm sai lệch; 009 gộp người trùng. Không
 * tự lập hay hủy yêu cầu (Q-307).
 */
public record SuppliedMedicationIntakeFailed(
        long intakeId,
        long purchaseRequestId,
        long residentId,
        long creatorStaffId,
        long departmentId,
        String medicationName,
        String discrepancy,
        Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M07",
                "SUPPLIED_MEDICATION_INTAKE_FAILED",
                SourceGroup.MEDICATION,
                new SourceRecord("M07.SUPPLIED_MEDICATION_INTAKE", String.valueOf(intakeId)),
                "SUPPLIED_MEDICATION_INTAKE_FAILED:" + intakeId,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(
                        new RecipientGroup.Staff(creatorStaffId, "PURCHASE_REQUEST_CREATOR"),
                        new RecipientGroup.DepartmentLead(departmentId, new Substitution.Default())),
                List.of(new ContentPart(
                        InformationType.GENERAL,
                        "Lần tiếp nhận " + medicationName + " của yêu cầu mua sắm " + purchaseRequestId + " Không đạt: "
                                + discrepancy)),
                Optional.empty(),
                Optional.of("Tiếp nhận thuốc cung ứng Không đạt"),
                Optional.empty(),
                false,
                at,
                Map.of("purchaseRequestId", purchaseRequestId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
