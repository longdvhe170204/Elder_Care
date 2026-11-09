package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.api.events.AlertLevelRaised;
import com.eldercare.backend.incident.api.events.AlertRepeated;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.RepeatNotificationSetting;
import com.eldercare.backend.incident.internal.alert.domain.RepeatWithin24HoursCounter;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
    private final AlertResponsibility responsibility;

    public AlertService(
            AlertStore alertStore,
            ApplicationEventPublisher eventPublisher,
            ParameterApi parameterApi,
            BusinessTime businessTime,
            MonitoringLock monitoringLock,
            @Autowired(required = false) AlertStaffPort staffPort,
            @Autowired(required = false) ResidentApi residentApi,
            @Autowired(required = false) RecordEventApi recordEvents,
            AlertResponsibility responsibility) {
        this.alertStore = alertStore;
        this.eventPublisher = eventPublisher;
        this.parameterApi = parameterApi;
        this.businessTime = businessTime;
        this.monitoringLock = monitoringLock;
        this.staffPort = Optional.ofNullable(staffPort);
        this.residentApi = Optional.ofNullable(residentApi);
        this.recordEvents = Optional.ofNullable(recordEvents);
        this.responsibility = responsibility;
    }

    @Transactional
    public Optional<AlertRecord> requestAlert(CreateAlertCommand cmd) {
        return requestAlert(
                cmd,
                created -> AlertNotices.created(
                        created,
                        cmd.sourceStaffId(),
                        false,
                        cmd.occurredAt() != null ? cmd.occurredAt() : businessTime.now()));
    }

    /**
     * Như {@link #requestAlert(CreateAlertCommand)}; {@code createdNotices} dựng yêu cầu thông báo gắn vào
     * {@link AlertCreated} khi tạo mới (cảnh báo SOS, FR-047e). Gộp thì theo quy tắc gộp hiện có (S-19).
     */
    @Transactional
    public Optional<AlertRecord> requestAlert(
            CreateAlertCommand cmd,
            Function<AlertRecord, List<com.eldercare.backend.common.notification.NotificationRequest>> createdNotices) {
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
        } else if (cmd.requesterId() != null) {
            // Nút hỗ trợ khẩn cấp: gộp theo người bấm (open_assist_lock, FR-028, S-24)
            var openAlert = alertStore.findOpenAssist(cmd.requesterId());
            if (openAlert.isPresent()) {
                return Optional.of(mergeAlert(openAlert.get(), cmd, now));
            }
        }

        return Optional.of(createNewAlert(cmd, now, createdNotices));
    }

    /**
     * Nguồn của cảnh báo đã được xử lý (FR-032, research S-09): cảnh báo đang mở theo người và khóa loại → Đã đóng, người
     * đóng Hệ thống, kết quả "nguồn đã được xử lý". Không có cảnh báo mở thì không làm gì.
     */
    @Transactional
    public boolean closeBySystem(long residentId, String typeLock, String resolution, Instant at) {
        monitoringLock.lock(residentId);
        var open = alertStore.findOpenAlert(residentId, typeLock);
        if (open.isEmpty()) return false;
        alertStore.closeBySystem(open.get().id(), resolution, at, open.get().version());
        new AlertHistory(recordEvents.orElse(null)).add(open.get().id(), "CLOSED", Map.of("by", "SYSTEM"), at);
        return true;
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

        String sourceKey = recordSource(existing.id(), cmd, now);

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

        if (sourceKey != null && cmd.level() == IncidentLevel.MEDIUM) notifyIfRepeated(updated, sourceKey, now);

        if (levelRaised) {
            eventPublisher.publishEvent(new AlertLevelRaised(
                    updated.id(),
                    updated.level().name(),
                    updated.noOnDutyDoctor(),
                    updated.typeLock(),
                    now,
                    AlertNotices.created(updated, cmd.sourceStaffId(), true, now)));
        }

        return updated;
    }

    private AlertRecord createNewAlert(
            CreateAlertCommand cmd,
            Instant now,
            Function<AlertRecord, List<com.eldercare.backend.common.notification.NotificationRequest>> createdNotices) {
        // Tầng của người cao tuổi kể cả khi vắng mặt; người phụ trách ban đầu theo FR-031 (BR-M05-19)
        Long floorAreaId = cmd.floorAreaId();
        if (floorAreaId == null && cmd.residentId() != null) {
            floorAreaId = responsibility.floorOf(cmd.residentId(), now).orElse(null);
        }
        Instant deadline = calculateDeadline(cmd.level(), now, floorAreaId);
        Long assigneeId = responsibility
                .initialAssignee(cmd.residentId(), floorAreaId, now)
                .orElse(null);

        var newRecord = new AlertRecord(
                0L,
                cmd.residentId(),
                floorAreaId,
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
                cmd.requesterId(),
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
                createdNotices.apply(created)));

        return created;
    }

    /** Ghi dòng nguồn; trả khóa dòng vừa ghi, rỗng khi không ghi (không có nguồn hoặc đã có). */
    private String recordSource(long alertId, CreateAlertCommand cmd, Instant now) {
        if (recordEvents.isEmpty() || cmd.sourceType() == null || cmd.sourceId() == null) {
            return null;
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
            return uniqueKey;
        } catch (Exception ignored) {
            // Đã lưu hoặc trùng uniqueKey
            return null;
        }
    }

    /**
     * Báo lặp lại (FR-038a, research S-08): sau mỗi lần gộp phát sinh mức Trung bình, đếm dòng nguồn Trung bình trong
     * (t − 24 giờ, t]; đạt CFG-M05-02 thì báo người phụ trách và Bác sĩ trực, khóa theo dòng nguồn nên gửi lại ở mỗi lần
     * phát sinh tiếp theo.
     */
    private void notifyIfRepeated(AlertRecord alert, String sourceKey, Instant now) {
        var times = recordEvents
                .get()
                .list(SharedKind.of("M05.ALERT_SOURCE", Map.class), new EntityRef("M05.ALERT", alert.id()))
                .stream()
                .filter(event ->
                        IncidentLevel.MEDIUM.name().equals(event.content().get("level")))
                .map(event -> event.occurredAt())
                .toList();
        var setting = parameterApi.read(ParameterCode.CFG_M05_02).asTable(RepeatNotificationSetting.class);
        int count =
                RepeatWithin24HoursCounter.count(times, now, setting.window().toDuration());
        if (!RepeatWithin24HoursCounter.reached(count, setting.count())) return;
        eventPublisher.publishEvent(new AlertRepeated(
                alert.id(),
                count,
                AlertNotices.SOURCE_TYPE + ":" + alert.id() + ":REPEATED:" + sourceKey,
                now,
                AlertNotices.repeated(alert, count, sourceKey, now)));
    }

    private Instant calculateDeadline(IncidentLevel level, Instant now, Long floorAreaId) {
        return AcknowledgementDeadlines.of(level, now, floorAreaId, parameterApi, staffPort);
    }
}
