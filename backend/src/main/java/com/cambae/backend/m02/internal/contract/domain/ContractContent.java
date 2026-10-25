package com.cambae.backend.m02.internal.contract.domain;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Nội dung sửa được của hợp đồng (api-004 {@code ContractInput}; FR-019): mọi cột "nội dung" của bảng {@code contract} và
 * ba bảng con. Cùng nội dung này được chụp thành phiên bản 0 lúc ký (FR-024) và băm để gắn với bản duyệt điều khoản khác
 * chuẩn (FR-025a). {@code holdBed} của dòng chính sách vắng là {@code YES}, {@code NO}, {@code PER_CONTRACT} hoặc {@code
 * MANAGER_DECIDES}; {@code coefficient} là số phần trăm, {@code PER_CONTRACT} hoặc {@code MANAGER_DECIDES}.
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
        List<AbsencePolicyLine> absencePolicyOverrides,
        List<ArrivalLine> arrivalSchedule,
        String terminationCondition) {

    /** Cùng nội dung nhưng với các dòng con đã tải từ bảng con. */
    public ContractContent withLines(
            List<ServiceLine> services,
            List<AbsencePolicyLine> absencePolicyOverrides,
            List<ArrivalLine> arrivalSchedule) {
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
                absencePolicyOverrides,
                arrivalSchedule,
                terminationCondition);
    }

    /** Dịch vụ đăng ký; {@code customUnitPrice} rỗng là dùng đơn giá chuẩn tại ngày bắt đầu. */
    public record ServiceLine(long serviceId, boolean inPackage, BigDecimal quantity, BigDecimal customUnitPrice) {}

    public record AbsencePolicyLine(
            ResidenceType residenceType,
            String absenceType,
            Integer fromDay,
            Integer toDay,
            String coefficient,
            String holdBed) {}

    /** Lịch đến bán trú: đúng một trong {@code dayOfWeek} (1 = thứ hai) hoặc {@code date}. */
    public record ArrivalLine(Integer dayOfWeek, LocalDate date, LocalTime arrivalTime, LocalTime departureTime) {}
}
