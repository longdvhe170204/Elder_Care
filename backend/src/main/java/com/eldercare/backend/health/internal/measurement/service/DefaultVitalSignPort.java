package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.care.api.VitalSignPort;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link VitalSignPort} từ module health: ghi nhận chỉ số từ công việc đo lịch (BR-M04-10, S-06).
 * Trả DA_LUU / BI_CHAN / CHO_DO_LAI tùy kết quả so ngưỡng vật lý và nghiệp vụ.
 */
@Service
class DefaultVitalSignPort implements VitalSignPort {

    private final VitalSignMeasurementService measurementService;

    DefaultVitalSignPort(VitalSignMeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public RecordResult record(
            long residentId,
            long indicatorId,
            Instant measuredAt,
            long taskId,
            Map<String, BigDecimal> values,
            CurrentUser user) {

        var command = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                measuredAt,
                user != null ? user.accountId() : 0L,
                values,
                false,
                taskId,
                null,
                false,
                null);

        try {
            var record = measurementService.record(command, user);
            if (record.confirmationStatus() == VitalSignConfirmationStatus.AWAITING_REMEASUREMENT) {
                return RecordResult.AWAITING_REMEASUREMENT;
            }
            return RecordResult.SAVED;
        } catch (BusinessException e) {
            if ("NGOAI_KHOANG_VAT_LY".equals(e.errorCode())) {
                return RecordResult.BLOCKED;
            }
            throw e;
        }
    }
}
