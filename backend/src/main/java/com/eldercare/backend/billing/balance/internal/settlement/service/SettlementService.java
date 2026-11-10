package com.eldercare.backend.billing.balance.internal.settlement.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.ResidencyEndingReadApi;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.api.dto.SettlementInfoView;
import com.eldercare.backend.billing.api.FinalPeriodCostApi;
import com.eldercare.backend.billing.api.dto.BalanceDocumentType;
import com.eldercare.backend.billing.api.events.BalanceDocumentIssued;
import com.eldercare.backend.billing.balance.internal.document.domain.CostTypeLabels;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.TypeTotal;
import com.eldercare.backend.billing.balance.internal.document.service.RepresentativeNames;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.LedgerPosting;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementLedger;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRecords;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules.Basis;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules.Unmet;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementStatementData;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementStatementRecord;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SignedCopyRecord;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileContent;
import com.eldercare.backend.common.file.FileService;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quyết toán số dư và tiền cọc (017 T031, T032; FR-022 → FR-024, FR-034, FR-037; BR-M11-22, 23; DBR-38; research E-08,
 * E-09). Đánh giá hai mã điều kiện từ sổ, giao dịch Chờ duyệt, bảng quyết toán hiện hành, kỳ cuối của 010 ({@link
 * FinalPeriodCostApi}; thiếu bean thì chưa đạt) và ngoại lệ của 004. Lệnh Xác nhận quyết toán của Kế toán dựng bảng PDF
 * phiên bản mới (lưu tệp, dòng {@code record_event}), không tự tạo khi không có lệnh (Q-306).
 */
@Service
public class SettlementService {

    static final AuditAction CONFIRMED = new AuditAction("BALANCE_SETTLEMENT_CONFIRMED", false);
    static final AuditAction SIGNED_COPY = new AuditAction("BALANCE_SETTLEMENT_SIGNED_COPY", false);
    public static final AuditAction DOWNLOADED = new AuditAction("BALANCE_DOCUMENT_DOWNLOADED", false);
    static final String NOTICE = "M11_SETTLEMENT_CONFIRMED";

    /** Kết quả đánh giá tại lúc gọi; {@code statements} theo phiên bản tăng dần. */
    public record Evaluation(
            SettlementConditions.Result result,
            boolean exceptionApproved,
            SettlementLedger ledger,
            long balance,
            long depositBalance,
            List<RecordEvent<SettlementStatementRecord>> statements,
            Optional<SettlementInfoView> info) {

        public Optional<RecordEvent<SettlementStatementRecord>> current() {
            return statements.isEmpty() ? Optional.empty() : Optional.of(statements.getLast());
        }
    }

    /** Một phiên bản bảng quyết toán cho màn hình (schema {@code SettlementStatement}). */
    public record Statement(
            long id,
            int version,
            boolean current,
            boolean hasException,
            long debtOrRefundAmount,
            String confirmedBy,
            Instant confirmedAt,
            boolean hasSignedCopy) {}

    public record View(Evaluation evaluation, List<Statement> versions) {}

    private final LedgerPosting posting;
    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final RecordEventApi records;
    private final FileApi files;
    private final FileService fileContents;
    private final ObjectProvider<FinalPeriodCostApi> finalPeriods;
    private final ObjectProvider<CostForBalancePort> costs;
    private final ObjectProvider<ResidencyEndingReadApi> endings;
    private final ContractReadApi contracts;
    private final ResidentApi residents;
    private final AccountApi accounts;
    private final ParameterApi parameters;
    private final SettlementChanges changes;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final CurrentUser user;
    private final BusinessTime time;
    private final RepresentativeNames representativeNames;

    SettlementService(
            LedgerPosting posting,
            LedgerStore ledgers,
            BalanceTransactionStore transactions,
            RecordEventApi records,
            FileApi files,
            FileService fileContents,
            ObjectProvider<FinalPeriodCostApi> finalPeriods,
            ObjectProvider<CostForBalancePort> costs,
            ObjectProvider<ResidencyEndingReadApi> endings,
            ContractReadApi contracts,
            ResidentApi residents,
            AccountApi accounts,
            ParameterApi parameters,
            SettlementChanges changes,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            CurrentUser user,
            BusinessTime time,
            RepresentativeNames representativeNames) {
        this.representativeNames = representativeNames;
        this.posting = posting;
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.records = records;
        this.files = files;
        this.fileContents = fileContents;
        this.finalPeriods = finalPeriods;
        this.costs = costs;
        this.endings = endings;
        this.contracts = contracts;
        this.residents = residents;
        this.accounts = accounts;
        this.parameters = parameters;
        this.changes = changes;
        this.audit = audit;
        this.events = events;
        this.user = user;
        this.time = time;
    }

