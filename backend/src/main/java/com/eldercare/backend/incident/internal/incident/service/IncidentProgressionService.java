package com.eldercare.backend.incident.internal.incident.service;

import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.PROGRESSION_ADDED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh "Ghi diễn biến" sự cố (bảng FR-045; FR-048a, DBR-19): người đã ghi nhận sự cố hoặc nhân viên có quyền "T"
 * trong phạm vi; Quản lý viện không ghi diễn biến (FR-041, Q-71).
 */
@Service
public class IncidentProgressionService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final Set<String> OPEN = Set.of("NEW", "IN_PROGRESS", "MONITORING");

    private final IncidentStore incidents;
    private final IncidentProgressionStore progressions;
    private final IncidentCatalogService catalog;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final EndOfLifeWishApi wishes;

    public IncidentProgressionService(
            IncidentStore incidents,
            IncidentProgressionStore progressions,
            IncidentCatalogService catalog,
            PermissionApi permissions,
            AuditLogApi audit,
            BusinessTime time,
            EndOfLifeWishApi wishes) {
        this.incidents = incidents;
        this.progressions = progressions;
        this.catalog = catalog;
        this.permissions = permissions;
        this.audit = audit;
        this.time = time;
        this.wishes = wishes;
    }

    @Transactional
    public long add(long incidentId, long progressionTypeId, String content, Instant occurredAt, CurrentUser user) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Quản lý viện không ghi diễn biến sự cố");
        }
        if (content == null || content.isBlank()) throw new InvalidValueException("Thiếu nội dung diễn biến");
        var type = type(progressionTypeId);
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        boolean recorder = incidents.recordedBy(incidentId) == user.accountId()
                || progressions.isHandler(incidentId, user.accountId());
        var permission = permissions.hasPermission(
                user,
                new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE),
                new Target(ENTITY, Long.toString(incidentId), incident.residentId(), incident.floorAreaId()));
        boolean outOfScope = false;
        if (!permission.allowed()) {
            boolean scopeOnly = permission.failedLayer().orElse(null) == FailedLayer.SCOPE;
            if (!(recorder && scopeOnly)) {
                if (scopeOnly) throw new NotFoundException();
                throw new PermissionDeniedException("Không có quyền ghi diễn biến sự cố");
            }
            outOfScope = true;
        }
        if (!OPEN.contains(incident.status())) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-045", "Sự cố đã kết thúc")));
        }
        if (type.resuscitation() && incident.residentId() != null) {
            var wish = wishes.current(incident.residentId());
            if (wish.isPresent() && !progressions.wishChecked(incidentId)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "WISH_NOT_CHECKED",
                        "Cần xác nhận đã đối chiếu nguyện vọng trước khi ghi biện pháp hồi sức",
                        Map.of("endOfLifeWish", wish.get()));
            }
        }

        var now = time.now();
        long id = progressions.add(
                incidentId,
                type.id(),
                type.code(),
                content,
                occurredAt == null ? now : occurredAt,
                user.accountId(),
                outOfScope,
                incident.floorAreaId(),
                now);
        audit.write(AuditLogEntry.builder()
                .action(PROGRESSION_ADDED)
                .entity(ENTITY, incidentId)
                .resident(incident.residentId())
                .after(Map.of("progressionId", id, "type", type.code(), "outOfScope", outOfScope))
                .build());
        return id;
    }

    /**
     * Diễn biến do Hệ thống ghi khi nguồn của cảnh báo đã chuyển sự cố được xử lý (FR-032, research S-09). Sự cố đã kết thúc
     * thì bỏ qua. Trả yêu cầu thông báo (Nhẹ) cho người xử lý hiện tại; rỗng khi bỏ qua hoặc chưa có người xử lý.
     */
    @Transactional
    public List<NotificationRequest> addBySystem(long incidentId, String typeCode, String content, Instant at) {
        var incident = incidents.lock(incidentId).orElse(null);
        if (incident == null || !OPEN.contains(incident.status())) return List.of();
        progressions.addBySystem(incidentId, typeIdByCode(typeCode), typeCode, content, at, incident.floorAreaId());
        return incidents
                .assignee(incidentId)
                .map(staffId -> IncidentNotices.sourceResolved(
                        incidentId, incident.residentId(), incident.floorAreaId(), staffId, at))
                .orElse(List.of());
    }

    /** Tra loại diễn biến theo mã cho lệnh từ module khác ({@code IncidentCommandApi.addProgression}). */
    public long typeIdByCode(String code) {
        return catalog.progressionTypes().stream()
                .filter(type -> type.code().equals(code))
                .findFirst()
                .map(ProgressionTypeView::id)
                .orElseThrow(() -> new InvalidValueException("Loại diễn biến không tồn tại"));
    }

    private ProgressionTypeView type(long id) {
        return catalog.progressionTypes().stream()
                .filter(type -> type.id() == id)
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("Loại diễn biến không tồn tại"));
    }
}
