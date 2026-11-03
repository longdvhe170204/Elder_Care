package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.api.events.AlertLevelRaised;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dịch vụ xử lý vòng đời cảnh báo: tạo, gộp, nâng mức (FR-027, FR-028, FR-031, S-08).
 */
@Service
public class AlertService {

    private final AlertStore alertStore;
    private final ApplicationEventPublisher eventPublisher;
    private final ParameterApi parameterApi;
    private final BusinessTime businessTime;
    private final MonitoringLock monitoringLock;
    private final Optional<AlertStaffPort> staffPort;
    private final Optional<ResidentApi> residentApi;
    private final Optional<RecordEventApi> recordEvents;

    public AlertService(
            AlertStore alertStore,
            ApplicationEventPublisher eventPublisher,
            ParameterApi parameterApi,
            BusinessTime businessTime,
            MonitoringLock monitoringLock,
            @Autowired(required = false) AlertStaffPort staffPort,
            @Autowired(required = false) ResidentApi residentApi,
            @Autowired(required = false) RecordEventApi recordEvents) {
        this.alertStore = alertStore;
        this.eventPublisher = eventPublisher;
        this.parameterApi = parameterApi;
        this.businessTime = businessTime;
        this.monitoringLock = monitoringLock;
        this.staffPort = Optional.ofNullable(staffPort);
        this.residentApi = Optional.ofNullable(residentApi);
        this.recordEvents = Optional.ofNullable(recordEvents);
    }

    @Transactional
    public Optional<AlertRecord> requestAlert(CreateAlertCommand cmd) {
        if (cmd.residentId() != null) {
            // Bỏ qua khi người cao tuổi ở trạng thái cuối (research S-08, S-09, FR-029)
            if (residentApi.isPresent() && residentApi.get().isFinalStatus(cmd.residentId())) {
                return Optional.empty();
            }
            // Khóa tuần tự theo_doi_nct (S-03, T007)
            monitoringLock.lock(cmd.residentId());
        }

        Instant now = cmd.occurredAt() != null ? cmd.occurredAt() : businessTime.now();

        // Kiểm tra xem đã có cảnh báo mở với cùng residentId và typeLock chưa
        if (cmd.residentId() != null) {
            var openAlert = alertStore.findOpenAlert(cmd.residentId(), cmd.typeLock());
            if (openAlert.isPresent()) {
                return Optional.of(mergeAlert(openAlert.get(), cmd, now));
            }
        }

        return Optional.of(createNewAlert(cmd, now));
    }

    private AlertRecord mergeAlert(AlertRecord existing, CreateAlertCommand cmd, Instant now) {
        int newCount = existing.occurrenceCount() + 1;
        IncidentLevel newLevel = existing.level();
        Instant newDeadline = existing.acknowledgementDeadline();
        boolean levelRaised = false;

        if (cmd.level() != null
                && (existing.level() == null
                        || cmd.level().ordinal() > existing.level().ordinal())) {
            newLevel = cmd.level();
            levelRaised = true;
            newDeadline = calculateDeadline(newLevel, now, existing.floorAreaId());
        }

        alertStore.updateMerged(existing.id(), newCount, now, newLevel, newDeadline, existing.version());

        recordSource(existing.id(), cmd, now);

        var updated = new AlertRecord(
                existing.id(),
                existing.residentId(),
                existing.floorAreaId(),
                existing.type(),
                existing.typeLock(),
                newLevel,
                existing.levelAtReceived(),
                newCount,
                existing.firstAt(),
                now,
                existing.status(),
                existing.escalationLevel(),
                newDeadline,
                existing.maxEscalationAt(),
                existing.maxReminderCount(),
                existing.assigneeId(),
                existing.handoverAcknowledged(),
                existing.receiverId(),
                existing.receivedAt(),
                existing.resolution(),
                existing.closeReason(),
                existing.closedById(),
                existing.closedBySystem(),
                existing.closedAt(),
                existing.incidentId(),
                existing.criticalFlagId(),
                existing.noOnDutyDoctor(),
                existing.escalationStopped(),
                existing.createdById(),
                existing.description(),
                existing.requesterId(),
                existing.arrivedAt(),
                existing.arrivedById(),
                existing.skipArrivalTime(),
                existing.version() + 1);

        if (levelRaised) {
            eventPublisher.publishEvent(new AlertLevelRaised(
                    updated.id(),
                    updated.level().name(),
                    updated.noOnDutyDoctor(),
                    updated.typeLock(),
                    now,
                    List.of()));
        }

        return updated;
    }

