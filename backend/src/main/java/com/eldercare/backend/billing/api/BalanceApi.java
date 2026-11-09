package com.eldercare.backend.billing.api;

import com.eldercare.backend.billing.api.dto.BalanceDocumentView;
import com.eldercare.backend.billing.api.dto.BalanceView;
import com.eldercare.backend.billing.api.dto.SettlementConditionsView;
import java.util.List;
import java.util.Optional;

/**
 * Đọc sổ số dư của người cao tuổi (017, tên logic {@code SoDuApi}; module-interfaces 017 mục 2.2). Không kiểm quyền:
 * module gọi tự kiểm quyền của mình trước khi hiện.
 */
public interface BalanceApi {

    /** Rỗng khi người cao tuổi chưa có sổ (hợp đồng đầu tiên chưa Chờ ký, FR-001). */
    Optional<BalanceView> balance(long residentId);

    /** FR-022: hai mã điều kiện kết thúc lưu trú dòng (h) kèm lý do và phiên bản bảng quyết toán hiện hành. */
    SettlementConditionsView settlementConditions(long residentId);

    /** FR-038: phiếu báo phí và bảng quyết toán hiện hành, mới nhất trước. */
    List<BalanceDocumentView> documents(long residentId);
}
