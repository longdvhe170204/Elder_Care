package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.contract.domain.ContractContent;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ArrivalLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ServiceLine;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kiểm tra nội dung hợp đồng khi lập và sửa (FR-019, FR-050, Q-286; hàm thuần): sai thì 422. Kiểm dịch vụ có tồn tại
 * thuộc dịch vụ, không ở đây.
 */
final class ContractRules {

    private static final Set<String> HOLD_BED = Set.of("YES", "NO", "PER_CONTRACT", "MANAGER_DECIDES");
    private static final Set<String> COEFFICIENT_WORDS = Set.of("PER_CONTRACT", "MANAGER_DECIDES");

    private ContractRules() {}

    static void validate(ContractContent content) {
        if (content.residenceType() == ResidenceType.SHORT_TERM_RESIDENTIAL && content.endDate() == null) {
            throw new InvalidValueException("endDate là bắt buộc với nội trú ngắn ngày (FR-019)");
        }
        if (content.endDate() != null && content.endDate().isBefore(content.startDate())) {
            throw new InvalidValueException("endDate không được sớm hơn startDate");
        }
        if (content.noDepositRequired() && content.depositAmount() != null) {
            throw new InvalidValueException("Không thể vừa không yêu cầu đặt cọc vừa có khoản cần đặt cọc (FR-030)");
        }
        if (content.depositAmount() != null && content.depositAmount().signum() < 0) {
            throw new InvalidValueException("depositAmount không được âm");
        }
        services(content.services());
        arrival(content);
        policies(content.absencePolicyOverrides());
    }

    private static void services(List<ServiceLine> lines) {
        Set<Long> seen = new HashSet<>();
        for (ServiceLine line : lines) {
            if (!seen.add(line.serviceId())) {
                throw new InvalidValueException("Dịch vụ " + line.serviceId() + " xuất hiện hai lần");
            }
            if (line.quantity().signum() <= 0) {
                throw new InvalidValueException("quantity của dịch vụ " + line.serviceId() + " phải lớn hơn 0");
            }
            if (line.customUnitPrice() != null && line.customUnitPrice().signum() < 0) {
                throw new InvalidValueException("customUnitPrice của dịch vụ " + line.serviceId() + " không được âm");
            }
        }
    }

    private static void arrival(ContractContent content) {
        if (!content.arrivalSchedule().isEmpty() && content.residenceType() != ResidenceType.DAY_CARE) {
            throw new InvalidValueException("Lịch đến chỉ áp dụng cho bán trú (FR-019)");
        }
        for (ArrivalLine line : content.arrivalSchedule()) {
            if ((line.dayOfWeek() == null) == (line.date() == null)) {
                throw new InvalidValueException("Mỗi lịch đến cần đúng một trong dayOfWeek hoặc date");
            }
            if (line.dayOfWeek() != null && (line.dayOfWeek() < 1 || line.dayOfWeek() > 7)) {
                throw new InvalidValueException("dayOfWeek phải từ 1 đến 7");
            }
            if (!line.departureTime().isAfter(line.arrivalTime())) {
                throw new InvalidValueException("Giờ về phải sau giờ đến trong cùng một ngày, không qua 00:00 (Q-286)");
            }
        }
    }

    private static void policies(List<AbsencePolicyLine> lines) {
        for (int i = 0; i < lines.size(); i++) {
            AbsencePolicyLine line = lines.get(i);
            coefficient(line.coefficient());
            if (line.holdBed() != null && !HOLD_BED.contains(line.holdBed())) {
                throw new InvalidValueException("holdBed phải là một trong " + HOLD_BED);
            }
            if (line.fromDay() != null && line.toDay() != null && line.toDay() < line.fromDay()) {
                throw new InvalidValueException("toDay không được nhỏ hơn fromDay");
            }
            for (int j = i + 1; j < lines.size(); j++) {
                AbsencePolicyLine other = lines.get(j);
                if (line.residenceType() == other.residenceType()
                        && line.absenceType().equals(other.absenceType())
                        && overlaps(line, other)) {
                    throw new InvalidValueException("Hai dòng chính sách vắng " + line.residenceType() + " / "
                            + line.absenceType() + " chồng khoảng ngày (FR-050)");
                }
            }
        }
    }

    private static void coefficient(String coefficient) {
        if (COEFFICIENT_WORDS.contains(coefficient)) {
            return;
        }
        try {
            if (new BigDecimal(coefficient).signum() >= 0) {
                return;
            }
        } catch (NumberFormatException e) {
            // rơi xuống lỗi chung
        }
        throw new InvalidValueException("coefficient là số phần trăm không âm, PER_CONTRACT hoặc MANAGER_DECIDES");
    }

    private static boolean overlaps(AbsencePolicyLine a, AbsencePolicyLine b) {
        long aFrom = a.fromDay() == null ? 0 : a.fromDay();
        long aTo = a.toDay() == null ? Long.MAX_VALUE : a.toDay();
        long bFrom = b.fromDay() == null ? 0 : b.fromDay();
        long bTo = b.toDay() == null ? Long.MAX_VALUE : b.toDay();
        return aFrom <= bTo && bFrom <= aTo;
    }
}