    private AlertRecord createNewAlert(CreateAlertCommand cmd, Instant now) {
        Instant deadline = calculateDeadline(cmd.level(), now, cmd.floorAreaId());
        Long assigneeId = null;
        if (cmd.residentId() != null && staffPort.isPresent()) {
            assigneeId = staffPort.get().assignedNurse(cmd.residentId(), now).orElse(null);
        }

        var newRecord = new AlertRecord(
                0L,
                cmd.residentId(),
                cmd.floorAreaId(),
                cmd.type(),
                cmd.typeLock(),
                cmd.level(),
                null,
                1,
                now,
                now,
                AlertStatus.NEW,
                0,
                deadline,
                null,
                0,
                assigneeId,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                cmd.createdById(),
                cmd.description(),
                null,
                null,
                null,
                false,
                0);

        long id = alertStore.insert(newRecord);
        var created = alertStore.findById(id).orElseThrow();

        recordSource(created.id(), cmd, now);

        eventPublisher.publishEvent(new AlertCreated(
                created.id(),
                created.level() != null ? created.level().name() : null,
                created.noOnDutyDoctor(),
                created.typeLock(),
                now,
                List.of()));

        return created;
    }

    private void recordSource(long alertId, CreateAlertCommand cmd, Instant now) {
        if (recordEvents.isEmpty() || cmd.sourceType() == null || cmd.sourceId() == null) {
            return;
        }
        var kind = SharedKind.of("M05.ALERT_SOURCE", Map.class);
        var parent = new EntityRef("M05.ALERT", alertId);
        Map<String, Object> content = new HashMap<>();
        content.put("sourceType", cmd.sourceType());
        content.put("sourceId", cmd.sourceId());
        if (cmd.level() != null) {
            content.put("level", cmd.level().name());
        }
        if (cmd.description() != null) {
            content.put("description", cmd.description());
        }
        String uniqueKey = "ALERT_SOURCE:" + alertId + ":" + cmd.sourceType() + ":" + cmd.sourceId();
        try {
            recordEvents
                    .get()
                    .add(
                            kind,
                            parent,
                            new NewRecordEvent<>(
                                    content,
                                    null,
                                    cmd.createdById(),
                                    cmd.createdById() == null,
                                    now,
                                    now,
                                    now,
                                    cmd.floorAreaId(),
                                    uniqueKey));
        } catch (Exception ignored) {
            // Đã lưu hoặc trùng uniqueKey
        }
    }

    private Instant calculateDeadline(IncidentLevel level, Instant now, Long floorAreaId) {
        if (level == IncidentLevel.CRITICAL) {
            return null; // Khẩn cấp không có hạn tiếp nhận, phát báo động ngay (FR-031)
        }
        if (level == IncidentLevel.MEDIUM) {
            return now.plus(parameterApi
                    .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M05_01)
                    .asDuration());
        }
        if (floorAreaId == null || staffPort.isEmpty()) {
            throw new IllegalStateException("Chưa có nguồn ca để tính hạn cảnh báo Nhẹ");
        }
        var shifts = staffPort.get();
        var current = shifts.shiftAt(floorAreaId, now)
                .orElseThrow(() -> new IllegalStateException("Chưa có ca hiện tại của tầng"));
        var handoverLead = parameterApi
                .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M09_04)
                .asDuration();
        if (now.isAfter(current.endsAt().minus(handoverLead))) {
            return shifts.nextShift(floorAreaId, now)
                    .orElseThrow(() -> new IllegalStateException("Chưa có ca kế tiếp của tầng"))
                    .endsAt();
        }
        return current.endsAt();
    }
}