    /** Hai mã điều kiện tại lúc gọi (FR-022); chỉ đọc. */
    @Transactional(readOnly = true)
    public Evaluation evaluate(long residentId) {
        SettlementLedger facts = SettlementLedger.of(transactions.list(residentId, null, null, null, null));
        Optional<LedgerState> state = ledgers.find(residentId);
        long balance = state.map(LedgerState::balance).orElse(0L);
        long deposit = state.map(LedgerState::depositBalance).orElse(0L);
        List<RecordEvent<SettlementStatementRecord>> statements = statements(residentId);
        Optional<RecordEvent<SettlementStatementRecord>> current =
                statements.isEmpty() ? Optional.empty() : Optional.of(statements.getLast());
        FinalPeriodCostApi finalPeriod = finalPeriods.getIfAvailable();
        boolean finalPaid = finalPeriod != null
                && finalPeriod.finalPeriodCondition(residentId).satisfied();
        SettlementConditions.Result result = SettlementConditions.evaluate(new SettlementConditions.Input(
                finalPaid,
                balance,
                deposit,
                facts.pending(),
                current.isPresent(),
                current.isPresent()
                        && facts.confirmedAfter(current.get().content().lastTransactionId())));
        ResidencyEndingReadApi ending = endings.getIfAvailable();
        boolean exception = ending != null && ending.hasApprovedSettlementException(residentId);
        Optional<SettlementInfoView> info = ending == null ? Optional.empty() : ending.settlementInfo(residentId);
        return new Evaluation(result, exception, facts, balance, deposit, statements, info);
    }

    /** {@code GET /residents/{id}/settlement}: Kế toán, Quản lý viện (Phụ lục 27 dòng "Bảng quyết toán"). */
    @RequiresPermission(function = Function.SETTLEMENT_RECORD, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public View view(Target target, long residentId) {
        Evaluation evaluation = evaluate(residentId);
        return new View(evaluation, versions(evaluation.statements()));
    }

    /** FR-034: Kế toán Xác nhận quyết toán dưới khóa sổ; không đạt thì 409 kèm danh sách điều kiện. */
    @RequiresPermission(function = Function.SETTLEMENT_RECORD, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Statement confirm(Target target, long residentId) {
        posting.lock(residentId);
        Evaluation ev = evaluate(residentId);
        Basis basis = basis(ev, residentId);
        List<Unmet> unmet = SettlementRules.confirmationUnmet(basis, ev.result(), ev.exceptionApproved());
        if (!unmet.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    unmet.getFirst().code(),
                    unmet.getFirst().description(),
                    Map.of(
                            "unmetConditions",
                            unmet.stream()
                                    .map(u -> new UnmetCondition(u.code(), u.description(), "FR-034", "M11", false))
                                    .toList()));
        }
        int version = ev.current().map(c -> c.content().version() + 1).orElse(1);
        Instant now = time.now();
        long by = user.accountId();
        boolean hasException = ev.result().settlement().status() == SettlementConditions.Status.NOT_MET;
        // E-09: số còn nợ hoặc còn phải hoàn là số dư hiện tại; khác 0 chỉ khi có ngoại lệ (lệnh đã kiểm)
        long remaining = ev.balance();
        SettlementStatementData data =
                data(ev, residentId, version, now, accounts.fullName(by).orElse(""));
        byte[] pdf = SettlementStatementDocument.pdf(facility(), null, data);
        long fileId =
                files.upload("bang-quyet-toan-" + residentId + "-v" + version + ".pdf", "application/pdf", pdf, by);
        RecordEvent<SettlementStatementRecord> row = records.add(
                SettlementRecords.STATEMENT,
                ledgerRef(residentId),
                new NewRecordEvent<>(
                        new SettlementStatementRecord(
                                version,
                                basis,
                                hasException,
                                remaining,
                                ev.ledger().lastConfirmedId(),
                                fileId,
                                by,
                                data),
                        null,
                        by,
                        false,
                        now,
                        null,
                        now,
                        null,
                        SettlementRecords.statementKey(residentId, version)));
        files.attach(fileId, SettlementRecords.STATEMENT.code(), row.id());
        var after = new LinkedHashMap<String, Object>();
        after.put("version", version);
        after.put("basis", basis.name());
        after.put("hasException", hasException);
        after.put("remaining", remaining);
        after.put("fileId", fileId);
        audit.write(AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(SettlementRecords.STATEMENT.code(), row.id())
                .resident(residentId)
                .after(after)
                .build());
        events.publishEvent(new BalanceNotice(List.of(notice(residentId, row.id(), version, remaining, basis, now))));
        events.publishEvent(new BalanceDocumentIssued(residentId, BalanceDocumentType.SETTLEMENT_STATEMENT, row.id()));
        changes.changed(residentId);
        return statement(row, true, false);
    }

    /** FR-034: bản scan có chữ ký vào phiên bản hiện hành, một lần; không đổi điều kiện. */
    @RequiresPermission(function = Function.SETTLEMENT_RECORD, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void attachSignedCopy(Target target, long statementId, long fileId) {
        RecordEvent<SettlementStatementRecord> statement = records.require(SettlementRecords.STATEMENT, statementId);
        long residentId = statement.parent().entityId();
        posting.lock(residentId);
        if (statements(residentId).getLast().id() != statementId) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NOT_CURRENT_VERSION", "Chỉ đính kèm bản ký vào phiên bản hiện hành");
        }
        String key = SettlementRecords.signedCopyKey(statementId);
        if (records.findByUniqueKey(SettlementRecords.SIGNED_COPY, key).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "SIGNED_COPY_EXISTS", "Phiên bản này đã có bản ký đính kèm");
        }
        Instant now = time.now();
        long by = user.accountId();
        RecordEvent<SignedCopyRecord> row = records.add(
                SettlementRecords.SIGNED_COPY,
                ledgerRef(residentId),
                new NewRecordEvent<>(
                        new SignedCopyRecord(statementId, fileId), null, by, false, now, null, now, null, key));
        files.attach(fileId, SettlementRecords.SIGNED_COPY.code(), row.id());
        audit.write(AuditLogEntry.builder()
                .action(SIGNED_COPY)
                .entity(SettlementRecords.STATEMENT.code(), statementId)
                .resident(residentId)
                .after(Map.of("signedCopyId", row.id(), "fileId", fileId))
                .build());
    }

