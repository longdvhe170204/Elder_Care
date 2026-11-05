package com.eldercare.backend.belongings.internal.incident.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.BELONGING_INCIDENT_LINKED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverRow;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liên kết sự cố ghi sau với bản ghi Báo thất lạc, Ghi hư hỏng ({@code POST /belonging-handovers/{id}/link-incident};
 * 013 T021; FR-021, FR-024, research K-07): đặt {@code incident_id} đúng một lần, sự cố phải có sẵn, cùng người cao tuổi,
 * chưa Đã hủy. Quyền như lệnh bàn giao (Hành chính; Điều dưỡng trong phạm vi).
 */
@Service
public class BelongingIncidentLinks {

    private static final String RECORD_TYPE = "M12.BELONGING_HANDOVER";

    private final BelongingHandoverStore handovers;
    private final BelongingIncidents incidents;
    private final AuditLogApi audit;
    private final ObjectProvider<CurrentUser> users;

    BelongingIncidentLinks(
            BelongingHandoverStore handovers,
            BelongingIncidents incidents,
            AuditLogApi audit,
            ObjectProvider<CurrentUser> users) {
        this.handovers = handovers;
        this.incidents = incidents;
        this.audit = audit;
        this.users = users;
    }

    /** Người cao tuổi chủ bản ghi, để lớp web dựng {@link Target} kiểm quyền theo phạm vi. */
    public long residentOf(long handoverId) {
        return handovers.find(handoverId).map(HandoverRow::residentId).orElseThrow(NotFoundException::new);
    }

    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public void link(Target resident, long handoverId, long incidentId) {
        users.getObject().staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên liên kết sự cố"));
        HandoverRow row = handovers.find(handoverId).orElseThrow(NotFoundException::new);
        if (row.residentId() != resident.residentId()) {
            throw new NotFoundException();
        }
        if (row.kind() != HandoverType.REPORT_LOST && row.kind() != HandoverType.RECORD_DAMAGE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "HANDOVER_NOT_LINKABLE",
                    "Chỉ bản ghi Báo thất lạc, Ghi hư hỏng liên kết được sự cố (FR-021)");
        }
        if (row.incidentId() != null) {
            throw alreadyLinked();
        }
        incidents.validateLink(incidentId, row.residentId());
        if (!handovers.linkIncident(handoverId, incidentId)) {
            throw alreadyLinked();
        }
        audit.write(AuditLogEntry.builder()
                .action(BELONGING_INCIDENT_LINKED)
                .entity(RECORD_TYPE, handoverId)
                .resident(row.residentId())
                .after(Map.of("incidentId", incidentId))
                .build());
    }

    private static BusinessException alreadyLinked() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "INCIDENT_ALREADY_LINKED",
                "Bản ghi đã liên kết sự cố, chỉ đặt được một lần (FR-024)");
    }
}
