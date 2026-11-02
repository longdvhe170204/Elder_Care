package com.eldercare.backend.admission.api;

import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.admission.api.dto.DepositAmountView;
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

    /** HopDongDocApi.lichDenTaiNgay: 005 điểm danh, 008 nhân lực ca, 011 suất ăn; không trả trường tài chính. */
    List<AttendanceScheduleView> attendanceOn(long residentId, LocalDate date);
}
