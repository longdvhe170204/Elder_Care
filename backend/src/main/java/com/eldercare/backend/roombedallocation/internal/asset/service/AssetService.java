package com.eldercare.backend.roombedallocation.internal.asset.service;

import static com.eldercare.backend.roombedallocation.api.dto.AssetStatus.*;
import static com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.CreateRequest;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementSourceType;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import com.eldercare.backend.roombedallocation.api.events.AssetStatusChanged;
import com.eldercare.backend.roombedallocation.api.spi.ProcurementRequestPort;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetCommandInput;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetInput;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetRow;
import com.eldercare.backend.roombedallocation.internal.asset.domain.BedAssetRules;
import com.eldercare.backend.roombedallocation.internal.asset.domain.BedDamageNotice;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommand;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommandPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 019 T018 lượt 1 (FR-008c, FR-011 → FR-013, FR-021; BR-M03-18, DBR-36; Q-284, Q-292, Q-294, Q-313; research W-06): vòng
 * đời tài sản theo bảng FR-013. Phụ trách bộ phận quản lý tài sản làm lệnh của bộ phận (chú thích ³⁷ chỉ mô tả ở lớp
 * vai trò, kiểm ở đây); Trưởng tầng báo hỏng, đưa vào bảo trì tài sản có vị trí thuộc tầng mình (³³, Q-313); Quản lý
 * viện chuyển bộ phận. Tài sản giường (bedId) đi qua cổng của 003 ở Phase 8; lượt này chặn lệnh trên tài sản giường.
 * Thanh lý, job hạn, đính chính ở lượt 2.
 */
@Service
public class AssetService {
    private final AssetStore store;
    private final CatalogApi catalog;
    private final StatusHistoryApi history;
    private final RecordEventApi records;
    private final LocationStructureApi structure;
    private final ObjectProvider<ProcurementRequestPort> procurement;
    private final ObjectProvider<DutyApi> duties;
    private final PermissionApi permissions;
    private final ScopeApi scope;
    private final CurrentUser user;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final JsonMapper json;
    private final BedAssetCommandPort beds;

    public AssetService(
            AssetStore store,
            CatalogApi catalog,
            StatusHistoryApi history,
            RecordEventApi records,
            LocationStructureApi structure,
            ObjectProvider<ProcurementRequestPort> procurement,
            ObjectProvider<DutyApi> duties,
            PermissionApi permissions,
            ScopeApi scope,
            CurrentUser user,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ObjectProvider<ApprovalRequestApi> approvals,
            JsonMapper json,
            BedAssetCommandPort beds) {
        this.store = store;
        this.catalog = catalog;
        this.history = history;
        this.records = records;
        this.structure = structure;
        this.procurement = procurement;
        this.duties = duties;
        this.permissions = permissions;
        this.scope = scope;
        this.user = user;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.approvals = approvals;
        this.json = json;
        this.beds = beds;
    }

    /** Đối tượng kiểm quyền: tầng của vị trí hiện hành (Trưởng tầng qua lớp phạm vi). */
    public Target target(long id) {
        var floor = store.find(id).map(AssetRow::floorAreaId).orElse(null);
        return new Target(ENTITY, Long.toString(id), null, floor);
    }

    // ---- Ghi tăng, mô tả --------------------------------------------------------------------------------------------

