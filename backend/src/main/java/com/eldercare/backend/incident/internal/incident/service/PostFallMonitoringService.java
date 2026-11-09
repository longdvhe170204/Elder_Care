package com.eldercare.backend.incident.internal.incident.service;

import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.POST_FALL_MONITORING_ENDED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.care.api.TaskSourceApi;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Khoảng theo dõi sau ngã (FR-052, BR-M05-07; research S-14): Bác sĩ kết thúc sớm kèm lý do, 005 hủy công việc theo dõi
 * chưa đến hạn trong cùng transaction; hết thời gian thì việc theo phút chuyển Đã hết. Vắng mặt không kéo dài khoảng.
 */
@Service
public class PostFallMonitoringService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final int BATCH = 200;
    private static final Logger log = LoggerFactory.getLogger(PostFallMonitoringService.class);

    private final IncidentStore incidents;
    private final FallMonitoringStore windows;
    private final MonitoringLock monitoringLock;
    private final PermissionApi permissions;
    private final TaskSourceApi taskSources;
    private final AuditLogApi audit;
    private final TransactionTemplate transaction;

    public PostFallMonitoringService(
            IncidentStore incidents,
            FallMonitoringStore windows,
            MonitoringLock monitoringLock,
            PermissionApi permissions,
            TaskSourceApi taskSources,
            AuditLogApi audit,
            TransactionTemplate transaction) {
        this.incidents = incidents;
        this.windows = windows;
        this.monitoringLock = monitoringLock;
        this.permissions = permissions;
        this.taskSources = taskSources;
        this.audit = audit;
        this.transaction = transaction;
    }

    @Transactional
    public void endEarly(long incidentId, Reason reason, CurrentUser user) {
        if (reason == null) throw new com.eldercare.backend.common.error.ReasonRequiredException();
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        if (incident.residentId() == null) throw new NotFoundException();
        var permission = permissions.hasPermission(
                user,
                new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE),
                new Target(ENTITY, Long.toString(incidentId), incident.residentId(), incident.floorAreaId()));
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền kết thúc theo dõi sau ngã");
        }
        if (!user.roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException("Chỉ Bác sĩ kết thúc theo dõi sau ngã");
        }
        monitoringLock.lock(incident.residentId());
        var window = windows.lockActive(incident.residentId())
                .filter(w -> w.content().incidentId() == incidentId)
                .orElseThrow(() -> new ConditionNotMetException(
                        List.of(new UnmetCondition("FR-052", "Sự cố không có khoảng theo dõi sau ngã đang diễn ra"))));
        windows.finish(
                window, FallMonitoringStore.ENDED_EARLY, window.content().ended(reason.value(), user.accountId()));
        taskSources.sourceCancelled(SourceType.POST_FALL_MONITORING, incidentId, reason);
        audit.write(AuditLogEntry.builder()
                .action(POST_FALL_MONITORING_ENDED)
                .entity(ENTITY, incidentId)
                .resident(incident.residentId())
                .after(Map.of("windowId", window.id(), "status", FallMonitoringStore.ENDED_EARLY))
                .reason(reason)
                .build());
    }

    /** Việc theo phút: khoảng quá hạn sang Đã hết; trả số khoảng đã chuyển, lỗi một khoảng không chặn khoảng khác. */
    public int expireDue(Instant now) {
        int processed = 0;
        for (var window : windows.dueForExpiry(now, BATCH)) {
            try {
                Boolean done = transaction.execute(status -> {
                    var locked = windows.lockActive(window.residentId());
                    if (locked.isEmpty() || locked.get().id() != window.id()) return false;
                    windows.finish(
                            locked.get(),
                            FallMonitoringStore.EXPIRED,
                            window.content().ended(null, null));
                    return true;
                });
                if (Boolean.TRUE.equals(done)) processed++;
            } catch (RuntimeException exception) {
                log.warn("Chuyển khoảng theo dõi sau ngã {} sang Đã hết lỗi", window.id(), exception);
            }
        }
        return processed;
    }
}
