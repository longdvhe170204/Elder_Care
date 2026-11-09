package com.eldercare.backend.billing.balance.internal.status.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.billing.api.events.BalanceStatusChanged;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.domain.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.CollectionAccounts;
import com.eldercare.backend.billing.balance.internal.ledger.service.PaymentService;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotices;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceStatusRule;
import com.eldercare.backend.billing.balance.internal.status.domain.Cycle;
import com.eldercare.backend.billing.balance.internal.status.domain.Decision;
import com.eldercare.backend.billing.balance.internal.status.domain.NoticeDecision;
import com.eldercare.backend.billing.balance.internal.status.domain.StatusComputation;
import com.eldercare.backend.billing.balance.internal.status.domain.StatusSnapshot;
import com.eldercare.backend.billing.balance.internal.status.domain.Trigger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính tình trạng số dư (017 T020; FR-016 → FR-020, BR-M11-13; research E-07): hằng ngày (job) và ngay sau giao dịch
 * nộp tiền vào sổ số dư. Chỉ tính hồ sơ có hợp đồng Hiệu lực chưa ở trạng thái cuối, và hồ sơ trạng thái cuối (để còn báo
 * "còn nợ", FR-020). Mỗi lần tính ghi một dòng lịch sử; tình trạng đổi thì phát {@link BalanceStatusChanged} cho 016.
 * Tổng chưa chốt, phí một ngày lấy từ 010 ({@code CostForBalancePort}); chưa có bản cài thì 0 và chưa biết (chỉ xét Còn
 * nợ).
 */
@Service
public class BalanceStatusService implements PaymentService.PaymentRecordedHook {

    private final LedgerStore ledgers;
    private final ContractReadApi contracts;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final ObjectProvider<CostForBalancePort> costs;
    private final CollectionAccounts accounts;
    private final StatusHistoryApi history;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public BalanceStatusService(
            LedgerStore ledgers,
            ContractReadApi contracts,
            ResidentApi residents,
            ParameterApi parameters,
            ObjectProvider<CostForBalancePort> costs,
            CollectionAccounts accounts,
            StatusHistoryApi history,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.ledgers = ledgers;
        this.contracts = contracts;
        this.residents = residents;
        this.parameters = parameters;
        this.costs = costs;
        this.accounts = accounts;
        this.history = history;
        this.events = events;
        this.time = time;
    }

    /** Hồ sơ được tính: có hợp đồng Hiệu lực, hoặc đã ở trạng thái cuối (bảng chuyển tình trạng, FR-020). */
    public boolean eligible(long residentId) {
        return residents.isFinalStatus(residentId)
                || contracts.ofResident(residentId).stream().anyMatch(c -> c.status() == ContractStatus.ACTIVE);
    }

    /** Ngay sau giao dịch nộp tiền vào sổ số dư (Q-252, Q-257): chạy trong transaction của lệnh ghi. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void paymentRecorded(long residentId, List<BalanceTransactionRow> rows) {
        var last = rows.stream().filter(r -> r.ledger() == Ledger.BALANCE).reduce((a, b) -> b);
        if (last.isEmpty() || !eligible(residentId)) return;
        recalculate(residentId, Trigger.PAYMENT, "TRANSACTION:" + last.get().id());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculate(long residentId, Trigger trigger, String source) {
        LedgerState ledger = ledgers.lock(residentId).orElseThrow();
        LocalDate today = time.today();
        CostForBalancePort port = costs.getIfAvailable();
        long unfinalized = port == null ? 0 : port.unfinalizedTotal(residentId);
        Long fee = port == null ? null : port.baseDailyFee(residentId, today).orElse(null);
        int threshold =
                (int) parameters.read(ParameterCode.CFG_M11_04).asDuration().toDays();
        int cycleDays =
                (int) parameters.read(ParameterCode.CFG_M11_05).asDuration().toDays();
        StatusComputation computed = BalanceStatusRule.compute(ledger.balance(), unfinalized, fee, threshold);
        Decision decision = NoticeDecision.decide(
                ledger.status(),
                computed.status(),
                new Cycle(ledger.cycleStartedOn(), ledger.lastNoticeOn(), ledger.inDebtSince()),
                today,
                cycleDays,
                trigger,
                ledger.remindersStopped());
        Cycle cycle = decision.cycle();
        ledgers.saveStatus(
                residentId,
                computed.status(),
                computed.daysCovered(),
                cycle.cycleStartedOn(),
                cycle.lastNoticeOn(),
                cycle.inDebtSince());
        history.add(
                StatusSnapshot.TRACK,
                residentId,
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        ledger.status().name(),
                        computed.status().name(),
                        null,
                        time.now(),
                        trigger.name(),
                        null,
                        true,
                        null,
                        null,
                        null,
                        new StatusSnapshot(
                                ledger.balance(), unfinalized, fee, computed.daysCovered(), decision.notices()),
                        null,
                        StatusSnapshot.TRACK.entityType() + ":" + residentId + ":" + today + ":" + source));
        if (ledger.status() != computed.status()) {
            events.publishEvent(new BalanceStatusChanged(residentId, computed.status()));
        }
        if (!decision.notices().isEmpty()) {
            events.publishEvent(BalanceNotices.of(
                    new BalanceNotices.Facts(
                            residentId,
                            residents.identity(residentId).fullName(),
                            computed.status(),
                            ledger.balance(),
                            unfinalized,
                            accounts.instructions(ledger.paymentCode())
                                    .map(BalanceStatusService::instructions)
                                    .orElse(null),
                            cycle.inDebtSince(),
                            trigger == Trigger.DAILY ? today.toString() : source,
                            time.now()),
                    decision.notices()));
        }
    }

    /** Dấu "số dư không đủ" cho đề nghị mua hộ (FR-021, BR-M11-15): số dư − tổng chưa chốt nhỏ hơn số tiền dự kiến. */
    @Transactional(readOnly = true)
    public boolean insufficient(long residentId, long amount) {
        CostForBalancePort port = costs.getIfAvailable();
        long unfinalized = port == null ? 0 : port.unfinalizedTotal(residentId);
        return ledgers.find(residentId)
                .map(l -> l.balance() - unfinalized < amount)
                .orElse(true);
    }

    private static String instructions(PaymentInstructions p) {
        return "Nộp tiền: " + p.bank() + ", số tài khoản " + p.accountNumber() + ", chủ tài khoản " + p.accountHolder()
                + ", nội dung chuyển khoản " + p.paymentCode() + ".";
    }
}
