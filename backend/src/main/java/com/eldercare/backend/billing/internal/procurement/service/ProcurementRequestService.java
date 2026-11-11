package com.eldercare.backend.billing.internal.procurement.service;

import static com.eldercare.backend.billing.api.dto.ProcurementStatus.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.api.events.ProcurementRequestChanged;
import com.eldercare.backend.billing.api.spi.ProcurementIntakePort;
import com.eldercare.backend.billing.internal.procurement.domain.*;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow.Line;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T007 (FR-008 → FR-008c, FR-020, FR-021; BR-M11-17 → 19; Q-284, Q-290, Q-292, Q-293, Q-299; research W-05): vòng đời
 * yêu cầu mua sắm theo bảng data-model mục 3. Quyền vai trò kiểm ở {@link RequiresPermission} (chú thích ³⁷, ³⁸ chỉ mô
 * tả ở lớp vai trò); điều kiện Phụ trách bộ phận, người lập, Kế toán khác người ghi đã mua kiểm ở đây. Hệ thống không tự
 * lập yêu cầu (Q-306). Loại MEDICATION (FR-008d, Q-337): Điều dưỡng của bộ phận cũng lập, gửi, rút lại, hủy được với
 * người cao tuổi trong phạm vi phân công; mọi người lập phải xem được nhóm HEALTH của người cao tuổi; dòng gắn đơn nguồn
 * viện chưa kết thúc của đúng người đó, kiểm qua {@link PrescriptionApi#unendedFacilityPrescriptions} của M07 (chủ dự án
 * duyệt 2026-11-08 thay {@code ProcurementPrescriptionPort}).
 */
@Service
public class ProcurementRequestService {
    public static final String ENTITY = "M11.PROCUREMENT_REQUEST";
    public static final String RECEIPT = "M11.PROCUREMENT_RECEIPT";

    private final ProcurementRequestStore store;
    private final ObjectProvider<DutyApi> duties;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final FileApi files;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ProcurementNotices notices;
    private final PermissionApi permissions;
    private final ScopeApi scope;
    private final FieldVisibilityApi visibility;
    private final ObjectProvider<PrescriptionApi> prescriptions;
    private final ObjectProvider<ProcurementIntakePort> intakes;

    public ProcurementRequestService(
            ProcurementRequestStore store,
            ObjectProvider<DutyApi> duties,
            CurrentUser user,
            BusinessTime time,
            ParameterApi parameters,
            FileApi files,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ProcurementNotices notices,
            PermissionApi permissions,
            ScopeApi scope,
            FieldVisibilityApi visibility,
            ObjectProvider<PrescriptionApi> prescriptions,
            ObjectProvider<ProcurementIntakePort> intakes) {
        this.store = store;
        this.duties = duties;
        this.user = user;
        this.time = time;
        this.parameters = parameters;
        this.files = files;
        this.audit = audit;
        this.events = events;
        this.notices = notices;
        this.permissions = permissions;
        this.scope = scope;
        this.visibility = visibility;
        this.prescriptions = prescriptions;
        this.intakes = intakes;
    }

    /** Đối tượng kiểm quyền; yêu cầu loại thuốc gắn người cao tuổi của dòng đầu để Điều dưỡng qua lớp phạm vi. */
    public Target target(long id) {
        var resident = store.find(id)
                .filter(r -> r.type() == ProcurementType.MEDICATION)
                .flatMap(r -> r.lines().stream()
                        .map(Line::residentId)
                        .filter(Objects::nonNull)
                        .findFirst())
                .orElse(null);
        return new Target(ENTITY, Long.toString(id), resident, null);
    }

    // ---- Nháp --------------------------------------------------------------------------------------------------

    /**
     * Lập yêu cầu Nháp: Phụ trách bộ phận của bộ phận đó (FR-008; Q-292, Q-293, Q-299), hoặc Điều dưỡng với loại thuốc
     * (FR-008d). Tự kiểm quyền (RequiresPermissionArchTest.SELF_CHECKED): khóa (PURCHASE_REQUEST, CREATE) không gắn đối
     * tượng, hoặc với loại thuốc trên từng người cao tuổi của dòng, để người ngoài phạm vi nhận 403.
     */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long create(ProcurementContent draft) {
        var content = normalize(draft);
        requireCreatePermission(content);
        requireAuthor(content.type(), content.departmentId());
        checkMedicationLines(content);
        long id = store.insert(content, staff(), time.now());
        record(id, "CREATE", null, DRAFT, null, Map.of("type", content.type(), "total", content.estimatedTotal()));
        publish(id, content.departmentId(), content.type(), null, DRAFT, "create", List.of());
        return id;
    }

