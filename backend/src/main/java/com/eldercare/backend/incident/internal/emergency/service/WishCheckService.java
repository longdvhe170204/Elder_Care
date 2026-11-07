package com.eldercare.backend.incident.internal.emergency.service;

import static com.eldercare.backend.incident.internal.emergency.domain.EmergencyActions.WISH_CHECKED;

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
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh "Xác nhận đã đối chiếu nguyện vọng" (FR-048a, DBR-19, BR-M05-13): chỉ người được xem đủ thẻ; người cao tuổi phải
 * có nguyện vọng Hiệu lực; mỗi sự cố một xác nhận. Sau đó lệnh ghi biện pháp hồi sức không còn bị chặn.
 */
@Service
public class WishCheckService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final Set<String> OPEN = Set.of("NEW", "IN_PROGRESS", "MONITORING");

    private final IncidentStore incidents;
    private final IncidentProgressionStore store;
    private final EmergencyCardService cards;
    private final PermissionApi permissions;
    private final EndOfLifeWishApi wishes;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public WishCheckService(
            IncidentStore incidents,
            IncidentProgressionStore store,
            EmergencyCardService cards,
            PermissionApi permissions,
            EndOfLifeWishApi wishes,
            AuditLogApi audit,
            BusinessTime time) {
        this.incidents = incidents;
        this.store = store;
        this.cards = cards;
        this.permissions = permissions;
        this.wishes = wishes;
        this.audit = audit;
        this.time = time;
    }

    @Transactional
    public void confirm(long incidentId, CurrentUser user) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Quản lý viện không ghi xử lý sự cố");
        }
        var locked = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        var permission = permissions.hasPermission(
                user,
                new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE),
                new Target(ENTITY, Long.toString(incidentId), incident.residentId(), incident.floorAreaId()));
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xử lý sự cố");
        }
        if (!cards.fullHealthAccess(user, incident)) {
            throw new PermissionDeniedException("Chỉ người được xem đủ thẻ thông tin khẩn cấp mới xác nhận đối chiếu");
        }
        if (incident.residentId() == null || !OPEN.contains(locked.status())) {
            throw blocked("Sự cố không ở trạng thái cho phép xác nhận đối chiếu");
        }
        var wish = wishes.current(incident.residentId())
                .orElseThrow(() -> blocked("Người cao tuổi chưa có nguyện vọng cuối đời để đối chiếu"));
        if (store.wishChecked(incidentId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "WISH_ALREADY_CHECKED", "Sự cố đã có xác nhận đối chiếu nguyện vọng");
        }
        store.addWishCheck(incidentId, wish.id(), user.accountId(), time.now(), incident.floorAreaId());
        audit.write(AuditLogEntry.builder()
                .action(WISH_CHECKED)
                .entity(ENTITY, incidentId)
                .resident(incident.residentId())
                .after(Map.of("wishVersionId", wish.id()))
                .build());
    }

    private static ConditionNotMetException blocked(String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition("FR-048a", message)));
    }
}
