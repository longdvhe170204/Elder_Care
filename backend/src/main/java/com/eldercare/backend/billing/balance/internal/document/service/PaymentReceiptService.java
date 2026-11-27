package com.eldercare.backend.billing.balance.internal.document.service;

import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.dateTime;
import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.money;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.billing.balance.internal.document.domain.AmountInWords;
import com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceDetails;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.billing.balance.internal.transaction.persistence.PendingTransactionStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.document.PdfDocument;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.IdentityView;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Biên nhận nộp tiền (Q-460, góp ý demo BF-10): dựng PDF khi tải từ giao dịch nộp đã xác nhận (giao dịch không sửa được);
 * số biên nhận theo mã giao dịch; ghi rõ không thay hóa đơn. Nhân viên: Kế toán, Quản lý viện; người thân: quyền xem chi
 * phí đang bật.
 */
@Service
public class PaymentReceiptService {

    private static final Set<TransactionType> PAYMENTS = Set.of(
            TransactionType.CASH_PAYMENT, TransactionType.BANK_TRANSFER_PAYMENT, TransactionType.DEPOSIT_RECEIPT);
    private static final Set<TransactionType> REFUNDS = Set.of(TransactionType.REFUND, TransactionType.DEPOSIT_REFUND);
    private static final Set<Role> STAFF_VIEW = Set.of(Role.ACCOUNTANT, Role.NURSING_HOME_MANAGER);

    private static final String NOT_AN_INVOICE = "Chứng từ này không thay thế hóa đơn.";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final BalanceTransactionStore transactions;
    private final BalanceQueries queries;
    private final FamilyCostsRight familyRight;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final CurrentUser user;
    private final PendingTransactionStore recipients;
    private final ObjectProvider<FamilyRelationshipApi> relationships;
    private final BusinessTime time;

    PaymentReceiptService(
            BalanceTransactionStore transactions,
            BalanceQueries queries,
            FamilyCostsRight familyRight,
            ResidentApi residents,
            ParameterApi parameters,
            CurrentUser user,
            PendingTransactionStore recipients,
            ObjectProvider<FamilyRelationshipApi> relationships,
            BusinessTime time) {
        this.recipients = recipients;
        this.relationships = relationships;
        this.time = time;
        this.transactions = transactions;
        this.queries = queries;
        this.familyRight = familyRight;
        this.residents = residents;
        this.parameters = parameters;
        this.user = user;
    }

    public record Receipt(String number, byte[] content) {}

    /** Người cao tuổi của giao dịch, để dựng {@link Target}; không có thì 404. */
    @Transactional(readOnly = true)
    public long residentOf(long transactionId) {
        return require(transactionId).residentId();
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Receipt receipt(Target target, long transactionId) {
        BalanceTransactionRow row = require(transactionId);
        if (user.type() == AccountType.FAMILY_MEMBER) {
            familyRight.require(row.residentId());
        } else if (user.roles().stream().noneMatch(STAFF_VIEW::contains)) {
            throw new PermissionDeniedException("Chỉ Kế toán, Quản lý viện tải biên nhận");
        }
        boolean paid = PAYMENTS.contains(row.type()) && row.amount() > 0;
        boolean refunded = REFUNDS.contains(row.type()) && row.amount() < 0;
        if (!paid && !refunded || row.status() != TransactionStatus.CONFIRMED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RECEIPT_NOT_AVAILABLE",
                    "Chỉ khoản nộp, khoản hoàn đã xác nhận mới có biên nhận");
        }
        String number = String.format(paid ? "BN-%06d" : "HT-%06d", row.id());
        return new Receipt(number, paid ? pdf(number, row) : refundPdf(number, row));
    }