    /** Sửa Nháp (FR-008), người lập; If-Match theo phiên bản. */
    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void update(Target target, long id, String ifMatch, ProcurementContent draft) {
        var row = locked(id);
        requireStatus(row, DRAFT);
        VersionEtag.requireMatch(ifMatch, row.version(), Map.of("id", id, "version", row.version()));
        requireCreator(row);
        var content = normalize(draft);
        if (content.departmentId() != row.departmentId())
            throw new InvalidValueException("Không đổi bộ phận của yêu cầu; hủy và lập yêu cầu mới");
        requireAuthor(row.type(), row.departmentId());
        checkMedicationLines(content);
        store.updateContent(id, content);
        record(id, "UPDATE_DRAFT", DRAFT, DRAFT, null, Map.of("total", content.estimatedTotal()));
    }

    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void submit(Target target, long id) {
        var row = locked(id);
        requireStatus(row, DRAFT);
        requireCreator(row);
        requireAuthor(row.type(), row.departmentId());
        move(row, PENDING_APPROVAL, "submit", null, Map.of(), List.of(ProcurementNotices.manager()));
    }

    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void withdraw(Target target, long id) {
        var row = locked(id);
        requireStatus(row, PENDING_APPROVAL);
        requireCreator(row);
        move(row, DRAFT, "withdraw", null, Map.of(), List.of());
    }

    /** Nháp: người lập hủy; Đã duyệt: Phụ trách bộ phận hủy kèm lý do khi chưa có nguồn gắn (Q-290). */
    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void cancel(Target target, long id, ProcurementCommandInput input) {
        var row = locked(id);
        requireStatus(row, DRAFT, APPROVED);
        if (row.status() == DRAFT) {
            requireCreator(row);
            move(row, CANCELLED, "cancel", input.reason(), Map.of(), List.of());
            return;
        }
        var reason = new Reason(input.reason());
        requireDepartmentLead(row.departmentId());
        if (!row.linkedSources().isEmpty())
            throw conflict(
                    "HAS_LINKED_SOURCES", "Yêu cầu đã có tài sản hoặc kết quả bảo trì gắn; ghi đã mua phần đã làm");
        move(row, CANCELLED, "cancel", reason.value(), Map.of(), List.of());
    }

    // ---- Duyệt ---------------------------------------------------------------------------------------------------

    /** Quản lý viện duyệt, có thể giảm; giảm thì bắt buộc lý do (FR-008). */
    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.APPROVE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void approve(Target target, long id, ProcurementCommandInput input) {
        var row = locked(id);
        requireStatus(row, PENDING_APPROVAL);
        long approved = input.approvedAmount() == null ? row.estimatedTotal() : input.approvedAmount();
        if (approved < 0 || approved > row.estimatedTotal())
            throw new InvalidValueException("Số được duyệt từ 0 tới tổng dự kiến; không duyệt tăng");
        String reduction = approved < row.estimatedTotal() ? new Reason(input.reason()).value() : null;
        var columns = new LinkedHashMap<String, Object>();
        columns.put("approved_amount", approved);
        columns.put("reduction_reason", reduction);
        columns.put("decided_by_staff_id", staff());
        columns.put("approved_at", time.now());
        move(row, APPROVED, "approve", reduction, columns, List.of(ProcurementNotices.creator(row.createdByStaffId())));
    }

    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.APPROVE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void reject(Target target, long id, ProcurementCommandInput input) {
        var row = locked(id);
        requireStatus(row, PENDING_APPROVAL);
        var reason = new Reason(input.reason());
        var columns = new LinkedHashMap<String, Object>();
        columns.put("decided_by_staff_id", staff());
        move(
                row,
                REJECTED,
                "reject",
                reason.value(),
                columns,
                List.of(ProcurementNotices.creator(row.createdByStaffId())));
    }

