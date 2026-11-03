package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.RemeasureVitalSignCommand;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.health.internal.catalog.domain.HealthCatalogs;
import com.eldercare.backend.health.internal.catalog.domain.VitalSignAttributes;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignComponent;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdComparator;
import com.eldercare.backend.health.internal.threshold.service.ApplicableThresholdResolver;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Ghi nhận chỉ số, kiểm khoảng vật lý, so ngưỡng, đo lại và phát sự kiện xác nhận (FR-010 đến FR-018, S-06).
 */
@Service
public class VitalSignMeasurementService {

    private final VitalSignRecordStore records;
    private final CatalogApi catalogs;
    private final ParameterApi parameters;
    private final ApplicableThresholdResolver thresholdResolver;
    private final BusinessTime businessTime;
    private final ApplicationEventPublisher publisher;
    private final PermissionApi permissions;
    private final AuditLogApi audit;

    public VitalSignMeasurementService(
            VitalSignRecordStore records,
            CatalogApi catalogs,
            ParameterApi parameters,
            ApplicableThresholdResolver thresholdResolver,
            BusinessTime businessTime,
            ApplicationEventPublisher publisher,
            PermissionApi permissions,
            AuditLogApi audit) {
        this.records = records;
        this.catalogs = catalogs;
        this.parameters = parameters;
        this.thresholdResolver = thresholdResolver;
        this.businessTime = businessTime;
        this.publisher = publisher;
        this.permissions = permissions;
        this.audit = audit;
    }

    @Transactional
    public VitalSignRecord record(RecordVitalSignCommand command, CurrentUser user) {
        return recordInternal(command, user, false);
    }

    /** Ghi ngoại tuyến, giữ cờ khi quyền đã mất ở thời điểm đồng bộ. */
    @Transactional
    public VitalSignRecord recordOffline(
            RecordVitalSignCommand command, CurrentUser user, boolean syncedAfterPermissionLoss) {
        return recordInternal(command, user, syncedAfterPermissionLoss);
    }

    private VitalSignRecord recordInternal(
            RecordVitalSignCommand command, CurrentUser user, boolean syncedAfterPermissionLoss) {
        PermissionResult permission =
                checkPermission(user, command.residentId(), command.offline() ? command.deviceMeasuredAt() : null);
        if (command.values().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "EMPTY_VALUES", "Bản ghi chỉ số phải có ít nhất một giá trị");
        }