    /**
     * Bảng kê các giao dịch đã chọn của một người cao tuổi (Q-460 mở rộng): chia nhóm gia đình nộp, hoàn lại, trừ chi
     * phí, khác; tổng từng nhóm và số dư hiện tại. Chỉ Kế toán, Quản lý viện.
     */
    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Receipt summary(Target target, long residentId, List<Long> ids) {
        if (user.type() != AccountType.STAFF || user.roles().stream().noneMatch(STAFF_VIEW::contains)) {
            throw new PermissionDeniedException("Chỉ Kế toán, Quản lý viện in bảng kê giao dịch");
        }
        if (ids.isEmpty()) {
            throw new InvalidValueException("Chọn ít nhất một giao dịch");
        }
        List<BalanceTransactionRow> rows = ids.stream()
                .distinct()
                .map(id -> transactions
                        .find(id)
                        .filter(t -> t.residentId() == residentId && t.status() == TransactionStatus.CONFIRMED)
                        .orElseThrow(() -> new InvalidValueException(
                                "Bảng kê chỉ gồm giao dịch Đã xác nhận của người cao tuổi này")))
                .sorted(Comparator.comparing(BalanceTransactionRow::at).thenComparing(BalanceTransactionRow::id))
                .toList();
        BalanceDetails balance = queries.balance(target, residentId);
        IdentityView resident = residents.identity(residentId);
        PdfDocument pdf = PdfDocument.a4(facility(), null, "BẢNG KÊ GIAO DỊCH");
        pdf.fields(
                2,
                "Người cao tuổi",
                resident.fullName(),
                "Mã hồ sơ",
                resident.profileCode(),
                "Ngày lập",
                dateTime(time.now()),
                "Người lập",
                queries.names().apply(user.accountId()));
        group(pdf, "Gia đình nộp", rows, PAYMENTS);
        group(pdf, "Hoàn lại gia đình", rows, REFUNDS);
        group(pdf, "Trừ chi phí (không thu thêm tiền)", rows, Set.of(TransactionType.STATEMENT_PAYMENT));
        group(pdf, "Điều chỉnh, cấn trừ cọc", rows, Set.of(TransactionType.ADJUSTMENT, TransactionType.DEPOSIT_OFFSET));
        pdf.paragraph("Số dư hiện tại: " + money(balance.balance()) + " đồng. Số dư cọc hiện tại: "
                + money(balance.depositBalance()) + " đồng.");
        pdf.signatures("Người lập bảng kê", "Người nhận bảng kê");
        pdf.note(NOT_AN_INVOICE, true);
        return new Receipt("BK-" + resident.profileCode(), pdf.toBytes());
    }

    private static void group(
            PdfDocument pdf, String title, List<BalanceTransactionRow> rows, Set<TransactionType> types) {
        List<BalanceTransactionRow> mine =
                rows.stream().filter(r -> types.contains(r.type())).toList();
        if (mine.isEmpty()) return;
        pdf.heading(title);
        pdf.table(
                List.of("Thời điểm", "Sổ", "Loại", "Số tiền (đồng)"),
                mine.stream()
                        .map(r -> List.of(
                                dateTime(r.at()),
                                r.ledger() == Ledger.DEPOSIT ? "Tiền cọc" : "Số dư",
                                DocumentFormat.transactionType(r.type()),
                                money(r.amount())))
                        .toList(),
                new float[] {3, 2, 4, 3});
        pdf.paragraph("Cộng " + title.toLowerCase(Locale.ROOT) + ": "
                + money(mine.stream().mapToLong(BalanceTransactionRow::amount).sum()) + " đồng");
    }