    // ---- Ghi đã mua ----------------------------------------------------------------------------------------------

    /** FR-008a: chứng từ bắt buộc; vượt số được duyệt quá CFG-M11-07 bị chặn; chỉ Phụ trách bộ phận. */
    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void recordPurchased(Target target, long id, ProcurementCommandInput input) {
        var row = locked(id);
        requireStatus(row, APPROVED);
        requireDepartmentLead(row.departmentId());
        if (input.actualAmount() == null
                || input.actualAmount() < 0
                || input.purchasedOn() == null
                || input.supplier() == null
                || input.supplier().isBlank())
            throw new InvalidValueException("Cần số tiền thực tế, ngày mua, nhà cung cấp");
        var receipts = input.receiptFileIds() == null ? List.<Long>of() : List.copyOf(input.receiptFileIds());
        if (receipts.isEmpty() || receipts.stream().anyMatch(Objects::isNull))
            throw conflict("RECEIPT_REQUIRED", "Ghi đã mua cần chứng từ (ảnh hóa đơn hoặc phiếu bán hàng)");
        var percent = parameters.read(ParameterCode.CFG_M11_07).asPercent();
        if (!ApprovedAmountCheck.withinLimit(row.approvedAmount(), input.actualAmount(), percent)) {
            long maximum = ApprovedAmountCheck.maximum(row.approvedAmount(), percent);
            String text = "Số thực tế vượt số được duyệt quá "
                    + percent.stripTrailingZeros().toPlainString() + "% (tối đa " + maximum
                    + " đồng); lập yêu cầu bổ sung cho phần vượt, tham chiếu yêu cầu gốc";
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "APPROVED_AMOUNT_EXCEEDED",
                    text,
                    Map.of(
                            "unmetConditions",
                            List.of(new UnmetCondition("APPROVED_AMOUNT_EXCEEDED", text, "BR-M11-18", "M11", false)),
                            "originalRequestId",
                            id));
        }
        for (long file : receipts) files.attach(file, RECEIPT, id);
        var columns = new LinkedHashMap<String, Object>();
        columns.put("actual_amount", input.actualAmount());
        columns.put("purchased_on", input.purchasedOn());
        columns.put("supplier", input.supplier().strip());
        columns.put("receipt_file_ids", receipts);
        columns.put("purchase_recorded_by_staff_id", staff());
        columns.put("purchase_recorded_at", time.now());
        move(row, PENDING_RECONCILIATION, "record-purchased", null, columns, List.of(ProcurementNotices.accountant()));
    }

    // ---- Đối chiếu -----------------------------------------------------------------------------------------------

    /** FR-008b: Kế toán, khác người ghi đã mua; loại tài sản thì tổng nguyên giá phải bằng số thực tế. */
    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void confirmMatch(Target target, long id) {
        var row = locked(id);
        requireStatus(row, PENDING_RECONCILIATION);
        requireReconciler(row);
        if (!ReceiptMatch.matches(row.type(), row.actualAmount(), row.linkedSourceTotal()))
            throw conflict(
                    "RECEIPT_MISMATCH",
                    "Tổng nguyên giá tài sản gắn (" + row.linkedSourceTotal() + ") khác số tiền thực tế ("
                            + row.actualAmount() + ")");
        var columns = new LinkedHashMap<String, Object>();
        columns.put("reconciliation_result", "MATCHED");
        columns.put("reconciled_by_staff_id", staff());
        move(row, RECONCILED, "confirm-match", null, columns, List.of());
    }

    /** Thiếu hoặc sai chứng từ: về Đã duyệt để Phụ trách bộ phận ghi đã mua lại. */
    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void returnForReconciliation(Target target, long id, ProcurementCommandInput input) {
        var row = locked(id);
        requireStatus(row, PENDING_RECONCILIATION);
        var reason = new Reason(input.reason());
        requireReconciler(row);
        var columns = new LinkedHashMap<String, Object>();
        for (var column : List.of(
                "actual_amount",
                "purchased_on",
                "supplier",
                "receipt_file_ids",
                "purchase_recorded_by_staff_id",
                "purchase_recorded_at")) columns.put(column, null);
        move(
                row,
                APPROVED,
                "return-for-reconciliation",
                reason.value(),
                columns,
                List.of(ProcurementNotices.creator(row.createdByStaffId())));
    }

    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void recordDiscrepancy(Target target, long id, ProcurementCommandInput input) {
        var row = locked(id);
        requireStatus(row, PENDING_RECONCILIATION);
        var reason = new Reason(input.reason());
        if (input.discrepancyAmount() == null || input.discrepancyAmount() == 0)
            throw new InvalidValueException("Cần số tiền lệch");
        requireReconciler(row);
        var columns = new LinkedHashMap<String, Object>();
        columns.put("reconciliation_result", "DISCREPANCY");
        columns.put("discrepancy_amount", input.discrepancyAmount());
        columns.put("reconciled_by_staff_id", staff());
        move(row, DISCREPANCY, "record-discrepancy", reason.value(), columns, List.of(ProcurementNotices.manager()));
    }

    /** Quản lý viện chấp nhận lệch hoặc yêu cầu bộ phận hoàn tiền; báo người lập, Kế toán đã ghi lệch. */
    @RequiresPermission(function = Function.PURCHASE_REQUEST, operation = Operation.APPROVE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void resolveDiscrepancy(Target target, long id, ProcurementCommandInput input) {
        var row = locked(id);
        requireStatus(row, DISCREPANCY);
        var reason = new Reason(input.reason());
        if (!"ACCEPT_DISCREPANCY".equals(input.resolution()) && !"REQUEST_REFUND".equals(input.resolution()))
            throw new InvalidValueException("Cần cách xử lý: ACCEPT_DISCREPANCY hoặc REQUEST_REFUND");
        var columns = new LinkedHashMap<String, Object>();
        columns.put("discrepancy_resolution", input.resolution());
        var recipients = new ArrayList<RecipientGroup>(List.of(ProcurementNotices.creator(row.createdByStaffId())));
        if (row.reconciledByStaffId() != null) recipients.add(ProcurementNotices.reconciler(row.reconciledByStaffId()));
        move(row, RECONCILED, "resolve-discrepancy", reason.value(), columns, recipients);
    }

    // ---- Đọc -----------------------------------------------------------------------------------------------------

    /**
     * Quản lý viện, Kế toán: mọi yêu cầu; Phụ trách bộ phận: yêu cầu của bộ phận mình; người khác: yêu cầu do mình lập
     * (api-019 GET /procurement-requests).
     */
    @Transactional(readOnly = true)
    public List<ProcurementRequestRow> list(Long departmentId, ProcurementStatus status) {
        // Quyền xem theo vai trò không gắn đối tượng (Điều dưỡng phạm vi ASSIGNED, ⁴⁴); hàng lọc theo người dùng bên
        // dưới
        if (!permissions.permissions(user).contains(new PermissionKey(Function.PURCHASE_REQUEST, Operation.VIEW)))
            throw new PermissionDeniedException("Không có quyền xem yêu cầu mua sắm");
        if (user.roles().contains(Role.NURSING_HOME_MANAGER) || user.roles().contains(Role.ACCOUNTANT))
            return store.list(null, null, departmentId, status);
        long staff = staff();
        var api = duties.getObject();
        Set<Long> led = api.departmentOf(staff, time.now())
                .filter(d -> api.departmentLead(d).filter(l -> l == staff).isPresent())
                .map(Set::of)
                .orElse(Set.of());
        return store.list(led, staff, departmentId, status);
    }

    /**
     * "Đã nhận" của api-019 (FR-008d, 11.7): chỉ loại MEDICATION; mọi dòng có lần tiếp nhận Đạt theo
     * {@link ProcurementIntakePort} (006 cài); thiếu bean thì chưa biết ({@code null}).
     */
    /**
     * Lệnh người xem làm được trên yêu cầu (api-019 {@code allowedCommands}, chủ dự án duyệt 2026-11-10; chỉ để hiện nút,
     * lệnh vẫn kiểm lại khi gọi). Theo đúng điều kiện trạng thái, quyền và người thực hiện của từng lệnh ở trên;
     * {@code update} là sửa Nháp (PUT).
     */
    public List<String> allowedCommands(ProcurementRequestRow row) {
        Long me = user.staffId().orElse(null);
        if (me == null) return List.of();
        var granted = permissions.allowedCommands(
                user,
                target(row.id()),
                Map.of(
                        "UPDATE", new PermissionKey(Function.PURCHASE_REQUEST, Operation.UPDATE),
                        "APPROVE", new PermissionKey(Function.PURCHASE_REQUEST, Operation.APPROVE)));
        boolean update = granted.contains("UPDATE");
        boolean approve = granted.contains("APPROVE");
        boolean creator = row.createdByStaffId() == me;
        boolean lead = duties.getObject()
                .departmentLead(row.departmentId())
                .filter(l -> l.longValue() == me)
                .isPresent();
        boolean author = lead || nurseAuthor(row.type(), row.departmentId());
        boolean reconciler =
                user.roles().contains(Role.ACCOUNTANT) && !Objects.equals(row.purchaseRecordedByStaffId(), me);
        var commands = new ArrayList<String>();
        switch (row.status()) {
            case DRAFT -> {
                if (update && creator && author) commands.addAll(List.of("update", "submit"));
                if (update && creator) commands.add("cancel");
            }
            case PENDING_APPROVAL -> {
                if (update && creator) commands.add("withdraw");
                if (approve) commands.addAll(List.of("approve", "reject"));
            }
            case APPROVED -> {
                if (update && lead) commands.addAll(List.of("record-purchased", "cancel"));
            }
            case PENDING_RECONCILIATION -> {
                if (update && reconciler)
                    commands.addAll(List.of("confirm-match", "return-for-reconciliation", "record-discrepancy"));
            }
            case DISCREPANCY -> {
                if (approve) commands.add("resolve-discrepancy");
            }
            default -> {}
        }
        return List.copyOf(commands);
    }

    public Boolean received(ProcurementRequestRow row) {
        if (row.type() != ProcurementType.MEDICATION) return null;
        var source = intakes.getIfAvailable();
        if (source == null) return null;
        var passed = source.passedLineIds(row.id());
        for (int i = 0; i < row.lines().size(); i++)
            if (!passed.contains(ProcurementLineId.of(row.id(), i))) return false;
        return true;
    }

    // ---- Dùng chung ----------------------------------------------------------------------------------------------

    /** Tổng dự kiến = tổng thành tiền các dòng, thành tiền làm tròn tới đồng (FR-008). */
    private ProcurementContent normalize(ProcurementContent draft) {
        if (draft.type() == null) throw new InvalidValueException("Cần loại yêu cầu");
        new Reason(draft.reason());
        if (draft.lines() == null || draft.lines().isEmpty()) throw new InvalidValueException("Cần ít nhất một dòng");
        if (draft.lines().size() > ProcurementLineId.MAX_LINES)
            throw new InvalidValueException("Tối đa " + ProcurementLineId.MAX_LINES + " dòng mỗi yêu cầu");
        if (draft.type() == ProcurementType.MEDICATION && (draft.priority() == null || draft.neededBy() == null))
            throw new InvalidValueException("Yêu cầu thuốc cần mức ưu tiên và thời điểm cần (FR-008d)");
        if (draft.priority() != null && !Set.of("NORMAL", "URGENT").contains(draft.priority()))
            throw new InvalidValueException("Mức ưu tiên không hợp lệ");
        if (draft.originalRequestId() != null && !store.exists(draft.originalRequestId()))
            throw new InvalidValueException("Yêu cầu gốc không tồn tại");
        boolean medication = draft.type() == ProcurementType.MEDICATION;
        var lines = new ArrayList<Line>();
        long total = 0;
        for (var line : draft.lines()) {
            if (line == null
                    || line.name() == null
                    || line.name().isBlank()
                    || line.quantity() == null
                    || line.quantity().signum() <= 0
                    || line.estimatedUnitPrice() < 0)
                throw new InvalidValueException("Dòng cần tên, số lượng dương, đơn giá dự kiến không âm");
            if (medication
                    && (line.residentId() == null
                            || line.prescriptionId() == null
                            || blank(line.activeIngredient())
                            || blank(line.strength())
                            || blank(line.dosageForm())
                            || blank(line.unit())))
                throw new InvalidValueException(
                        "Dòng thuốc cần người cao tuổi, đơn thuốc, hoạt chất, hàm lượng, dạng, đơn vị (FR-008d)");
            long amount = line.quantity()
                    .multiply(BigDecimal.valueOf(line.estimatedUnitPrice()))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
            total += amount;
            lines.add(new Line(
                    line.name().strip(),
                    line.quantity(),
                    line.estimatedUnitPrice(),
                    amount,
                    line.residentId(),
                    line.prescriptionId(),
                    line.activeIngredient(),
                    line.strength(),
                    line.dosageForm(),
                    line.unit()));
        }
        return new ProcurementContent(
                draft.departmentId(),
                draft.type(),
                draft.priority(),
                draft.neededBy(),
                draft.relatedFileIds(),
                List.copyOf(lines),
                total,
                draft.reason().strip(),
                draft.neededOn(),
                draft.reference(),
                draft.originalRequestId());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void requireCreatePermission(ProcurementContent content) {
        var key = new PermissionKey(Function.PURCHASE_REQUEST, Operation.CREATE);
        if (permissions.hasPermission(user, key, Target.UNSPECIFIED).allowed()) return;
        boolean perResident = content.type() == ProcurementType.MEDICATION
                && content.lines().stream()
                        .allMatch(l -> permissions
                                .hasPermission(user, key, new Target(ENTITY, null, l.residentId(), null))
                                .allowed());
        if (!perResident) throw new PermissionDeniedException("Không có quyền lập yêu cầu mua sắm");
    }

    /** Điều dưỡng của bộ phận với loại thuốc (FR-008d); còn lại Phụ trách bộ phận (FR-008). */
    private boolean nurseAuthor(ProcurementType type, long departmentId) {
        return type == ProcurementType.MEDICATION
                && user.roles().contains(Role.NURSING_STAFF)
                && duties.getObject()
                        .departmentOf(staff(), time.now())
                        .filter(d -> d == departmentId)
                        .isPresent();
    }

    private void requireAuthor(ProcurementType type, long departmentId) {
        if (!nurseAuthor(type, departmentId)) requireDepartmentLead(departmentId);
    }

    /**
     * FR-008d, research W-05: người lập xem được HEALTH của người cao tuổi của dòng (không cấp thêm quyền); Điều dưỡng
     * không là Phụ trách bộ phận chỉ với người trong phạm vi phân công; đơn là đơn nguồn viện chưa kết thúc của người đó.
     */
    private void checkMedicationLines(ProcurementContent content) {
        if (content.type() != ProcurementType.MEDICATION) return;
        var source = prescriptions.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn đơn thuốc của 006");
        boolean lead = duties.getObject()
                .departmentLead(content.departmentId())
                .filter(l -> l == staff())
                .isPresent();
        for (var line : content.lines()) {
            var resident = new Target("M01.RESIDENT", Long.toString(line.residentId()), line.residentId(), null);
            if (!lead && !scope.inScope(user, resident))
                throw new PermissionDeniedException("Người cao tuổi ngoài phạm vi phân công");
            if (visibility.canView(user, InfoGroup.HEALTH, resident) != Visibility.FULL)
                throw new PermissionDeniedException("Người lập không xem được thông tin thuốc của người cao tuổi này");
            if (source.unendedFacilityPrescriptions(line.residentId()).stream()
                    .noneMatch(p -> p.prescriptionId() == line.prescriptionId()))
                throw new ConditionNotMetException(List.of(new UnmetCondition(
                        "PRESCRIPTION_NOT_FACILITY_UNENDED",
                        "Đơn không phải đơn nguồn viện Hiệu lực, Tạm dừng, Chờ hiệu lực của người cao tuổi của dòng",
                        "FR-008d",
                        "M11",
                        false)));
        }
    }

    /** Q-292: bộ phận chưa có Phụ trách thì chặn và nhắc Quản lý viện; Phụ trách khác người dùng thì 403. */
    private void requireDepartmentLead(long departmentId) {
        long staff = staff();
        var lead = duties.getObject().departmentLead(departmentId);
        if (lead.isEmpty()) {
            notices.departmentLeadMissing(departmentId);
            throw conflict("DEPARTMENT_LEAD_MISSING", "Bộ phận chưa có Phụ trách bộ phận; Quản lý viện cần chỉ định");
        }
        if (lead.get() != staff)
            throw new PermissionDeniedException(
                    "NOT_DEPARTMENT_LEAD", "Chỉ Phụ trách bộ phận của bộ phận này thực hiện lệnh", Map.of());
    }

    private void requireCreator(ProcurementRequestRow row) {
        if (row.createdByStaffId() != staff())
            throw new PermissionDeniedException("PERMISSION_DENIED", "Chỉ người lập thực hiện lệnh này", Map.of());
    }

    /** BR-M11-19, Q-293: người ghi đã mua không đối chiếu; chỉ Kế toán đối chiếu. */
    private void requireReconciler(ProcurementRequestRow row) {
        if (Objects.equals(row.purchaseRecordedByStaffId(), staff()))
            throw conflict("PURCHASE_RECORDER_CANNOT_RECONCILE", "Người ghi đã mua không đối chiếu yêu cầu này");
        if (!user.roles().contains(Role.ACCOUNTANT))
            throw new PermissionDeniedException("PERMISSION_DENIED", "Chỉ Kế toán đối chiếu chứng từ", Map.of());
    }

    private ProcurementRequestRow locked(long id) {
        return store.lock(id).orElseThrow(NotFoundException::new);
    }

    private static void requireStatus(ProcurementRequestRow row, ProcurementStatus... allowed) {
        if (!Arrays.asList(allowed).contains(row.status()))
            throw conflict(
                    "STATE_CHANGED", "Yêu cầu đang ở trạng thái " + row.status() + ", không thực hiện được lệnh");
    }

    private long staff() {
        return user.staffId()
                .orElseThrow(() -> new PermissionDeniedException("Chỉ tài khoản nhân viên thực hiện lệnh mua sắm"));
    }

    private void move(
            ProcurementRequestRow row,
            ProcurementStatus to,
            String command,
            String reason,
            Map<String, Object> columns,
            List<RecipientGroup> recipients) {
        var now = time.now();
        boolean last = to == RECONCILED || to == REJECTED || to == CANCELLED;
        store.transition(row.id(), to, reason, last ? now : null, columns);
        record(row.id(), command.toUpperCase(Locale.ROOT).replace('-', '_'), row.status(), to, reason, columns);
        var notifications = notices.request(
                "PROCUREMENT_REQUEST_" + to.name(),
                row.id(),
                Integer.toString(row.version() + 1),
                "Yêu cầu mua sắm #" + row.id() + ": " + title(to),
                recipients);
        publish(row.id(), row.departmentId(), row.type(), row.status(), to, command, notifications);
    }

    private static String title(ProcurementStatus to) {
        return switch (to) {
            case DRAFT -> "đã rút lại về Nháp";
            case PENDING_APPROVAL -> "chờ duyệt chi phí";
            case APPROVED -> "đã duyệt, cần ghi đã mua";
            case PENDING_RECONCILIATION -> "chờ đối chiếu chứng từ";
            case DISCREPANCY -> "lệch chứng từ, cần xử lý";
            case RECONCILED -> "đã đối chiếu";
            case REJECTED -> "bị từ chối";
            case CANCELLED -> "đã hủy";
        };
    }

    private void publish(
            long id,
            long departmentId,
            ProcurementType type,
            ProcurementStatus from,
            ProcurementStatus to,
            String command,
            List<NotificationRequest> notifications) {
        events.publishEvent(
                new ProcurementRequestChanged(id, departmentId, type, from, to, command, time.now(), notifications));
    }

    private void record(
            long id, String action, ProcurementStatus from, ProcurementStatus to, String reason, Map<String, ?> after) {
        var data = new LinkedHashMap<String, Object>();
        after.forEach((k, v) -> data.put(k, v instanceof List<?> list ? List.copyOf(list) : v));
        data.put("status", to.name());
        var entry = AuditLogEntry.builder()
                .action(new AuditAction("PROCUREMENT_" + action, reason != null))
                .entity(ENTITY, id)
                .before(from == null ? Map.of() : Map.of("status", from.name()))
                .after(data);
        if (reason != null) entry.reason(new Reason(reason));
        audit.write(entry.build());
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
