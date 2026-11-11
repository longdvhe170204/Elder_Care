package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.api.spi.ProcurementIntakePort;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementLineId;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Q-410 (chủ dự án 2026-11-11; 006 FR-054, 019 FR-008d): dòng thuốc của yêu cầu mua sắm loại thuốc đã ghi đã mua (Chờ đối
 * chiếu, Lệch, Đã đối chiếu) chưa có lần tiếp nhận Đạt, của người cao tuổi người xem được tiếp nhận thuốc; chỉ thông tin
 * thuốc của dòng, không giá, lý do, người duyệt. Quyền xem danh sách yêu cầu mua sắm giữ nguyên.
 */
@Service
public class MedicationLinesAwaitingIntake {
    /** Đối tượng kiểm quyền tiếp nhận của 006 ({@code SuppliedMedicationIntakeService.ENTITY_TYPE}). */
    static final String INTAKE_ENTITY = "M07.SUPPLIED_MEDICATION_INTAKE";

    private static final List<ProcurementStatus> PURCHASED = List.of(
            ProcurementStatus.PENDING_RECONCILIATION, ProcurementStatus.DISCREPANCY, ProcurementStatus.RECONCILED);
    private static final PermissionKey RECEIVE = new PermissionKey(Function.MEDICATION_RECEIPT, Operation.CREATE);

    public record Line(
            long id,
            long procurementRequestId,
            long residentId,
            Long prescriptionId,
            String medicationName,
            String activeIngredient,
            String strength,
            String dosageForm,
            BigDecimal quantity,
            String unit) {}

    private final ProcurementRequestStore store;
    private final ProcurementIntakePort intakes;
    private final PermissionApi permissions;
    private final CurrentUser user;

    public MedicationLinesAwaitingIntake(
            ProcurementRequestStore store, ProcurementIntakePort intakes, PermissionApi permissions, CurrentUser user) {
        this.store = store;
        this.intakes = intakes;
        this.permissions = permissions;
        this.user = user;
    }

    /** Tự kiểm quyền: Tiếp nhận thuốc theo vai trò, rồi phạm vi từng người cao tuổi (Điều dưỡng ASSIGNED, ⁴⁴). */
    @Transactional(readOnly = true)
    public List<Line> list() {
        if (!permissions.permissions(user).contains(RECEIVE))
            throw new PermissionDeniedException("Không có quyền tiếp nhận thuốc cung ứng");
        var out = new ArrayList<Line>();
        for (ProcurementStatus status : PURCHASED) {
            for (var r : store.list(null, null, null, status)) {
                if (r.type() != ProcurementType.MEDICATION) continue;
                Set<Long> passed = intakes.passedLineIds(r.id());
                for (int i = 0; i < r.lines().size(); i++) {
                    var l = r.lines().get(i);
                    long id = ProcurementLineId.of(r.id(), i);
                    if (passed.contains(id) || l.residentId() == null || !receivable(l.residentId())) continue;
                    out.add(new Line(
                            id,
                            r.id(),
                            l.residentId(),
                            l.prescriptionId(),
                            l.name(),
                            l.activeIngredient(),
                            l.strength(),
                            l.dosageForm(),
                            l.quantity(),
                            l.unit()));
                }
            }
        }
        return out;
    }

    private boolean receivable(long residentId) {
        return permissions
                .hasPermission(user, RECEIVE, new Target(INTAKE_ENTITY, null, residentId, null))
                .allowed();
    }
}
