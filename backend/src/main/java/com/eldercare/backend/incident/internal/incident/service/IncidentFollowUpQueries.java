package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.care.api.SourceTaskReadApi;
import com.eldercare.backend.care.api.dto.SourceTaskView;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Việc tiếp theo của một sự cố (FR-045b): công việc theo dõi sau ngã và công việc phát sinh có căn cứ sự cố, kèm kết quả, để
 * hồ sơ sự cố hiện đủ. Quyền xem như chi tiết sự cố; ngoài phạm vi trả 404.
 */
@Service
public class IncidentFollowUpQueries {
    public static final Set<SourceType> TYPES = Set.of(SourceType.INCIDENT_FOLLOW_UP, SourceType.POST_FALL_MONITORING);
    private static final String ENTITY = "M05.INCIDENT";
    private static final PermissionKey VIEW = new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.VIEW);

    private final IncidentStore incidents;
    private final PermissionApi permissions;
    private final Optional<SourceTaskReadApi> tasks;

    public IncidentFollowUpQueries(
            IncidentStore incidents, PermissionApi permissions, @Autowired(required = false) SourceTaskReadApi tasks) {
        this.incidents = incidents;
        this.permissions = permissions;
        this.tasks = Optional.ofNullable(tasks);
    }

    @Transactional(readOnly = true)
    public List<SourceTaskView> ofIncident(long incidentId, CurrentUser user) {
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        var result = permissions.hasPermission(
                user,
                VIEW,
                new Target(ENTITY, Long.toString(incidentId), incident.residentId(), incident.floorAreaId()));
        if (!result.allowed()) {
            if (result.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem sự cố");
        }
        return tasks.map(api -> api.bySource(TYPES, incidentId)).orElse(List.of());
    }
}
