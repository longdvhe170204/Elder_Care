package com.eldercare.backend.roombedallocation.internal.asset.service;

import static com.eldercare.backend.roombedallocation.api.dto.AssetStatus.*;
import static com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetDetails;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetRow;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc chi tiết tài sản và lệnh người xem làm được (019 FR-012; api-019 {@code AssetDetail}, {@code allowedCommands}, chủ
 * dự án duyệt 2026-11-10). {@code allowedCommands} chỉ để hiện nút (constitution XI): theo đúng điều kiện trạng thái, quyền
 * và người thực hiện của lệnh trong {@link AssetService}, lệnh vẫn kiểm lại khi gọi.
 */
@Service
@Transactional(readOnly = true)
public class AssetQueries {

    private final AssetStore store;
    private final AssetService assets;
    private final StatusHistoryApi history;
    private final RecordEventApi records;
    private final CorrectionApi corrections;
    private final PermissionApi permissions;
    private final ScopeApi scope;
    private final ObjectProvider<DutyApi> duties;
    private final CurrentUser user;

    AssetQueries(
            AssetStore store,
            AssetService assets,
            StatusHistoryApi history,
            RecordEventApi records,
            CorrectionApi corrections,
            PermissionApi permissions,
            ScopeApi scope,
            ObjectProvider<DutyApi> duties,
            CurrentUser user) {
        this.store = store;
        this.assets = assets;
        this.history = history;
        this.records = records;
        this.corrections = corrections;
        this.permissions = permissions;
        this.scope = scope;
        this.duties = duties;
        this.user = user;
    }

    /** FR-012: chi tiết và lịch sử; người xem như danh sách (bộ phận mình, tầng mình, Quản lý viện, Hành chính). */
    public AssetDetails detail(long id) {
        AssetRow row = store.find(id).orElseThrow(NotFoundException::new);
        boolean visible = assets.list(null, null, null).stream().anyMatch(r -> r.id() == id);
        if (!visible) throw new PermissionDeniedException("Không có quyền xem tài sản này");
        var ref = new EntityRef(ENTITY, id);
        var maintenances = records.list(MAINTENANCE, ref);
        var damages = records.list(DAMAGE, ref);
        Map<Long, CurrentValue> current = corrections.current(
                AssetHistoryCorrectionProvider.RECORD_TYPE,
                Stream.concat(maintenances.stream(), damages.stream())
                        .filter(e -> e.confirmedAt() != null)
                        .map(RecordEvent::id)
                        .toList());
        return new AssetDetails(
                row,
                assets.locationLabel(row),
                allowedCommands(row),
                history.list(LOCATION, id).stream()
                        .map(h -> new AssetDetails.LocationPeriod(
                                h.details() == null ? null : h.details().type(),
                                h.details() == null ? null : h.details().id(),
                                h.startsAt(),
                                h.endsAt(),
                                h.command(),
                                h.actorId(),
                                h.reason()))
                        .toList(),
                history.list(DEPARTMENT, id).stream()
                        .map(h -> new AssetDetails.DepartmentTransfer(
                                h.valueBefore() == null ? null : Long.valueOf(h.valueBefore()),
                                h.valueId(),
                                h.startsAt(),
                                h.actorId(),
                                h.reason()))
                        .toList(),
                maintenances.stream()
                        .map(e -> maintenance(e, current.get(e.id())))
                        .toList(),
                damages.stream().map(e -> damage(e, current.get(e.id()))).toList());
    }

    /** Lệnh người xem làm được trên tài sản; {@code update} là sửa mô tả (PUT). */
    public List<String> allowedCommands(AssetRow row) {
        if (row.status() == DISPOSED) return List.of();
        Long me = user.staffId().orElse(null);
        var target = new Target(ENTITY, Long.toString(row.id()), null, row.floorAreaId());
        var granted = permissions.allowedCommands(
                user,
                target,
                Map.of(
                        "UPDATE", new PermissionKey(Function.ASSET_VEHICLE, Operation.UPDATE),
                        "CREATE", new PermissionKey(Function.ASSET_VEHICLE, Operation.CREATE),
                        "APPROVE", new PermissionKey(Function.ASSET_VEHICLE, Operation.APPROVE)));
        boolean update = granted.contains("UPDATE");
        boolean lead = me != null
                && duties.getObject()
                        .departmentLead(row.departmentId())
                        .filter(l -> l.longValue() == me)
                        .isPresent();
        boolean floorManager =
                row.floorAreaId() != null && user.roles().contains(Role.FLOOR_MANAGER) && scope.inScope(user, target);
        boolean manager = user.roles().contains(Role.NURSING_HOME_MANAGER);
        boolean bed = row.bedId() != null;
        AssetStatus status = row.status();
        var commands = new ArrayList<String>();
        if (update && lead) commands.add("update");
        if (update && lead && !bed && status == READY) commands.add("put-into-use");
        if (update && lead && !bed) commands.add("relocate");
        if (update && (lead || floorManager) && Set.of(READY, IN_USE).contains(status)) commands.add("report-damage");
        if (update
                && (lead || floorManager || (manager && row.awaitingResidentTransfer()))
                && Set.of(READY, IN_USE, DAMAGED).contains(status)) commands.add("start-maintenance");
        if (update && lead && status == UNDER_MAINTENANCE) commands.add("record-maintenance-result");
        if (update && bed && row.awaitingResidentTransfer() && (floorManager || manager))
            commands.add("remove-transfer-flag");
        if (granted.contains("CREATE") && lead) commands.add("request-disposal");
        if (granted.contains("APPROVE") && manager) commands.add("transfer-department");
        if (update && bed && status == READY && (lead || floorManager)) commands.add("link-bed-preparation-damage");
        return List.copyOf(commands);
    }

    private static AssetDetails.MaintenanceEntry maintenance(
            RecordEvent<AssetKinds.Maintenance> e, CurrentValue current) {
        var c = e.content();
        return new AssetDetails.MaintenanceEntry(
                e.id(),
                c.kind(),
                c.result(),
                c.date(),
                c.content(),
                c.performedBy(),
                c.outsourced(),
                c.cost(),
                c.procurementRequestId(),
                e.occurredAt(),
                e.confirmedAt(),
                current != null && current.corrected(),
                current != null && current.voided());
    }

    private static AssetDetails.DamageEntry damage(RecordEvent<AssetKinds.Damage> e, CurrentValue current) {
        var c = e.content();
        return new AssetDetails.DamageEntry(
                e.id(),
                c.description(),
                c.source(),
                c.impactLevel(),
                c.awaitingTransfer(),
                c.removalReason(),
                c.repairDescription(),
                c.repairedBy(),
                e.occurredAt(),
                e.confirmedAt(),
                current != null && current.corrected(),
                current != null && current.voided());
    }
}