    /** FR-037: đúng file đã lưu; mỗi lần tải ghi nhật ký. Quyền tệp kiểm lại ở {@code FilePermissionProvider}. */
    @RequiresPermission(function = Function.SETTLEMENT_RECORD, operation = Operation.VIEW)
    @Transactional
    public FileContent download(Target target, long statementId) {
        RecordEvent<SettlementStatementRecord> statement = records.require(SettlementRecords.STATEMENT, statementId);
        FileContent file = fileContents.download(statement.content().pdfFileId(), user.accountId());
        audit.write(AuditLogEntry.builder()
                .action(DOWNLOADED)
                .entity(SettlementRecords.STATEMENT.code(), statementId)
                .resident(statement.parent().entityId())
                .after(Map.of("format", "PDF", "version", statement.content().version()))
                .build());
        return file;
    }

    /** Người cao tuổi của một phiên bản, để kiểm quyền theo phạm vi; không có thì 404. */
    @Transactional(readOnly = true)
    public long residentOfStatement(long statementId) {
        return records.require(SettlementRecords.STATEMENT, statementId)
                .parent()
                .entityId();
    }

    @Transactional(readOnly = true)
    public List<RecordEvent<SettlementStatementRecord>> statements(long residentId) {
        return records.list(SettlementRecords.STATEMENT, ledgerRef(residentId)).stream()
                .sorted(Comparator.comparingInt(r -> r.content().version()))
                .toList();
    }

    /** Căn cứ của lệnh (FR-034); rỗng khi không có. */
    Basis basis(Evaluation ev, long residentId) {
        if (ev.info().isPresent()) {
            return ev.info().get().endCase() == EndCase.DEATH ? Basis.POST_DEATH : Basis.RESIDENCY_ENDING;
        }
        boolean newAfterLast = ev.current().isPresent()
                && ev.ledger().confirmedAfter(ev.current().get().content().lastTransactionId());
        return residents.isFinalStatus(residentId) && newAfterLast ? Basis.AFTER_FINAL_STATUS : null;
    }

    private List<Statement> versions(List<RecordEvent<SettlementStatementRecord>> statements) {
        return statements.stream()
                .map(s -> statement(
                        s,
                        s == statements.getLast(),
                        records.findByUniqueKey(SettlementRecords.SIGNED_COPY, SettlementRecords.signedCopyKey(s.id()))
                                .isPresent()))
                .toList();
    }

