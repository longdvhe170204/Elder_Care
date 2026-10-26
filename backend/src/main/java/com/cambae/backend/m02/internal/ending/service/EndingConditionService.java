package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m01.api.ResidentStatusApi;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m02.api.dto.ConditionListView;
import com.cambae.backend.m02.api.dto.ConditionStatus;
import com.cambae.backend.m02.api.dto.ConditionView;
import com.cambae.backend.m02.internal.absence.service.AbsenceNames;
import com.cambae.backend.m02.internal.ending.domain.EndingConditionCode;
import com.cambae.backend.m02.internal.ending.domain.EndingExceptionRecord;
import com.cambae.backend.m02.internal.ending.domain.EndingStatus;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách điều kiện kết thúc lưu trú (a) → (h) (004 US7; FR-063, FR-064; research T-09). Dựng từ chính kết quả {@link
 * ResidentStatusApi#previewConditions} của lệnh Kết thúc lưu trú nên luôn khớp lệnh: điều kiện của 010, 013, 006, 007, 017
 * (mã {@code END_*}) và (e) của 004 nằm trong kết quả khi Chưa đạt, ngoài kết quả là Đạt; "Đạt (ngoại lệ)" khi có ngoại lệ
 * đã duyệt. (f) hợp đồng và (g) giường do hệ thống làm khi lệnh chạy nên hiện "Tự động". Dòng (h) gộp hai mã của 017: Chưa
 * đạt khi còn một trong hai, "Đạt (ngoại lệ)" khi ý tiền được miễn và ý xác nhận đạt.
 */
@Service
public class EndingConditionService {

    static final String CONTRACT = "END_F_CONTRACT";
    static final String BED = "END_G_BED";

    /** Dòng cố định của danh sách: mã, tên, căn cứ, module sở hữu. */
    private record Row(String code, String name, String basis, String module) {}

    private static final Row COSTS =
            new Row(EndingConditionCode.END_A_COSTS.name(), "Chi phí kỳ cuối đã chốt", "FR-063 (a), FR-066", "M11");
    private static final Row BELONGINGS =
            new Row(EndingConditionCode.END_B_BELONGINGS.name(), "Không còn đồ gửi cần nhận", "FR-063 (b)", "M12");
    private static final Row MEDICATION = new Row(
            EndingConditionCode.END_C_STORED_MEDICATION.name(), "Không còn thuốc gia đình gửi", "FR-063 (c)", "M07");
    private static final Row ALERTS = new Row(
            EndingConditionCode.END_D_ALERTS_INCIDENTS.name(), "Không còn cảnh báo, sự cố mở", "FR-063 (d)", "M05");
    private static final Row HANDOVER = new Row(
            EndingConditionCode.END_E_HANDOVER.name(), "Đã ghi nhận bàn giao người cao tuổi", "FR-063 (e)", "M02");
    private static final Row SETTLEMENT = new Row(
            EndingConditionCode.END_H_SETTLEMENT.name(),
            "Số dư và tiền cọc đã quyết toán",
            "FR-063 (h), FR-064",
            "M11");

    private final ResidencyEndingStore store;
    private final ResidentStatusApi residentStatus;
    private final EndingHandovers handovers;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final AbsenceNames names;

    EndingConditionService(
            ResidencyEndingStore store,
            ResidentStatusApi residentStatus,
            EndingHandovers handovers,
            ObjectProvider<ApprovalRequestApi> approvals,
            AbsenceNames names) {
        this.store = store;
        this.residentStatus = residentStatus;
        this.handovers = handovers;
        this.approvals = approvals;
        this.names = names;
    }

    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public ConditionListView conditions(Target target, long endingId) {
        return of(endingId);
    }

    /** Cho {@code AdmissionConditionApi.endingConditions} (012, 016); quyền do bên gọi kiểm. */
    @Transactional(readOnly = true)
    public ConditionListView of(long endingId) {
        ResidencyEnding ending = store.find(endingId).orElseThrow(NotFoundException::new);
        if (ending.status() == EndingStatus.CANCELLED) {
            throw ResidencyEndingService.notPreparing();
        }
        List<Condition> unmet = ending.preparing()
                ? residentStatus.previewConditions(ending.residentId(), StatusCommand.END_RESIDENCY)
                : List.of();
        Map<EndingConditionCode, EndingExceptionRecord> exceptions = new EnumMap<>(EndingConditionCode.class);
        store.exceptionsOf(endingId).forEach(e -> exceptions.putIfAbsent(e.conditionCode(), e));

        List<ConditionView> lines = new ArrayList<>();
        lines.add(moneyOrWork(COSTS, EndingConditionCode.END_A_COSTS, unmet, exceptions));
        lines.add(moneyOrWork(BELONGINGS, EndingConditionCode.END_B_BELONGINGS, unmet, exceptions));
        lines.add(moneyOrWork(MEDICATION, EndingConditionCode.END_C_STORED_MEDICATION, unmet, exceptions));
        lines.add(moneyOrWork(ALERTS, EndingConditionCode.END_D_ALERTS_INCIDENTS, unmet, exceptions));
        lines.add(handoverLine(ending, unmet));
        lines.add(new ConditionView(
                CONTRACT, "Hợp đồng được xử lý", ConditionStatus.AUTOMATIC, "FR-063 (f), FR-065", "M02", null));
        lines.add(new ConditionView(
                BED, "Giường được giải phóng", ConditionStatus.AUTOMATIC, "FR-063 (g), FR-065", "M03", null));
        lines.add(settlementLine(unmet, exceptions));
        return new ConditionListView(List.copyOf(lines), List.of());
    }

    /** Tên người duyệt yêu cầu ngoại lệ, cho phản hồi; rỗng nếu chưa có. */
    @Transactional(readOnly = true)
    public Optional<String> approverOf(long approvalRequestId) {
        ApprovalRequestApi api = approvals.getIfAvailable();
        if (api == null) {
            return Optional.empty();
        }
        return api.find(approvalRequestId).map(ApprovalRequestView::decidedBy).map(account -> names.account(account));
    }

    private ConditionView moneyOrWork(
            Row row,
            EndingConditionCode code,
            List<Condition> unmet,
            Map<EndingConditionCode, EndingExceptionRecord> exceptions) {
        List<Condition> mine =
                unmet.stream().filter(c -> code.name().equals(c.code())).toList();
        EndingExceptionRecord exception = exceptions.get(code);
        if (!mine.isEmpty()) {
            return new ConditionView(
                    row.code(), row.name(), ConditionStatus.NOT_MET, reasons(mine), row.module(), null);
        }
        if (exception != null) {
            return new ConditionView(
                    row.code(),
                    row.name(),
                    ConditionStatus.MET_BY_EXCEPTION,
                    row.basis(),
                    row.module(),
                    exception.approvalRequestId());
        }
        return new ConditionView(row.code(), row.name(), ConditionStatus.MET, row.basis(), row.module(), null);
    }

    private ConditionView handoverLine(ResidencyEnding ending, List<Condition> unmet) {
        List<Condition> mine = unmet.stream()
                .filter(c -> EndingConditionCode.END_E_HANDOVER.name().equals(c.code()))
                .toList();
        boolean met = mine.isEmpty()
                && (!ending.preparing() || !handovers.effective(ending.id()).isEmpty());
        return met
                ? new ConditionView(
                        HANDOVER.code(), HANDOVER.name(), ConditionStatus.MET, HANDOVER.basis(), "M02", null)
                : new ConditionView(
                        HANDOVER.code(),
                        HANDOVER.name(),
                        ConditionStatus.NOT_MET,
                        mine.isEmpty() ? "Chưa ghi nhận bàn giao người cao tuổi (FR-063 (e))" : reasons(mine),
                        "M02",
                        null);
    }

    private ConditionView settlementLine(
            List<Condition> unmet, Map<EndingConditionCode, EndingExceptionRecord> exceptions) {
        List<Condition> mine = unmet.stream()
                .filter(c -> EndingConditionCode.END_H_SETTLEMENT.name().equals(c.code())
                        || EndingConditionCode.END_H_SETTLEMENT_CONFIRMATION
                                .name()
                                .equals(c.code()))
                .toList();
        EndingExceptionRecord exception = exceptions.get(EndingConditionCode.END_H_SETTLEMENT);
        if (!mine.isEmpty()) {
            return new ConditionView(
                    SETTLEMENT.code(), SETTLEMENT.name(), ConditionStatus.NOT_MET, reasons(mine), "M11", null);
        }
        if (exception != null) {
            return new ConditionView(
                    SETTLEMENT.code(),
                    SETTLEMENT.name(),
                    ConditionStatus.MET_BY_EXCEPTION,
                    SETTLEMENT.basis(),
                    "M11",
                    exception.approvalRequestId());
        }
        return new ConditionView(
                SETTLEMENT.code(), SETTLEMENT.name(), ConditionStatus.MET, SETTLEMENT.basis(), "M11", null);
    }

    private static String reasons(List<Condition> conditions) {
        return conditions.stream().map(Condition::description).collect(Collectors.joining("; "));
    }
}
