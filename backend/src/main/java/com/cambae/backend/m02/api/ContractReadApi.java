package com.cambae.backend.m02.api;

import com.cambae.backend.m02.api.dto.ArrivalScheduleView;
import com.cambae.backend.m02.api.dto.ContractContentView;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.DepositAmountView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Đọc hợp đồng cho 003, 005, 010, 012, 017, 018 (tên logic {@code HopDongDocApi}; contracts mục 1.2). Chỉ đọc; trường
 * tài chính mang nhóm {@code FINANCE}.
 */
public interface ContractReadApi {

    /** FR-024: nội dung hiệu lực tại ngày, kèm đơn giá đã ghi; rỗng khi không có hợp đồng hiệu lực ngày đó. */
    Optional<ContractContentView> contentAtDate(long residentId, LocalDate date);

    Optional<ContractView> contract(long contractId);

    List<ContractView> ofResident(long residentId);

    /** 017 (FR-030): khoản đặt cọc cần có. */
    Optional<DepositAmountView> depositAmount(long contractId);

    /** 005 điểm danh, 011 suất ăn: lịch đến bán trú áp dụng tại ngày. */
    List<ArrivalScheduleView> arrivalScheduleAtDate(long residentId, LocalDate date);
}
