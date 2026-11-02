package com.eldercare.backend.admission.internal.contract.domain;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Nội dung sửa được của hợp đồng (api-004 {@code ContractInput}; FR-019): mọi cột "nội dung" của bảng {@code contract} và
 * dòng {@code DRAFT} của {@code contract_version}. Cùng nội dung này được chụp thành phiên bản 0 lúc ký (FR-024) và băm để gắn
 * với bản duyệt điều khoản khác chuẩn (FR-025a). Q-395 bỏ ghi đè chính sách vắng theo hợp đồng.
 */
public record ContractContent(
        ResidenceType residenceType,
        LocalDate startDate,
        LocalDate endDate,
        CareLevel careLevel,
        Long roomTypeId,
        Long bedId,
        BillingPeriod residencyFeeBillingPeriod,
        List<ServiceLine> services,
        boolean noDepositRequired,
        BigDecimal depositAmount,
        String bedHoldPolicy,
        List<ArrivalLine> arrivalSchedule,
        String terminationCondition) {

    /** Cùng nội dung nhưng với các dòng con đã tải từ bảng con. */
    public ContractContent withLines(List<ServiceLine> services, List<ArrivalLine> arrivalSchedule) {
        return new ContractContent(
                residenceType,
                startDate,
                endDate,
                careLevel,
                roomTypeId,
                bedId,
                residencyFeeBillingPeriod,
                services,
                noDepositRequired,
                depositAmount,
                bedHoldPolicy,
                arrivalSchedule,
                terminationCondition);
    }

    /** Dịch vụ đăng ký; {@code customUnitPrice} rỗng là dùng đơn giá chuẩn tại ngày bắt đầu. */
    public record ServiceLine(long serviceId, boolean inPackage, BigDecimal quantity, BigDecimal customUnitPrice) {}

    /** Lịch đến bán trú: đúng một trong {@code dayOfWeek} (1 = thứ hai) hoặc {@code date}. */
    public record ArrivalLine(Integer dayOfWeek, LocalDate date, LocalTime arrivalTime, LocalTime departureTime) {}
}
