package com.eldercare.backend.billing.balance.internal.document.service;

import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.money;

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
import com.eldercare.backend.billing.api.dto.BalanceDocumentType;
import com.eldercare.backend.billing.api.events.BalanceDocumentIssued;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeRecords;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeRecords.PaymentNoticeRecord;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.service.LedgerPosting;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.error.BusinessException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu báo phí (017 T035, US9; FR-031, FR-032, FR-037, FR-038; BR-M11-21, 23; DBR-37; research E-10). {@link #publish}
 * là lệnh của Kế toán: dựng PDF và Excel từ số liệu đã chụp, lưu tệp, tạo dòng {@code record_event} (mỗi bảng chi phí tối
 * đa một phiếu), nhật ký, thông báo người thân có quyền xem chi phí; không tự tạo khi không có lệnh (Q-306). Tải lại đúng
 * file đã lưu và ghi nhật ký mỗi lần tải. Cổng người thân gộp phiếu và phiên bản hiện hành của bảng quyết toán.
 */
@Service
public class PaymentNoticeService {

    static final AuditAction ISSUED = new AuditAction("BALANCE_PAYMENT_NOTICE_ISSUED", false);
    static final String NOTICE = "M11_PAYMENT_NOTICE_ISSUED";
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Một phiếu cho màn hình (schema {@code PaymentNotice}). */
    public record Notice(
            long id,
            String noticeCode,
            long costStatementId,
            String period,
            String statementType,
            long total,
            long balanceBefore,
            long balanceAfter,
            String issuedBy,
            Instant issuedAt) {}

    /** Một chứng từ trên cổng người thân (schema {@code BalanceDocument}). */
    public record PortalDocument(
            BalanceDocumentType type, long id, String code, LocalDate date, String period, List<String> formats) {}

    private final LedgerPosting posting;
    private final FamilyCostsRight familyRight;
    private final RecordEventApi records;
    private final FileApi files;
    private final FileService fileContents;
    private final SettlementService settlements;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final CurrentUser user;
    private final BusinessTime time;

    PaymentNoticeService(
            LedgerPosting posting,
            FamilyCostsRight familyRight,
            RecordEventApi records,
            FileApi files,
            FileService fileContents,
            SettlementService settlements,
            ResidentApi residents,
            ParameterApi parameters,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            CurrentUser user,
            BusinessTime time) {
        this.posting = posting;
        this.familyRight = familyRight;
        this.records = records;
        this.files = files;
        this.fileContents = fileContents;
        this.settlements = settlements;
        this.residents = residents;
        this.parameters = parameters;
        this.audit = audit;
        this.events = events;
        this.user = user;
        this.time = time;
    }

    /**
     * FR-031, FR-032: phát hành phiếu cho một bảng đã chốt dưới khóa sổ; đã có phiếu thì 409 {@code ALREADY_ISSUED}.
     *
     * @param vietQr chuỗi VietQR không số tiền; {@code null} nếu không có tài khoản thu (bỏ ảnh QR)
     */
    @RequiresPermission(function = Function.PAYMENT_NOTICE, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Notice publish(Target target, long residentId, long costStatementId, PaymentNoticeData data, String vietQr) {
        posting.lock(residentId);
        String key = PaymentNoticeRecords.key(costStatementId);
        if (records.findByUniqueKey(PaymentNoticeRecords.KIND, key).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ALREADY_ISSUED", "Bảng chi phí này đã phát hành phiếu báo phí");
        }
        Instant now = time.now();
        long by = user.accountId();
        FacilityInfo facility = facility();
        byte[] qr = vietQr != null && data.balanceAfter() < 0 ? QrImage.png(vietQr, 300) : null;
        byte[] pdf = PaymentNoticeDocument.pdf(facility, null, data, qr);
        long pdfId = files.upload(data.code() + ".pdf", "application/pdf", pdf, by);
        long xlsxId = files.upload(data.code() + ".xlsx", XLSX, PaymentNoticeDocument.excel(facility, data), by);
        RecordEvent<PaymentNoticeRecord> row = records.add(
                PaymentNoticeRecords.KIND,
                ledgerRef(residentId),
                new NewRecordEvent<>(
                        new PaymentNoticeRecord(
                                data.code(),
                                costStatementId,
                                data.period(),
                                data.statementType(),
                                data.total(),
                                data.balanceBefore(),
                                data.balanceAfter(),
                                data.issuedBy(),
                                pdfId,
                                xlsxId),
                        null,
                        by,
                        false,
                        now,
                        null,
                        now,
                        null,
                        key));
        files.attach(pdfId, PaymentNoticeRecords.KIND.code(), row.id());
        files.attach(xlsxId, PaymentNoticeRecords.KIND.code(), row.id());
        var after = new LinkedHashMap<String, Object>();
        after.put("code", data.code());
        after.put("costStatementId", costStatementId);
        after.put("total", data.total());
        after.put("balanceAfter", data.balanceAfter());
        audit.write(AuditLogEntry.builder()
                .action(ISSUED)
                .entity(PaymentNoticeRecords.KIND.code(), row.id())
                .resident(residentId)
                .after(after)
                .build());
        events.publishEvent(new BalanceNotice(List.of(notice(residentId, row.id(), costStatementId, data, now))));
        events.publishEvent(new BalanceDocumentIssued(residentId, BalanceDocumentType.PAYMENT_NOTICE, row.id()));
        return view(row);
    }

    /** FR-031: phiếu của một hồ sơ, mới nhất trước. */
    @RequiresPermission(function = Function.PAYMENT_NOTICE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Notice> list(Target target, long residentId) {
        familyRight.require(residentId);
        return notices(residentId).stream().map(PaymentNoticeService::view).toList();
    }

    /** FR-037: đúng file đã lưu; mỗi lần tải ghi nhật ký. Quyền tệp kiểm lại ở {@code FilePermissionProvider}. */
    @RequiresPermission(function = Function.PAYMENT_NOTICE, operation = Operation.VIEW)
    @Transactional
    public FileContent download(Target target, long noticeId, boolean xlsx) {
        RecordEvent<PaymentNoticeRecord> row = records.require(PaymentNoticeRecords.KIND, noticeId);
        familyRight.require(row.parent().entityId());
        PaymentNoticeRecord c = row.content();
        FileContent file = fileContents.download(xlsx ? c.xlsxFileId() : c.pdfFileId(), user.accountId());
        audit.write(AuditLogEntry.builder()
                .action(SettlementService.DOWNLOADED)
                .entity(PaymentNoticeRecords.KIND.code(), noticeId)
                .resident(row.parent().entityId())
                .after(Map.of("format", xlsx ? "XLSX" : "PDF", "code", c.code()))
                .build());
        return file;
    }

    /** Người cao tuổi của một phiếu, để kiểm quyền theo phạm vi; không có thì 404. */
    @Transactional(readOnly = true)
    public long residentOf(long noticeId) {
        return records.require(PaymentNoticeRecords.KIND, noticeId).parent().entityId();
    }

    /** FR-038: phiếu và phiên bản hiện hành của bảng quyết toán, mới nhất trước. */
    @RequiresPermission(function = Function.PAYMENT_NOTICE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<PortalDocument> portalDocuments(Target target, long residentId) {
        familyRight.require(residentId);
        var documents = new ArrayList<PortalDocument>();
        for (RecordEvent<PaymentNoticeRecord> row : notices(residentId)) {
            documents.add(new PortalDocument(
                    BalanceDocumentType.PAYMENT_NOTICE,
                    row.id(),
                    row.content().code(),
                    row.confirmedAt().atZone(BusinessTime.ZONE).toLocalDate(),
                    row.content().period(),
                    List.of("PDF", "XLSX")));
        }
        var statements = settlements.statements(residentId);
        if (!statements.isEmpty()) {
            var current = statements.getLast();
            documents.add(new PortalDocument(
                    BalanceDocumentType.SETTLEMENT_STATEMENT,
                    current.id(),
                    "v" + current.content().version(),
                    current.confirmedAt().atZone(BusinessTime.ZONE).toLocalDate(),
                    null,
                    List.of("PDF")));
        }
        documents.sort(Comparator.comparing(PortalDocument::date).reversed());
        return documents;
    }

    /** Người cao tuổi của chứng từ cổng, để dựng {@link Target}. */
    @Transactional(readOnly = true)
    public long residentOfDocument(BalanceDocumentType type, long id) {
        return type == BalanceDocumentType.PAYMENT_NOTICE ? residentOf(id) : settlements.residentOfStatement(id);
    }

    private List<RecordEvent<PaymentNoticeRecord>> notices(long residentId) {
        return records.list(PaymentNoticeRecords.KIND, ledgerRef(residentId)).stream()
                .sorted(Comparator.comparing((RecordEvent<PaymentNoticeRecord> r) -> r.confirmedAt())
                        .thenComparing(RecordEvent::id)
                        .reversed())
                .toList();
    }

    private static Notice view(RecordEvent<PaymentNoticeRecord> row) {
        PaymentNoticeRecord c = row.content();
        return new Notice(
                row.id(),
                c.code(),
                c.costStatementId(),
                c.period(),
                c.statementType().name(),
                c.total(),
                c.balanceBefore(),
                c.balanceAfter(),
                c.issuedBy(),
                row.confirmedAt());
    }

    private FacilityInfo facility() {
        var value = parameters.read(ParameterCode.CFG_M11_11);
        return value.isSet() ? value.asTable(FacilityInfo.class) : null;
    }

    private NotificationRequest notice(
            long residentId, long noticeId, long costStatementId, PaymentNoticeData data, Instant now) {
        String text = "Phiếu báo phí " + data.code() + " của "
                + residents.identity(residentId).fullName() + " đã được phát hành; tải phiếu trên cổng người thân.";
        String costs = "Tổng bảng " + money(data.total()) + " đồng"
                + (data.balanceAfter() < 0 ? ", còn nợ " + money(-data.balanceAfter()) + " đồng." : ".");
        return new NotificationRequest(
                "M11",
                NOTICE,
                SourceGroup.COSTS,
                new SourceRecord(PaymentNoticeRecords.KIND.code(), Long.toString(noticeId)),
                NOTICE + ":" + costStatementId,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FamilyWithRight(residentId, FamilyRight.VIEW_COSTS)),
                List.of(new ContentPart(InformationType.GENERAL, text), new ContentPart(InformationType.COSTS, costs)),
                Optional.of(text),
                Optional.of("Phiếu báo phí"),
                Optional.empty(),
                false,
                now,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.empty());
    }

    private static EntityRef ledgerRef(long residentId) {
        return new EntityRef(BalanceTypes.LEDGER, residentId);
    }
}