    private byte[] refundPdf(String number, BalanceTransactionRow row) {
        IdentityView resident = residents.identity(row.residentId());
        long amount = -row.amount();
        String json = recipients.recipient(row.id());
        JsonNode recipient = json == null ? null : JSON.readTree(json);
        String name = "Không ghi";
        String identity = null;
        if (recipient != null && recipient.has("familyMemberId")) {
            long member = recipient.get("familyMemberId").asLong();
            FamilyRelationshipApi api = relationships.getIfAvailable();
            name = api == null ? name : api.familyMemberName(member).orElse(name);
        } else if (recipient != null && recipient.has("fullName")) {
            name = recipient.get("fullName").asString();
            identity = recipient.has("identityDocument")
                    ? recipient.get("identityDocument").asString()
                    : null;
        }
        String paidBy = row.createdBy() == null ? null : queries.names().apply(row.createdBy());
        String approvedBy = row.approvedBy() == null ? null : queries.names().apply(row.approvedBy());
        PdfDocument pdf = PdfDocument.a4(facility(), null, "BIÊN NHẬN HOÀN TIỀN");
        pdf.fields(
                2,
                "Số",
                number,
                "Thời điểm lập",
                dateTime(row.at()),
                "Người cao tuổi",
                resident.fullName(),
                "Mã hồ sơ",
                resident.profileCode(),
                "Người nhận",
                name,
                "Hình thức",
                row.method() == PaymentMethod.CASH
                        ? "Tiền mặt"
                        : row.method() == PaymentMethod.BANK_TRANSFER ? "Chuyển khoản" : "Khác");
        if (identity != null) pdf.paragraph("Giấy tờ người nhận: " + identity);
        pdf.table(
                List.of("STT", "Nội dung", "Số tiền (đồng)"),
                List.of(List.of(
                        "1",
                        row.type() == TransactionType.DEPOSIT_REFUND ? "Hoàn tiền đặt cọc" : "Hoàn số dư còn lại",
                        money(amount))),
                new float[] {1, 7, 3});
        pdf.paragraph("Tổng cộng: " + money(amount) + " đồng");
        pdf.paragraph("Bằng chữ: " + AmountInWords.dong(amount) + ".");
        if (row.reason() != null) pdf.paragraph("Lý do: " + row.reason());
        if (approvedBy != null) pdf.paragraph("Người duyệt: " + approvedBy);
        pdf.signatures("Người nhận tiền", paidBy == null ? "Người chi tiền" : "Người chi tiền: " + paidBy);
        pdf.note(NOT_AN_INVOICE, true);
        return pdf.toBytes();
    }

    private FacilityInfo facility() {
        var value = parameters.read(ParameterCode.CFG_M11_11);
        return value.isSet() ? value.asTable(FacilityInfo.class) : null;
    }

    private byte[] pdf(String number, BalanceTransactionRow row) {
        IdentityView resident = residents.identity(row.residentId());
        String recordedBy = row.createdBy() == null ? null : queries.names().apply(row.createdBy());
        PdfDocument pdf = PdfDocument.a4(facility(), null, "BIÊN NHẬN NỘP TIỀN");
        pdf.fields(
                2,
                "Số",
                number,
                "Thời điểm nộp",
                dateTime(row.at()),
                "Người cao tuổi",
                resident.fullName(),
                "Mã hồ sơ",
                resident.profileCode(),
                "Người nộp",
                row.payerName() == null ? "Không ghi" : row.payerName(),
                "Hình thức",
                row.method() == PaymentMethod.CASH ? "Tiền mặt" : "Chuyển khoản");
        if (row.bankTransactionCode() != null) {
            pdf.paragraph("Mã giao dịch ngân hàng: " + row.bankTransactionCode());
        }
        String content = row.ledger() == Ledger.DEPOSIT
                ? "Tiền đặt cọc lưu trú"
                : "Nộp vào số dư để thanh toán chi phí lưu trú, dịch vụ";
        pdf.table(
                List.of("STT", "Nội dung", "Số tiền (đồng)"),
                List.of(List.of("1", content, money(row.amount()))),
                new float[] {1, 7, 3});
        pdf.paragraph("Tổng cộng: " + money(row.amount()) + " đồng");
        pdf.paragraph("Bằng chữ: " + AmountInWords.dong(row.amount()) + ".");
        if (row.note() != null && !row.note().isBlank()) {
            pdf.paragraph("Ghi chú: " + row.note());
        }
        if (row.adjusted()) {
            pdf.paragraph("Giao dịch này đã có điều chỉnh; xem lịch sử giao dịch để biết số hiện hành.");
        }
        pdf.signatures("Người nộp tiền", recordedBy == null ? "Người thu tiền" : "Người thu tiền: " + recordedBy);
        pdf.note(NOT_AN_INVOICE, true);
        return pdf.toBytes();
    }

    private BalanceTransactionRow require(long id) {
        return transactions.find(id).orElseThrow(NotFoundException::new);
    }
}