        Instant now = businessTime.now();
        if (command.measuredAt().isAfter(now)) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "MEASURED_TIME_IN_FUTURE",
                    "Thời điểm đo không được sau thời điểm ghi");
        }

        CatalogItem<VitalSignAttributes> indicator = catalogs.require(HealthCatalogs.VITAL_SIGN, command.indicatorId());
        validatePhysicalRanges(indicator.code(), command.values());

        var resolved = thresholdResolver.resolve(
                command.residentId(), command.indicatorId(), indicator.code(), command.measuredAt());

        Map<String, VitalSignComponent> components = new LinkedHashMap<>();
        List<VitalSignLevel> compLevels = new ArrayList<>();
        for (var entry : command.values().entrySet()) {
            String compCode = entry.getKey();
            BigDecimal val = entry.getValue();
            ThresholdBand band = resolved.bands().getOrDefault(compCode, ThresholdBand.none());
            VitalSignLevel compLevel = ThresholdComparator.classify(val, band);
            components.put(compCode, new VitalSignComponent(val, compLevel));
            compLevels.add(compLevel);
        }
        VitalSignLevel overallLevel = ThresholdComparator.mostSevere(compLevels);

        Set<VitalSignLabel> labels = new LinkedHashSet<>();
        if (command.immediateAction()) {
            labels.add(VitalSignLabel.IMMEDIATE_ACTION);
        }
        if (command.offline()) {
            labels.add(VitalSignLabel.OFFLINE_RECORDED);
        }

        VitalSignConfirmationStatus status;
        Instant confirmedAt = null;
        Instant lockedAt = null;
        Instant remeasurementDeadline = null;

        if (overallLevel == VitalSignLevel.DANGEROUS && !command.immediateAction()) {
            status = VitalSignConfirmationStatus.AWAITING_REMEASUREMENT;
            Duration remeasureDuration =
                    parameters.read(ParameterCode.CFG_M06_04).asDuration();
            remeasurementDeadline = command.measuredAt().plus(remeasureDuration);
        } else {
            status = VitalSignConfirmationStatus.CONFIRMED;
            confirmedAt = now;
            lockedAt = confirmedAt;
        }

        Instant createdAt = now.isBefore(command.measuredAt()) ? command.measuredAt() : now;
        VitalSignRecord record = records.insert(
                command.residentId(),
                command.indicatorId(),
                command.measuredAt(),
                command.measuredBy(),
                command.taskId(),
                command.deviceMeasuredAt(),
                command.offline(),
                resolved.thresholdVersionId(),
                overallLevel,
                status,
                remeasurementDeadline,
                null,
                components,
                labels,
                command.note(),
                confirmedAt,
                lockedAt,
                createdAt,
                user != null ? user.accountId() : command.measuredBy(),
                syncedAfterPermissionLoss);

        writeRecordingAudit(record, command.values(), permission, null);
        if (status == VitalSignConfirmationStatus.CONFIRMED) {
            String eventKey = "VITAL_SIGN:" + record.id();
            publisher.publishEvent(new VitalSignConfirmed(
                    record.id(),
                    record.residentId(),
                    record.indicatorId(),
                    record.level(),
                    record.thresholdVersionId(),
                    record.taskId(),
                    record.measuredAt(),
                    record.labels(),
                    eventKey,
                    now));
        }

        return record;
    }

    @Transactional
    public VitalSignRecord remeasure(RemeasureVitalSignCommand command, CurrentUser user) {
        return remeasureInternal(command, user, null, false, false);
    }

    /** Đồng bộ lần đo lại vẫn giữ thời điểm thiết bị và nhãn ngoại tuyến. */
    @Transactional
    public VitalSignRecord remeasureOffline(
            RemeasureVitalSignCommand command,
            CurrentUser user,
            Instant deviceMeasuredAt,
            boolean syncedAfterPermissionLoss) {
        return remeasureInternal(command, user, deviceMeasuredAt, true, syncedAfterPermissionLoss);
    }

    private VitalSignRecord remeasureInternal(
            RemeasureVitalSignCommand command,
            CurrentUser user,
            Instant deviceMeasuredAt,
            boolean offline,
            boolean syncedAfterPermissionLoss) {
        if (command.values().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "EMPTY_VALUES", "Bản ghi chỉ số phải có ít nhất một giá trị");
        }
        if (command.measuredAt().isAfter(businessTime.now())) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "MEASURED_TIME_IN_FUTURE",
                    "Thời điểm đo không được sau thời điểm ghi");
        }
        VitalSignRecord original = records.findById(command.originalRecordId()).orElseThrow(NotFoundException::new);

        PermissionResult permission = checkPermission(user, original.residentId(), offline ? deviceMeasuredAt : null);
        if (original.confirmationStatus() != VitalSignConfirmationStatus.AWAITING_REMEASUREMENT) {
            throw new ConditionNotMetException(List.of(
                    new UnmetCondition("RECORD_NOT_AWAITING_REMEASUREMENT", "Bản ghi không ở trạng thái chờ đo lại")));
        }

        CatalogItem<VitalSignAttributes> indicator =
                catalogs.require(HealthCatalogs.VITAL_SIGN, original.indicatorId());
        validatePhysicalRanges(indicator.code(), command.values());

        var resolved = thresholdResolver.resolve(
                original.residentId(), original.indicatorId(), indicator.code(), command.measuredAt());

        Map<String, VitalSignComponent> components = new LinkedHashMap<>();
        List<VitalSignLevel> compLevels = new ArrayList<>();
        for (var entry : command.values().entrySet()) {
            String compCode = entry.getKey();
            BigDecimal val = entry.getValue();
            ThresholdBand band = resolved.bands().getOrDefault(compCode, ThresholdBand.none());
            VitalSignLevel compLevel = ThresholdComparator.classify(val, band);
            components.put(compCode, new VitalSignComponent(val, compLevel));
            compLevels.add(compLevel);
        }
        VitalSignLevel overallLevel = ThresholdComparator.mostSevere(compLevels);

        Instant now = businessTime.now();
        records.confirmRemeasurement(original.id(), now, original.labels());

        Instant createdAt = now.isBefore(command.measuredAt()) ? command.measuredAt() : now;
        VitalSignRecord newRecord = records.insert(
                original.residentId(),
                original.indicatorId(),
                command.measuredAt(),
                command.measuredBy(),
                original.taskId(),
                deviceMeasuredAt,
                offline,
                resolved.thresholdVersionId(),
                overallLevel,
                VitalSignConfirmationStatus.CONFIRMED,
                null,
                original.id(),
                components,
                offline ? Set.of(VitalSignLabel.OFFLINE_RECORDED) : Set.of(),
                command.note(),
                now,
                now,
                createdAt,
                user != null ? user.accountId() : command.measuredBy(),
                syncedAfterPermissionLoss);

        writeRecordingAudit(newRecord, command.values(), permission, original.id());
        String eventKey = "VITAL_SIGN:" + newRecord.id();
        publisher.publishEvent(new VitalSignConfirmed(
                newRecord.id(),
                newRecord.residentId(),
                newRecord.indicatorId(),
                newRecord.level(),
                newRecord.thresholdVersionId(),
                newRecord.taskId(),
                newRecord.measuredAt(),
                newRecord.labels(),
                eventKey,
                now));

        return newRecord;
    }

    /** Hết hạn đo lại: khóa bản nháp đúng một lần và phát sự kiện để M05 cảnh báo. */
    @Transactional
    public boolean confirmOverdue(long recordId, Instant dueAt) {
        var original = records.findById(recordId).orElse(null);
        if (original == null
                || original.confirmationStatus() != VitalSignConfirmationStatus.AWAITING_REMEASUREMENT
                || original.remeasurementDeadline() == null
                || original.remeasurementDeadline().isAfter(dueAt)) return false;
        Instant confirmedAt = businessTime.now();
        Set<VitalSignLabel> labels = new LinkedHashSet<>(original.labels());
        labels.add(VitalSignLabel.NOT_REMEASURED);
        if (!records.confirmOverdue(recordId, dueAt, confirmedAt, labels)) return false;
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("VITAL_SIGN_OVERDUE_CONFIRMED", false))
                .entity("M06.VITAL_SIGN_RECORD", recordId)
                .resident(original.residentId())
                .before(Map.of(
                        "confirmationStatus", original.confirmationStatus().name()))
                .after(Map.of("confirmationStatus", "CONFIRMED", "labels", labels))
                .basis(new Basis("BR-M06-04", null, recordId, null))
                .deviceTime(original.deviceMeasuredAt())
                .build());
        publisher.publishEvent(new VitalSignConfirmed(
                original.id(),
                original.residentId(),
                original.indicatorId(),
                original.level(),
                original.thresholdVersionId(),
                original.taskId(),
                original.measuredAt(),
                labels,
                "VITAL_SIGN:" + recordId,
                confirmedAt));
        return true;
    }

    @Transactional(readOnly = true)
    public List<VitalSignRecord> getHistory(long residentId, Long indicatorId, Instant from, Instant to) {
        if (indicatorId != null && from != null && to != null) {
            return records.withinRange(residentId, indicatorId, from, to);
        }
        var confirmed = records.confirmedForResident(residentId);
        if (indicatorId != null) {
            return confirmed.stream()
                    .filter(r -> r.indicatorId() == indicatorId)
                    .toList();
        }
        return confirmed;
    }

    private PermissionResult checkPermission(CurrentUser user, long residentId, Instant deviceTime) {
        if (user == null) throw new PermissionDeniedException("Không có người ghi chỉ số");
        var key = new PermissionKey(Function.VITAL_SIGN, Operation.CREATE);
        var target = new Target("M06.VITAL_SIGN_RECORD", null, residentId, null);
        var result = deviceTime == null
                ? permissions.hasPermission(user, key, target)
                : permissions.hasPermissionAt(user, key, target, deviceTime);
        if (!result.allowed()) {
            if (result.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            String code = result.failedLayer().orElse(null) == FailedLayer.LEGAL
                    ? "LEGAL_CONDITION_NOT_MET"
                    : "PERMISSION_DENIED";
            throw new PermissionDeniedException(
                    code,
                    "Không có quyền ghi chỉ số",
                    Map.of("failedLayer", result.failedLayer().orElseThrow().name()));
        }
        return result;
    }

    private void writeRecordingAudit(
            VitalSignRecord record, Map<String, BigDecimal> values, PermissionResult permission, Long originalId) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("indicatorId", record.indicatorId());
        after.put("values", values);
        after.put("level", record.level().name());
        after.put("confirmationStatus", record.confirmationStatus().name());
        after.put("thresholdVersionId", record.thresholdVersionId());
        if (originalId != null) after.put("remeasurementFor", originalId);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(originalId == null ? "VITAL_SIGN_RECORDED" : "VITAL_SIGN_REMEASURED", false))
                .entity("M06.VITAL_SIGN_RECORD", record.id())
                .resident(record.residentId())
                .after(after)
                .details(Map.of(
                        "permissionBasis",
                        permission.basis().orElseThrow().kind().name()))
                .deviceTime(record.deviceMeasuredAt())
                .build());
    }

    private void validatePhysicalRanges(String indicatorCode, Map<String, BigDecimal> values) {
        var rangeParam = parameters.read(ParameterCode.CFG_M06_03);
        JsonNode rangesNode = rangeParam.isSet() ? rangeParam.raw() : null;

        for (var entry : values.entrySet()) {
            String compCode = entry.getKey();
            BigDecimal val = entry.getValue();

            if (rangesNode != null && rangesNode.has(indicatorCode)) {
                JsonNode indicatorRanges = rangesNode.get(indicatorCode);
                if (indicatorRanges.has(compCode)) {
                    JsonNode compRange = indicatorRanges.get(compCode);
                    if (compRange.hasNonNull("min")) {
                        BigDecimal min = new BigDecimal(compRange.get("min").asText());
                        if (val.compareTo(min) < 0) {
                            throw new BusinessException(
                                    HttpStatus.UNPROCESSABLE_ENTITY,
                                    "OUT_OF_PHYSICAL_RANGE",
                                    "Giá trị nhỏ hơn cận vật lý tối thiểu: " + min);
                        }
                    }
                    if (compRange.hasNonNull("max")) {
                        BigDecimal max = new BigDecimal(compRange.get("max").asText());
                        if (val.compareTo(max) > 0) {
                            throw new BusinessException(
                                    HttpStatus.UNPROCESSABLE_ENTITY,
                                    "OUT_OF_PHYSICAL_RANGE",
                                    "Giá trị vượt quá cận vật lý tối đa: " + max);
                        }
                    }
                }
            }
        }
    }
}
