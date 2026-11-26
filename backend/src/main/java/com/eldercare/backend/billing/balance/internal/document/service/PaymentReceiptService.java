package com.eldercare.backend.billing.balance.internal.document.service;

import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.dateTime;
import static com.eldercare.backend.billing.balance.internal.document.domain.DocumentFormat.money;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.billing.balance.internal.document.domain.AmountInWords;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.document.PdfDocument;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.IdentityView;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Biên nhận nộp tiền (Q-451, góp ý demo BF-10): dựng PDF khi tải từ giao dịch nộp đã xác nhận (giao dịch không sửa được);
 * số biên nhận theo mã giao dịch; ghi rõ không thay hóa đơn. Nhân viên: Kế toán, Quản lý viện; người thân: quyền xem chi
 * phí đang bật.
 */
@Service
public class PaymentReceiptService {

    private static final Set<TransactionType> PAYMENTS = Set.of(
            TransactionType.CASH_PAYMENT, TransactionType.BANK_TRANSFER_PAYMENT, TransactionType.DEPOSIT_RECEIPT);
    private static final Set<Role> STAFF_VIEW = Set.of(Role.ACCOUNTANT, Role.NURSING_HOME_MANAGER);

    private final BalanceTransactionStore transactions;
    private final BalanceQueries queries;
    private final FamilyCostsRight familyRight;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final CurrentUser user;

    PaymentReceiptService(
            BalanceTransactionStore transactions,
            BalanceQueries queries,
            FamilyCostsRight familyRight,
            ResidentApi residents,
            ParameterApi parameters,
            CurrentUser user) {
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
        if (!PAYMENTS.contains(row.type()) || row.status() != TransactionStatus.CONFIRMED || row.amount() <= 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "RECEIPT_NOT_AVAILABLE", "Chỉ khoản nộp tiền đã xác nhận mới có biên nhận");
        }
        String number = String.format("BN-%06d", row.id());
        return new Receipt(number, pdf(number, row));
    }

    private byte[] pdf(String number, BalanceTransactionRow row) {
        IdentityView resident = residents.identity(row.residentId());
        var value = parameters.read(ParameterCode.CFG_M11_11);
        FacilityInfo facility = value.isSet() ? value.asTable(FacilityInfo.class) : null;
        String recordedBy = row.createdBy() == null ? null : queries.names().apply(row.createdBy());
        PdfDocument pdf = PdfDocument.a4(facility, null, "BIÊN NHẬN NỘP TIỀN");
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
                java.util.List.of("STT", "Nội dung", "Số tiền (đồng)"),
                java.util.List.of(java.util.List.of("1", content, money(row.amount()))),
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
        pdf.note("Chứng từ này không thay thế hóa đơn.", true);
        return pdf.toBytes();
    }

    private BalanceTransactionRow require(long id) {
        return transactions.find(id).orElseThrow(NotFoundException::new);
    }
}
