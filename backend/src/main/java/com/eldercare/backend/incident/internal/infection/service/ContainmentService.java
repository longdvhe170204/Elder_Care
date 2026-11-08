package com.eldercare.backend.incident.internal.infection.service;

import static com.eldercare.backend.incident.internal.infection.domain.ContainmentActions.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.IsolationCreated;
import com.eldercare.backend.incident.api.events.IsolationLifted;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentContent;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lệnh đặt, gắn sự cố và gỡ khoanh vùng theo FR-063/064/066. */
@Service
@Transactional
public class ContainmentService {
    private static final String ENTITY = "M05.CONTAINMENT";
    private final ContainmentStore store;
    private final IncidentStore incidents;
    private final LocationStructureApi locations;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    public ContainmentService(
            ContainmentStore store,
            IncidentStore incidents,
            LocationStructureApi locations,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.store = store;
        this.incidents = incidents;
        this.locations = locations;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.users = users;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.INFECTION_CONTAINMENT, operation = Operation.CREATE)
    public SimpleRecord<ContainmentContent> create(ContainmentScope scope, Set<Long> incidentIds, Reason reason) {
        requireReason(reason);
        requireIncidents(incidentIds);
        boolean validFloors = scope.floorIds().stream().allMatch(id -> locations.active(new LocationRef("FLOOR", id)));
        boolean validAreas = scope.areaIds().stream().allMatch(id -> locations.active(new LocationRef("AREA", id)));
        if (!validFloors || !validAreas) throw blocked("FR-063", "Phạm vi phải là tầng hoặc khu vực đang hoạt động");
        var at = time.now();
        var created = store.create(scope, at, users.getObject().accountId(), reason.value(), incidentIds);
        audit.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY, created.id())
                .after(Map.of("status", created.status(), "incidentIds", incidentIds))
                .reason(reason)
                .build());
        var notices = ContainmentNotices.created(
                created.id(), scope, locations, users.getObject().accountId(), at);
        events.publishEvent(new IsolationCreated(
                created.id(),
                List.copyOf(scope.floorIds()),
                List.copyOf(scope.areaIds()),
                ENTITY + ":" + created.id() + ":CREATED",
                at,
                notices));
        return created;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.INFECTION_CONTAINMENT, operation = Operation.UPDATE)
    public SimpleRecord<ContainmentContent> lift(long id, Reason reason) {
        requireReason(reason);
        var before = store.lock(id);
        requireActive(before);
        var at = time.now();
        var after = store.release(id, before.version(), at, users.getObject().accountId(), reason.value());
        audit.write(AuditLogEntry.builder()
                .action(LIFTED)
                .entity(ENTITY, id)
                .before(Map.of("status", before.status()))
                .after(Map.of("status", after.status()))
                .reason(reason)
                .build());
        var scope = before.content().period().scope();
        events.publishEvent(new IsolationLifted(
                id,
                List.copyOf(scope.floorIds()),
                List.copyOf(scope.areaIds()),
                ENTITY + ":" + id + ":LIFTED",
                at,
                List.of()));
        return after;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.INFECTION_CONTAINMENT, operation = Operation.UPDATE)
    public SimpleRecord<ContainmentContent> attach(long id, long incidentId) {
        requireIncidents(Set.of(incidentId));
        var before = store.lock(id);
        requireActive(before);
        var ids = new HashSet<>(before.content().incidentIds());
        if (!ids.add(incidentId)) return before;
        var after = store.attach(id, before.version(), ids);
        audit.write(AuditLogEntry.builder()
                .action(ATTACHED)
                .entity(ENTITY, id)
                .before(Map.of("incidentIds", before.content().incidentIds()))
                .after(Map.of("incidentIds", ids))
                .build());
        return after;
    }

    @RequiresPermission(function = Function.INFECTION_CONTAINMENT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<SimpleRecord<ContainmentContent>> list() {
        return store.list();
    }

    private void requireIncidents(Set<Long> ids) {
        if (ids == null || ids.isEmpty() || !incidents.lockOpenSuspected(ids).equals(ids))
            throw blocked("FR-063", "Phải gắn ít nhất một sự cố lây nhiễm chưa đóng");
    }

    private static void requireActive(SimpleRecord<ContainmentContent> row) {
        if (row.lockedAt() != null) throw blocked("FR-064", "Vùng khoanh đã được gỡ");
    }

    private static void requireReason(Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