    /** FR-011, FR-008c, Q-294: không phải loại giường; mua mới gắn yêu cầu Đã duyệt cùng bộ phận, có nguyên giá. */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long create(Target target, AssetInput in) {
        var input = validated(in, true);
        requireDepartmentLead(input.departmentId());
        if (store.codeTaken(input.code())) throw conflict("DUPLICATE_CODE", "Mã tài sản đã có");
        ProcurementRequestPort port = null;
        if ("PURCHASED".equals(input.source())) {
            if (input.acquisitionCost() == null)
                throw conflict("ACQUISITION_COST_REQUIRED", "Tài sản gắn yêu cầu mua sắm cần nguyên giá (Q-294)");
            if (input.procurementRequestId() == null)
                throw conflict(
                        "PROCUREMENT_REQUEST_REQUIRED", "Tài sản mua mới cần yêu cầu mua sắm Đã duyệt (FR-008c)");
            port = procurementPort();
            var unmet = port.checkUsable(input.procurementRequestId(), input.departmentId(), ProcurementType.ASSET);
            if (!unmet.isEmpty())
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "PROCUREMENT_REQUEST_NOT_USABLE",
                        unmet.getFirst().description(),
                        Map.of("unmetConditions", unmet));
        }
        var now = time.now();
        long id = store.insert(input, staff(), now);
        history.add(LOCATION, id, location(null, "ASSET_STORAGE", null, null, "CREATE", null, now));
        if (port != null)
            port.linkSource(input.procurementRequestId(), ProcurementSourceType.ASSET, id, input.acquisitionCost());
        record(id, "CREATE", null, READY, null);
        publish(id, input.departmentId(), null, READY, "create", List.of());
        return id;
    }

    /** FR-011: sửa thông tin mô tả; không đổi bộ phận, nguồn, yêu cầu mua sắm (lệnh riêng, DBR-36). */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void update(Target target, long id, String ifMatch, AssetInput in) {
        var row = locked(id);
        requireNotDisposed(row);
        VersionEtag.requireMatch(ifMatch, row.version(), Map.of("id", id, "version", row.version()));
        requireDepartmentLead(row.departmentId());
        var input = validated(in, false);
        if (!Objects.equals(input.departmentId(), row.departmentId())
                || !Objects.equals(input.source(), row.source())
                || !Objects.equals(input.procurementRequestId(), row.procurementRequestId()))
            throw new InvalidValueException("Không đổi bộ phận, nguồn, yêu cầu mua sắm bằng lệnh sửa");
        if (row.procurementRequestId() != null && !Objects.equals(input.acquisitionCost(), row.acquisitionCost()))
            throw new InvalidValueException("Nguyên giá đã gắn yêu cầu mua sắm không đổi (BR-M11-19)");
        if (!input.code().equals(row.code()) && store.codeTaken(input.code()))
            throw conflict("DUPLICATE_CODE", "Mã tài sản đã có");
        if (row.bedId() != null
                && typeCode(input.typeId()).filter(BED_CODE::equals).isEmpty())
            throw new InvalidValueException("Tài sản giường giữ loại Giường");
        store.updateDescription(id, input);
        record(id, "UPDATE", row.status(), row.status(), null);
    }

    // ---- Lệnh trạng thái
    // ---------------------------------------------------------------------------------------------

    /** Sẵn sàng → Đang sử dụng tại vị trí; không áp cho giường (Q-242). */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void putIntoUse(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        if (row.bedId() != null) throw conflict("STATE_CHANGED", "Tài sản giường không dùng Đang sử dụng (Q-242)");
        requireStatus(row, READY);
        requireDepartmentLead(row.departmentId());
        move(row, IN_USE, in, "put-into-use", null);
    }

    /** FR-012: chuyển vị trí kèm lý do; lịch sử không chồng khoảng. Giường ở Phase 8 (Q-243). */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void relocate(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        requireNotBed(row);
        requireNotDisposed(row);
        var reason = new Reason(in.reason());
        requireDepartmentLead(row.departmentId());
        move(row, row.status(), in, "relocate", reason);
    }

    /** BR-M03-18: Sẵn sàng, Đang sử dụng → Hỏng; báo Phụ trách bộ phận quản lý tài sản. */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @org.springframework.resilience.annotation.Retryable(
            includes = org.springframework.dao.PessimisticLockingFailureException.class,
            maxRetries = 2,
            delay = 50)
    @Transactional
    public void reportDamage(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        if (in.description() == null || in.description().isBlank())
            throw new InvalidValueException("Báo hỏng cần mô tả");
        requireStatus(row, READY, IN_USE);
        if (row.bedId() != null) {
            reportBedDamage(row, in, "DAMAGE_REPORT");
            return;
        }
        requireLeadOrFloorManager(row);
        var now = time.now();
        records.add(
                DAMAGE,
                new EntityRef(ENTITY, id),
                new NewRecordEvent<>(
                        new AssetKinds.Damage(in.description().strip(), "DAMAGE_REPORT", null, false),
                        null,
                        staff(),
                        false,
                        now,
                        null,
                        now,
                        row.floorAreaId(),
                        null));
        store.updateStatus(id, DAMAGED);
        record(id, "REPORT_DAMAGE", row.status(), DAMAGED, null);
        publish(
                id,
                row.departmentId(),
                row.status(),
                DAMAGED,
                "report-damage",
                notice(
                        "ASSET_DAMAGED",
                        id,
                        "Tài sản " + row.code() + " bị báo hỏng",
                        new RecipientGroup.DepartmentLead(row.departmentId(), new Substitution.Default())));
    }

    /** Sẵn sàng, Đang sử dụng, Hỏng → Đang bảo trì; mở lần bảo trì (nháp tới khi ghi kết quả). */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void startMaintenance(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        requireStatus(row, READY, IN_USE, DAMAGED);
        requireLeadOrFloorManager(row, row.awaitingResidentTransfer());
        String kind = in.maintenanceKind() == null ? "MAINTENANCE" : in.maintenanceKind();
        if (!Set.of("MAINTENANCE", "INSPECTION", "VEHICLE_INSPECTION", "INSURANCE")
                .contains(kind))
            throw new InvalidValueException("Loại lần là MAINTENANCE, INSPECTION, VEHICLE_INSPECTION hoặc INSURANCE");
        if (Set.of("VEHICLE_INSPECTION", "INSURANCE").contains(kind)
                && typeCode(row.typeId()).filter(VEHICLE_CODE::equals).isEmpty())
            throw new InvalidValueException("Đăng kiểm, bảo hiểm chỉ cho tài sản loại Xe");
        bedTransition(row, UNDER_MAINTENANCE, false, "Đưa vào bảo trì");
        var now = time.now();
        records.add(
                MAINTENANCE,
                new EntityRef(ENTITY, id),
                new NewRecordEvent<>(
                        new AssetKinds.Maintenance(kind, null, null, null, null, null, null, null),
                        null,
                        staff(),
                        false,
                        now,
                        null,
                        null,
                        row.floorAreaId(),
                        null));
        store.updateStatus(id, UNDER_MAINTENANCE);
        record(id, "START_MAINTENANCE", row.status(), UNDER_MAINTENANCE, null);
        publish(id, row.departmentId(), row.status(), UNDER_MAINTENANCE, "start-maintenance", List.of());
    }

    /**
     * Đang bảo trì → Sẵn sàng (Đạt) / Hỏng (Không đạt), theo FR-013 (chủ dự án chọn 2026-11-06). Cần ngày, nội dung, đơn
     * vị thực hiện; thuê ngoài có chi phí gắn yêu cầu mua sắm dịch vụ Đã duyệt cùng bộ phận (FR-008c). Đạt thì hạn bảo
     * trì kế tiếp = ngày + chu kỳ, gỡ dấu quá hạn bảo trì (FR-014).
     */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void recordMaintenanceResult(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        requireStatus(row, UNDER_MAINTENANCE);
        requireDepartmentLead(row.departmentId());
        if (!"PASS".equals(in.result()) && !"FAIL".equals(in.result()))
            throw new InvalidValueException("Kết quả bảo trì là PASS hoặc FAIL");
        if (in.date() == null || blank(in.content()) || blank(in.performedBy()))
            throw new InvalidValueException("Kết quả bảo trì cần ngày, nội dung, đơn vị thực hiện");
        boolean outsourcedCost = Boolean.TRUE.equals(in.outsourced()) && in.cost() != null && in.cost() > 0;
        if (in.cost() != null && in.cost() < 0) throw new InvalidValueException("Chi phí không âm");
        ProcurementRequestPort port = null;
        if (outsourcedCost) {
            if (in.procurementRequestId() == null)
                throw conflict(
                        "PROCUREMENT_REQUEST_REQUIRED",
                        "Bảo trì thuê ngoài có chi phí cần yêu cầu mua sắm Đã duyệt của bộ phận (FR-008c)");
            port = procurementPort();
            var unmet = port.checkUsable(in.procurementRequestId(), row.departmentId(), ProcurementType.SERVICE);
            if (!unmet.isEmpty())
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "PROCUREMENT_REQUEST_NOT_USABLE",
                        unmet.getFirst().description(),
                        Map.of("unmetConditions", unmet));
        }
        var open = records.list(MAINTENANCE, new EntityRef(ENTITY, id)).stream()
                .filter(e -> e.confirmedAt() == null)
                .findFirst()
                .orElseThrow(() -> conflict("STATE_CHANGED", "Không có lần bảo trì đang mở"));
        String kind = open.content().kind();
        boolean renewal = Set.of("VEHICLE_INSPECTION", "INSURANCE").contains(kind);
        if ("PASS".equals(in.result())
                && renewal
                && (in.nextDueOn() == null || !in.nextDueOn().isAfter(in.date())))
            throw new InvalidValueException("Lần đăng kiểm, bảo hiểm Đạt cần hạn kế tiếp sau ngày thực hiện (FR-014)");
        var content = new AssetKinds.Maintenance(
                kind,
                in.result(),
                in.date(),
                in.content().strip(),
                in.performedBy().strip(),
                Boolean.TRUE.equals(in.outsourced()),
                in.cost(),
                outsourcedCost ? in.procurementRequestId() : null);
        records.updateDraft(MAINTENANCE, open.id(), content);
        records.confirm(MAINTENANCE, open.id(), time.now());
        if (port != null)
            port.linkSource(in.procurementRequestId(), ProcurementSourceType.MAINTENANCE, open.id(), in.cost());
        boolean pass = "PASS".equals(in.result());
        AssetStatus to = pass ? READY : DAMAGED;
        bedTransition(row, to, false, null);
        if (pass && "VEHICLE_INSPECTION".equals(kind))
            store.renewDue(id, to, "vehicle_inspection_due_on", "vehicle_inspection_overdue", in.nextDueOn());
        else if (pass && "INSURANCE".equals(kind))
            store.renewDue(id, to, "insurance_due_on", "insurance_overdue", in.nextDueOn());
        else {
            // Bảo trì, kiểm định: hạn kế tiếp = ngày + chu kỳ (FR-014)
            LocalDate next = pass && row.maintenanceIntervalDays() != null
                    ? in.date().plusDays(row.maintenanceIntervalDays())
                    : null;
            store.maintenanceResult(id, to, next, pass);
        }
        record(id, "RECORD_MAINTENANCE_RESULT", row.status(), to, null);
        publish(id, row.departmentId(), row.status(), to, "record-maintenance-result", List.of());
    }

    /**
     * FR-013, Q-314: Phụ trách bộ phận lập yêu cầu thanh lý (loại {@code M03.ASSET_DISPOSAL} của 000), bắt buộc lý do;
     * trạng thái chưa đổi tới khi Quản lý viện duyệt. Đang có yêu cầu chờ duyệt thì chặn.
     */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void requestDisposal(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        requireNotDisposed(row);
        var reason = new Reason(in.reason());
        requireDepartmentLead(row.departmentId());
        if (row.bedId() != null) {
            // FR-015, Q-51: giường có người hoặc giữ chỗ thì chặn như BR-M03-05; kiểm lại khi áp dụng
            var unmet = beds.checkAssetCommand(row.bedId(), BedAssetCommand.RETIREMENT, false, false);
            if (!unmet.isEmpty()) throw new ConditionNotMetException(unmet);
        }
        var api = approvals.getIfAvailable();
        if (api == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có yêu cầu phê duyệt của 000");
        boolean pending = store.disposalRequest(id)
                .flatMap(api::find)
                .filter(r -> "PENDING_APPROVAL".equals(String.valueOf(r.status()))
                        || "DRAFT".equals(String.valueOf(r.status())))
                .isPresent();
        if (pending) throw conflict("STATE_CHANGED", "Tài sản đang có yêu cầu thanh lý chờ duyệt");
        var request = api.create(new CreateRequest(
                AssetDisposalHandler.TYPE,
                json.valueToTree(Map.of("assetId", id)),
                LocalDate.ofInstant(time.now(), BusinessTime.ZONE),
                reason,
                true));
        store.setDisposalRequest(id, request.id());
        record(id, "REQUEST_DISPOSAL", row.status(), row.status(), reason);
    }

    /** Áp dụng yêu cầu thanh lý đã duyệt (trong transaction áp dụng của 000): tài sản Đã thanh lý, trạng thái cuối. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void dispose(long id, long requestId) {
        var row = locked(id);
        requireNotDisposed(row);
        bedTransition(row, DISPOSED, false, "Thanh lý");
        store.dispose(id, time.now());
        record(id, "DISPOSE", row.status(), DISPOSED, null);
        publish(id, row.departmentId(), row.status(), DISPOSED, "dispose", List.of());
    }

    // ---- Tài sản giường (Phase 8) ---------------------------------------------------------------------------------

    /**
     * FR-016, Q-282: hư hỏng phát hiện khi dọn giường; Trưởng tầng (tầng mình) hoặc Phụ trách bộ phận xác nhận, ghi
     * nguồn BED_PREPARATION rồi làm như Báo hỏng với ngoại lệ FR-013a của 003 (hủy phân bổ tương lai). 003 không còn bản
     * ghi hư hỏng riêng nên {@code damageId} của api-019 không dùng (chủ dự án chọn 2026-11-06).
     */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void linkBedPreparationDamage(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        if (row.bedId() == null) throw conflict("STATE_CHANGED", "Chỉ tài sản giường có hư hỏng khi dọn giường");
        if (blank(in.description())) throw new InvalidValueException("Báo hỏng cần mô tả");
        requireStatus(row, READY);
        reportBedDamage(row, in, "BED_PREPARATION");
    }

    /**
     * FR-015, Q-245, Q-265: Trưởng tầng (tầng mình) hoặc Quản lý viện gỡ dấu "chờ chuyển người" kèm lý do, cả khi giường
     * còn người; mức mất an toàn bắt buộc nội dung đã sửa và người sửa. Trạng thái không đổi; giường Trống thì 003 kích
     * hoạt BR-M02-01.
     */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void removeTransferFlag(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        if (row.bedId() == null || !row.awaitingResidentTransfer())
            throw conflict("STATE_CHANGED", "Tài sản không mang dấu chờ chuyển người");
        var reason = new Reason(in.reason());
        boolean floorManager = row.floorAreaId() != null
                && user.roles().contains(Role.FLOOR_MANAGER)
                && scope.inScope(user, new Target(ENTITY, Long.toString(id), null, row.floorAreaId()));
        if (!floorManager && !user.roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Trưởng tầng của tầng hoặc Quản lý viện gỡ dấu chờ chuyển người");
        var open = openFlag(row);
        if ("UNSAFE".equals(open.content().impactLevel()) && (blank(in.repairDescription()) || blank(in.repairedBy())))
            throw new InvalidValueException("Gỡ dấu mức mất an toàn cần nội dung đã sửa và người, đơn vị sửa (Q-265)");
        closeFlag(row, open, reason.value(), in.repairDescription(), in.repairedBy());
        beds.residentTransferFlagRemoved(row.bedId());
        record(id, "REMOVE_TRANSFER_FLAG", row.status(), row.status(), reason);
    }

    /**
     * FR-015: Báo hỏng tài sản giường. Giường không có người: tài sản Hỏng, giường Đang bảo trì cùng lần (báo hỏng lại
     * giường mang dấu thì gỡ dấu, Q-232). Giường có người (003 trả BR-M03-05): bắt buộc mức ảnh hưởng, ghi hư hỏng mang
     * dấu "chờ chuyển người" (bản nháp tới khi gỡ), trạng thái không đổi; nhắc Trưởng tầng, mức mất an toàn gửi Khẩn cấp.
     */
    private void reportBedDamage(AssetRow row, AssetCommandInput in, String source) {
        boolean preparation = "BED_PREPARATION".equals(source);
        requireLeadOrFloorManager(row, !preparation && row.awaitingResidentTransfer());
        long bedId = row.bedId();
        var unmet =
                beds.checkAssetCommand(bedId, BedAssetCommand.MAINTENANCE, preparation, row.awaitingResidentTransfer());
        boolean occupied = unmet.stream().anyMatch(c -> "BR-M03-05".equals(c.code()));
        if (occupied && !preparation && !row.awaitingResidentTransfer()) {
            flagOccupiedBed(row, in);
            return;
        }
        if (!unmet.isEmpty()) throw new ConditionNotMetException(unmet);
        bedTransition(row, DAMAGED, preparation, "Báo hỏng lại");
        var now = time.now();
        records.add(
                DAMAGE,
                new EntityRef(ENTITY, row.id()),
                new NewRecordEvent<>(
                        new AssetKinds.Damage(in.description().strip(), source, null, false),
                        null,
                        staff(),
                        false,
                        now,
                        null,
                        now,
                        row.floorAreaId(),
                        null));
        store.updateStatus(row.id(), DAMAGED);
        record(row.id(), preparation ? "LINK_BED_PREPARATION_DAMAGE" : "REPORT_DAMAGE", row.status(), DAMAGED, null);
        publish(
                row.id(),
                row.departmentId(),
                row.status(),
                DAMAGED,
                preparation ? "link-bed-preparation-damage" : "report-damage",
                notice(
                        "ASSET_DAMAGED",
                        row.id(),
                        "Tài sản " + row.code() + " bị báo hỏng",
                        new RecipientGroup.DepartmentLead(row.departmentId(), new Substitution.Default())));
    }

    private void flagOccupiedBed(AssetRow row, AssetCommandInput in) {
        String impact = in.impactLevel();
        if (!"UNSAFE".equals(impact) && !"STILL_SAFE".equals(impact))
            throw new InvalidValueException(
                    "Báo hỏng giường có người cần mức ảnh hưởng UNSAFE hoặc STILL_SAFE (Q-245)");
        long bedId = row.bedId();
        var suitable = "UNSAFE".equals(impact) ? beds.suitableEmptyBedsForOccupant(bedId) : List.<LocationView>of();
        var now = time.now();
        records.add(
                DAMAGE,
                new EntityRef(ENTITY, row.id()),
                new NewRecordEvent<>(
                        new AssetKinds.Damage(in.description().strip(), "DAMAGE_REPORT", impact, true),
                        null,
                        staff(),
                        false,
                        now,
                        null,
                        null,
                        row.floorAreaId(),
                        null));
        store.setAwaitingTransfer(row.id(), true);
        beds.residentTransferFlagAdded(bedId);
        record(row.id(), "REPORT_DAMAGE_AWAITING", row.status(), row.status(), null);
        long floor = row.floorAreaId();
        var notices = new ArrayList<NotificationRequest>(notice(
                "ASSET_DAMAGED",
                row.id(),
                "Tài sản " + row.code() + " bị báo hỏng",
                new RecipientGroup.DepartmentLead(row.departmentId(), new Substitution.Default())));
        if ("UNSAFE".equals(impact)) {
            String title = "Giường " + row.code() + " hỏng mất an toàn, cần chuyển người gấp";
            var recipients = BedAssetRules.unsafeRecipients(floor, suitable);
            String content = recipients.size() > 2
                    ? title + ". Giường Trống phù hợp ở tầng khác: " + BedAssetRules.otherFloorBeds(floor, suitable)
                    : title;
            notices.add(request("BED_DAMAGED_UNSAFE", row.id(), NotificationLevel.URGENT, title, content, recipients));
        } else {
            String title = "Giường " + row.code() + " bị báo hỏng, cần chuyển người sang giường khác";
            notices.add(request(
                    "BED_AWAITING_TRANSFER",
                    row.id(),
                    NotificationLevel.LOW,
                    title,
                    title,
                    List.of(new RecipientGroup.FloorManager(floor, new Substitution.Default()))));
        }
        events.publishEvent(new BedDamageNotice(row.id(), bedId, notices));
    }

    /** FR-015: đổi trạng thái giường theo trạng thái tài sản mới, cùng transaction; dấu chờ chuyển người được gỡ. */
    private void bedTransition(AssetRow row, AssetStatus to, boolean preparation, String flagRemoval) {
        if (row.bedId() == null) return;
        var command = BedAssetRules.bedCommand(row.status(), to);
        if (command.isEmpty()) return;
        long bedId = row.bedId();
        var unmet = beds.checkAssetCommand(bedId, command.get(), preparation, row.awaitingResidentTransfer());
        if (!unmet.isEmpty()) throw new ConditionNotMetException(unmet);
        beds.applyAssetCommand(bedId, command.get(), new Basis("FR-015", null, row.id(), null));
        if (row.awaitingResidentTransfer() && flagRemoval != null) {
            closeFlag(row, openFlag(row), flagRemoval, null, null);
            beds.residentTransferFlagRemoved(bedId);
        }
    }

    private RecordEvent<AssetKinds.Damage> openFlag(AssetRow row) {
        return records.list(DAMAGE, new EntityRef(ENTITY, row.id())).stream()
                .filter(e -> e.confirmedAt() == null
                        && Boolean.TRUE.equals(e.content().awaitingTransfer()))
                .findFirst()
                .orElseThrow(() -> conflict("STATE_CHANGED", "Không có hư hỏng mang dấu chờ chuyển người"));
    }

    /** Lần gỡ dấu đặt một lần trên hư hỏng mang dấu (data-model mục 4). */
    private void closeFlag(
            AssetRow row, RecordEvent<AssetKinds.Damage> open, String reason, String repair, String repairedBy) {
        var c = open.content();
        records.updateDraft(
                DAMAGE,
                open.id(),
                new AssetKinds.Damage(
                        c.description(),
                        c.source(),
                        c.impactLevel(),
                        true,
                        reason,
                        blank(repair) ? null : repair.strip(),
                        blank(repairedBy) ? null : repairedBy.strip()));
        records.confirm(DAMAGE, open.id(), time.now());
        store.setAwaitingTransfer(row.id(), false);
    }

    /** FR-011: Quản lý viện chuyển bộ phận quản lý, bắt buộc lý do; trạng thái giữ nguyên, ghi lịch sử bộ phận. */
    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.APPROVE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void transferDepartment(Target target, long id, AssetCommandInput in) {
        var row = locked(id);
        requireNotDisposed(row);
        var reason = new Reason(in.reason());
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện chuyển tài sản giữa bộ phận");
        if (in.newDepartmentId() == null || in.newDepartmentId() == row.departmentId())
            throw new InvalidValueException("Cần bộ phận mới khác bộ phận hiện tại");
        var now = time.now();
        history.add(
                DEPARTMENT,
                id,
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        Long.toString(row.departmentId()),
                        Long.toString(in.newDepartmentId()),
                        in.newDepartmentId(),
                        now,
                        "TRANSFER_DEPARTMENT",
                        staff(),
                        false,
                        reason,
                        null,
                        null,
                        null,
                        row.floorAreaId(),
                        null));
        store.updateDepartment(id, in.newDepartmentId());
        record(id, "TRANSFER_DEPARTMENT", row.status(), row.status(), reason);
    }

    // ---- Đọc -----------------------------------------------------------------------------------------------------

    /**
     * Quản lý viện, Hành chính: mọi tài sản (W-08 XEM); Phụ trách bộ phận: tài sản của bộ phận mình; Trưởng tầng: thêm
     * tài sản có vị trí thuộc tầng trong phạm vi. Quyền xem theo vai trò kiểm ở đây (phạm vi ASSIGNED không gắn đối tượng).
     */
    @Transactional(readOnly = true)
    public List<AssetRow> list(Long departmentId, Long typeId, AssetStatus status) {
        if (!permissions.permissions(user).contains(new PermissionKey(Function.ASSET_VEHICLE, Operation.VIEW)))
            throw new PermissionDeniedException("Không có quyền xem tài sản");
        var rows = store.list(departmentId, typeId, status);
        if (user.roles().contains(Role.NURSING_HOME_MANAGER) || user.roles().contains(Role.OFFICE_STAFF)) return rows;
        long staff = staff();
        var api = duties.getObject();
        return rows.stream()
                .filter(r -> api.departmentLead(r.departmentId())
                                .filter(l -> l == staff)
                                .isPresent()
                        || (r.floorAreaId() != null
                                && user.roles().contains(Role.FLOOR_MANAGER)
                                && scope.inScope(
                                        user, new Target(ENTITY, Long.toString(r.id()), null, r.floorAreaId()))))
                .toList();
    }

    /** Nhãn vị trí hiện hành (api-019 {@code Asset.location}). */
    public String locationLabel(AssetRow row) {
        return switch (row.locationType()) {
            case "ROOM" -> structure.room(row.locationId()).map(r -> r.name()).orElse("Phòng");
            case "FLOOR" -> structure.floor(row.locationId()).map(f -> f.name()).orElse("Tầng");
            case "AREA" -> structure.area(row.locationId()).map(a -> a.name()).orElse("Khu vực");
            case "PARKING_LOT" -> "Bãi xe";
            default -> "Kho tài sản";
        };
    }

    // ---- Dùng chung ----------------------------------------------------------------------------------------------

    private AssetInput validated(AssetInput in, boolean creating) {
        if (blank(in.code())
                || in.typeId() == null
                || blank(in.name())
                || in.departmentId() == null
                || blank(in.source())) throw new InvalidValueException("Cần mã, loại, tên, bộ phận, nguồn");
        if (!Set.of("PURCHASED", "DONATED", "TRANSFERRED_IN").contains(in.source()) && creating)
            throw new InvalidValueException("Nguồn là PURCHASED, DONATED hoặc TRANSFERRED_IN");
        var type = typeCode(in.typeId());
        if (creating) {
            catalog.requireActive(ASSET_TYPE, in.typeId());
            if (type.filter(BED_CODE::equals).isPresent())
                throw conflict(
                        "BED_ASSET_TYPE_NOT_ALLOWED", "Tài sản giường chỉ tạo kèm lệnh Tạo giường của 003 (Q-238)");
            if (!"PURCHASED".equals(in.source()) && blank(in.sourceDescription()))
                throw new InvalidValueException("Hàng được tặng hoặc chuyển cần ghi nguồn (FR-008c)");
            if (!"PURCHASED".equals(in.source()) && in.procurementRequestId() != null)
                throw new InvalidValueException("Chỉ tài sản mua mới gắn yêu cầu mua sắm");
        }
        boolean vehicle = type.filter(VEHICLE_CODE::equals).isPresent();
        if (!vehicle
                && (in.licensePlate() != null
                        || in.seatCount() != null
                        || in.vehicleInspectionDueOn() != null
                        || in.insuranceDueOn() != null))
            throw new InvalidValueException("Biển số, số chỗ, hạn đăng kiểm, bảo hiểm chỉ cho loại Xe");
        if ((in.maintenanceIntervalDays() != null && in.maintenanceIntervalDays() <= 0)
                || (in.seatCount() != null && in.seatCount() <= 0)
                || (in.acquisitionCost() != null && in.acquisitionCost() < 0))
            throw new InvalidValueException("Chu kỳ, số chỗ phải dương; nguyên giá không âm");
        return new AssetInput(
                in.code().strip(),
                in.typeId(),
                in.name().strip(),
                in.departmentId(),
                in.source(),
                blank(in.sourceDescription()) ? null : in.sourceDescription().strip(),
                in.procurementRequestId(),
                in.acquisitionCost(),
                in.inServiceOn(),
                in.maintenanceIntervalDays(),
                in.maintenanceDueOn(),
                in.licensePlate(),
                in.seatCount(),
                in.vehicleInspectionDueOn(),
                in.insuranceDueOn());
    }

    private Optional<String> typeCode(long typeId) {
        return Optional.ofNullable(catalog.require(ASSET_TYPE, typeId).code());
    }

    /** Đổi vị trí (và trạng thái): đóng khoảng vị trí đang mở, mở khoảng mới. */
    private void move(AssetRow row, AssetStatus to, AssetCommandInput in, String command, Reason reason) {
        String type = in.locationType();
        if (type == null
                || !Set.of("AREA", "FLOOR", "ROOM", "PARKING_LOT", "ASSET_STORAGE")
                        .contains(type)) throw new InvalidValueException("Cần vị trí hợp lệ");
        Long locationId = Set.of("PARKING_LOT", "ASSET_STORAGE").contains(type) ? null : in.locationId();
        Long floor = floorOf(type, locationId);
        var now = time.now();
        history.closeOpen(LOCATION, row.id(), now);
        history.add(LOCATION, row.id(), location(row, type, locationId, floor, command, reason, now));
        store.updateLocation(row.id(), to, type, locationId, floor);
        record(row.id(), command.toUpperCase(Locale.ROOT).replace('-', '_'), row.status(), to, reason);
        if (to != row.status()) publish(row.id(), row.departmentId(), row.status(), to, command, List.of());
    }

    private Long floorOf(String type, Long locationId) {
        return switch (type) {
            case "ROOM" ->
                structure
                        .room(requireId(locationId))
                        .filter(r -> r.active())
                        .orElseThrow(() -> new InvalidValueException("Phòng không tồn tại hoặc đã ngừng"))
                        .floorId();
            case "FLOOR" ->
                structure
                        .floor(requireId(locationId))
                        .filter(f -> f.active())
                        .orElseThrow(() -> new InvalidValueException("Tầng không tồn tại hoặc đã ngừng"))
                        .id();
            case "AREA" -> {
                structure
                        .area(requireId(locationId))
                        .filter(a -> a.active())
                        .orElseThrow(() -> new InvalidValueException("Khu vực không tồn tại hoặc đã ngừng"));
                yield null;
            }
            default -> null;
        };
    }

    private static long requireId(Long id) {
        if (id == null) throw new InvalidValueException("Cần id vị trí");
        return id;
    }

    private HistoryEntry<AssetKinds.Location> location(
            AssetRow before, String type, Long id, Long floor, String command, Reason reason, Instant at) {
        return new HistoryEntry<>(
                HistoryKind.PERIOD,
                before == null ? null : before.locationType(),
                type,
                id,
                at,
                command.toUpperCase(Locale.ROOT).replace('-', '_'),
                staff(),
                false,
                reason,
                null,
                null,
                new AssetKinds.Location(type, id, reason == null ? null : reason.value()),
                floor,
                null);
    }

    /** Q-292: bộ phận chưa có Phụ trách thì chặn; Phụ trách khác người dùng thì 403. */
    private void requireDepartmentLead(long departmentId) {
        var lead = duties.getObject().departmentLead(departmentId);
        if (lead.isEmpty())
            throw conflict("DEPARTMENT_LEAD_MISSING", "Bộ phận chưa có Phụ trách bộ phận; Quản lý viện cần chỉ định");
        if (lead.get() != staff())
            throw new PermissionDeniedException(
                    "NOT_DEPARTMENT_LEAD", "Chỉ Phụ trách bộ phận quản lý tài sản thực hiện lệnh", Map.of());
    }

    /** FR-015: báo hỏng lại, đưa vào bảo trì giường mang dấu thì Quản lý viện cũng làm được. */
    private void requireLeadOrFloorManager(AssetRow row, boolean managerAllowed) {
        if (managerAllowed && user.roles().contains(Role.NURSING_HOME_MANAGER)) return;
        requireLeadOrFloorManager(row);
    }

    /** Q-313: Trưởng tầng chỉ với tài sản có vị trí thuộc tầng trong phạm vi; còn lại Phụ trách bộ phận. */
    private void requireLeadOrFloorManager(AssetRow row) {
        boolean lead = duties.getObject()
                .departmentLead(row.departmentId())
                .filter(l -> l == staff())
                .isPresent();
        if (lead) return;
        boolean floorManager = row.floorAreaId() != null
                && user.roles().contains(Role.FLOOR_MANAGER)
                && scope.inScope(user, new Target(ENTITY, Long.toString(row.id()), null, row.floorAreaId()));
        if (!floorManager)
            throw new PermissionDeniedException("Chỉ Phụ trách bộ phận, hoặc Trưởng tầng với tài sản trong tầng mình");
    }

    private void requireNotBed(AssetRow row) {
        if (row.bedId() != null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "Chuyển vị trí tài sản giường chờ cổng đồng bộ vị trí của 003 (Q-243, tạm hoãn)");
    }

    private static void requireNotDisposed(AssetRow row) {
        if (row.status() == DISPOSED) throw conflict("STATE_CHANGED", "Tài sản đã thanh lý");
    }

    private static void requireStatus(AssetRow row, AssetStatus... allowed) {
        if (!Arrays.asList(allowed).contains(row.status()))
            throw conflict(
                    "STATE_CHANGED", "Tài sản đang ở trạng thái " + row.status() + ", không thực hiện được lệnh");
    }

    private ProcurementRequestPort procurementPort() {
        var port = procurement.getIfAvailable();
        if (port == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn yêu cầu mua sắm (M11)");
        return port;
    }

    private AssetRow locked(long id) {
        return store.lock(id).orElseThrow(NotFoundException::new);
    }

    private long staff() {
        return user.staffId()
                .orElseThrow(() -> new PermissionDeniedException("Chỉ tài khoản nhân viên thực hiện lệnh tài sản"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private List<NotificationRequest> notice(String event, long id, String title, RecipientGroup to) {
        return List.of(request(event, id, NotificationLevel.LOW, title, title, List.of(to)));
    }

    private NotificationRequest request(
            String event, long id, NotificationLevel level, String title, String content, List<RecipientGroup> to) {
        var now = time.now();
        return new NotificationRequest(
                "M03",
                event,
                SourceGroup.OTHER,
                new SourceRecord(ENTITY, Long.toString(id)),
                event + ":" + id + ":" + now,
                Optional.of(level),
                Optional.empty(),
                Optional.empty(),
                to,
                List.of(new ContentPart(InformationType.GENERAL, content)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                now,
                Map.of("assetId", id),
                false,
                Optional.empty(),
                Optional.empty());
    }

    private void publish(
            long id,
            long departmentId,
            AssetStatus from,
            AssetStatus to,
            String command,
            List<NotificationRequest> notifications) {
        events.publishEvent(new AssetStatusChanged(
                id,
                departmentId,
                store.find(id).map(AssetRow::bedId).orElse(null),
                from,
                to,
                command,
                time.now(),
                notifications));
    }

    private void record(long id, String action, AssetStatus from, AssetStatus to, Reason reason) {
        var entry = AuditLogEntry.builder()
                .action(new AuditAction("M03_ASSET_" + action, reason != null))
                .entity(ENTITY, id)
                .before(from == null ? Map.of() : Map.of("status", from.name()))
                .after(Map.of("status", to.name()));
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
