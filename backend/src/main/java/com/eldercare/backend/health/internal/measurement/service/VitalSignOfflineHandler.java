package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.common.offline.SyncResult;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.RemeasureVitalSignCommand;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignOfflineContent;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** FR-010: ghi chỉ số, gồm lần đo lại, qua khung đồng bộ ngoại tuyến của 002. */
@Component
public class VitalSignOfflineHandler implements OfflineHandler<VitalSignOfflineContent> {
    private final VitalSignMeasurementService measurements;
    private final VitalSignRecordStore records;
    private final CurrentUser user;

    public VitalSignOfflineHandler(
            VitalSignMeasurementService measurements, VitalSignRecordStore records, CurrentUser user) {
        this.measurements = measurements;
        this.records = records;
        this.user = user;
    }

    @Override
    public String recordType() {
        return "M06.BAN_GHI_CHI_SO";
    }

    @Override
    public PermissionKey permissionKey() {
        return new PermissionKey(Function.VITAL_SIGN, Operation.CREATE);
    }

    @Override
    public Target target(VitalSignOfflineContent content) {
        return new Target("M06.VITAL_SIGN_RECORD", null, content.residentId(), null);
    }

    @Override
    public void validate(VitalSignOfflineContent content) {
        if (content.residentId() <= 0
                || content.indicatorId() <= 0
                || content.values().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VITAL_SIGN", "Nội dung chỉ số không hợp lệ");
        }
        if (content.originalRecordId() != null) {
            var original = records.findById(content.originalRecordId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_REMEASUREMENT", "Không có bản ghi gốc"));
            if (original.residentId() != content.residentId() || original.indicatorId() != content.indicatorId()) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INVALID_REMEASUREMENT",
                        "Bản ghi gốc không khớp người cao tuổi hoặc chỉ số");
            }
        }
    }

    @Override
    public String apply(VitalSignOfflineContent content, SyncOutcome outcome, Instant effectiveAt) {
        boolean permissionLost = outcome.result() == SyncResult.ACCEPTED_WITH_FLAG;
        long recorder = user.accountId();
        long id;
        if (content.originalRecordId() == null) {
            id = measurements
                    .recordOffline(
                            new RecordVitalSignCommand(
                                    content.residentId(),
                                    content.indicatorId(),
                                    effectiveAt,
                                    recorder,
                                    content.values(),
                                    content.immediateAction(),
                                    content.taskId(),
                                    effectiveAt,
                                    true,
                                    content.note()),
                            user,
                            permissionLost)
                    .id();
        } else {
            id = measurements
                    .remeasureOffline(
                            new RemeasureVitalSignCommand(
                                    content.originalRecordId(),
                                    effectiveAt,
                                    recorder,
                                    content.values(),
                                    content.note()),
                            user,
                            effectiveAt,
                            permissionLost)
                    .id();
        }
        return "vital_sign:" + id;
    }
}
