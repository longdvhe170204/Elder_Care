package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import java.time.Instant;

/**
 * Trạng thái đặt cọc của hợp đồng đổi (tên logic {@code TrangThaiDatCocDaDoi}; 017 FR-006): 017 phát sau commit, 004
 * nghe để kiểm lại điều kiện tiếp nhận và lưu trú. Khai ở {@code admission.api} vì billing đã gọi admission, 004 nghe
 * sự kiện ở {@code billing.api} sẽ thành vòng module. {@code transactionId}, {@code accountantId} rỗng khi đổi do khoản
 * cọc của hợp đồng đổi.
 */
public record DepositStatusChanged(
        long contractId, long residentId, DepositStatus status, Long transactionId, Long accountantId, Instant at) {}