    private static Statement statement(RecordEvent<SettlementStatementRecord> s, boolean current, boolean signed) {
        SettlementStatementRecord c = s.content();
        return new Statement(
                s.id(),
                c.version(),
                current,
                c.hasException(),
                c.remaining(),
                c.data().confirmedBy(),
                s.confirmedAt(),
                signed);
    }

    private SettlementStatementData data(Evaluation ev, long residentId, int version, Instant now, String by) {
        SettlementInfoView info = ev.info().orElse(null);
        String contractCode = info == null || info.contractId() == null
                ? null
                : contracts
                        .contract(info.contractId())
                        .map(ContractView::contractNumber)
                        .orElse(null);
        SettlementLedger l = ev.ledger();
        return new SettlementStatementData(
                version,
                now,
                by,
                residents.identity(residentId).fullName(),
                representativeNames.of(residentId),
                info == null ? null : stayType(info.residenceType()),
                contractCode,
                info == null ? null : info.startDate(),
                info == null ? null : info.expectedEndDate(),
                info == null ? "Sau trạng thái cuối" : endCase(info.endCase()),
                finalPeriodTotals(ev.ledger().finalPeriodStatementId()),
                l.finalPeriodTotal(),
                l.balanceBeforeSettlement(),
                l.depositReceived(),
                l.depositOffset(),
                l.depositRefunded(),
                l.sinceFinalPeriod(),
                ev.balance(),
                ev.result().settlement().status() == SettlementConditions.Status.NOT_MET);
    }

    /** FR-034: tổng bảng kỳ cuối theo loại, lấy từ bảng Đã chốt của 010; chưa có bảng thì để trống. */
    private List<TypeTotal> finalPeriodTotals(Long statementId) {
        CostForBalancePort port = costs.getIfAvailable();
        if (statementId == null || port == null) return List.of();
        return port.finalizedStatement(statementId)
                .map(s -> s.statement().totalsByType().stream()
                        .map(t -> new TypeTotal(CostTypeLabels.of(t.type()), t.total()))
                        .toList())
                .orElse(List.of());
    }

    private FacilityInfo facility() {
        var value = parameters.read(ParameterCode.CFG_M11_11);
        return value.isSet() ? value.asTable(FacilityInfo.class) : null;
    }

    private NotificationRequest notice(
            long residentId, long statementId, int version, long remaining, Basis basis, Instant now) {
        String text = "Bảng quyết toán của " + residents.identity(residentId).fullName() + " (phiên bản " + version
                + ") đã được xác nhận; tải bảng trên cổng người thân.";
        String costs = remaining < 0
                ? "Còn nợ " + money(-remaining) + " đồng."
                : remaining > 0 ? "Còn phải hoàn " + money(remaining) + " đồng." : "Đã quyết toán đủ.";
        return new NotificationRequest(
                "M11",
                NOTICE,
                SourceGroup.COSTS,
                new SourceRecord(SettlementRecords.STATEMENT.code(), Long.toString(statementId)),
                NOTICE + ":" + residentId + ":" + version,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FamilyWithRight(residentId, FamilyRight.VIEW_COSTS)),
                List.of(new ContentPart(InformationType.GENERAL, text), new ContentPart(InformationType.COSTS, costs)),
                Optional.of(text),
                Optional.of("Bảng quyết toán"),
                Optional.empty(),
                false,
                now,
                Map.of(),
                basis == Basis.POST_DEATH,
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.empty());
    }

    static EntityRef ledgerRef(long residentId) {
        return new EntityRef(BalanceTypes.LEDGER, residentId);
    }

    private static String money(long amount) {
        return String.format(Locale.GERMANY, "%,d", amount);
    }

    private static String stayType(ResidenceType type) {
        if (type == null) return null;
        return switch (type) {
            case LONG_TERM_RESIDENTIAL -> "Nội trú dài hạn";
            case SHORT_TERM_RESIDENTIAL -> "Nội trú ngắn ngày";
            case DAY_CARE -> "Bán trú";
        };
    }

    private static String endCase(EndCase endCase) {
        return switch (endCase) {
            case CONTRACT_EXPIRED -> "Hết hạn hợp đồng";
            case DISCHARGE -> "Xuất viện";
            case TRANSFER_TO_ANOTHER_FACILITY -> "Chuyển cơ sở khác";
            case CONTRACT_TERMINATION -> "Chấm dứt hợp đồng";
            case DEATH -> "Qua đời";
        };
    }
}
