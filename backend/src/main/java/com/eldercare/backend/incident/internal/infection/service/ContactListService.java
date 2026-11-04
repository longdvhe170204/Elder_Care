package com.eldercare.backend.incident.internal.infection.service;

import static com.eldercare.backend.incident.internal.infection.domain.ContactListActions.CREATED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.domain.ContactListContent;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Người có quyền lập danh sách Nháp, không tự thêm người từ kết quả truy vết. */
@Service
@Transactional
public class ContactListService {
    private final IncidentStore incidents;
    private final ContactListStore lists;
    private final ParameterApi parameters;
    private final PermissionApi permissions;
    private final CurrentUser user;
    private final AuditLogApi audit;

    public ContactListService(
            IncidentStore incidents,
            ContactListStore lists,
            ParameterApi parameters,
            PermissionApi permissions,
            CurrentUser user,
            AuditLogApi audit) {
        this.incidents = incidents;
        this.lists = lists;
        this.parameters = parameters;
        this.permissions = permissions;
        this.user = user;
        this.audit = audit;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.CREATE)
    public SimpleRecord<ContactListContent> createDraft(long incidentId) {
        var incident = incidents.lockSuspected(incidentId).orElseThrow(NotFoundException::new);
        var target = new Target("M05.INCIDENT", Long.toString(incidentId), incident.residentId(), null);
        var result =
                permissions.hasPermission(user, new PermissionKey(Function.CONTACT_LIST, Operation.CREATE), target);
        if (!result.allowed()) {
            if (result.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền lập danh sách tiếp xúc");
        }
        if (!List.of("NEW", "IN_PROGRESS", "MONITORING").contains(incident.status())) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-061", "Sự cố lây nhiễm đã đóng")));
        }
        if (lists.findByIncident(incidentId).isPresent()) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-061", "Sự cố đã có danh sách tiếp xúc")));
        }
        var duration = parameters.read(ParameterCode.CFG_M05_07).asDuration();
        var row = lists.create(
                incidentId, incident.discoveredAt().minus(duration), incident.discoveredAt(), user.accountId());
        audit.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity("M05.CONTACT_LIST", row.id())
                .resident(incident.residentId())
                .after(Map.of("status", row.status(), "incidentId", incidentId))
                .build());
        return row;
    }
}
