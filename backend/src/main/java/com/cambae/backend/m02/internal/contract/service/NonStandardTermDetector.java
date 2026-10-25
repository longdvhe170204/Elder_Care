package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.contract.domain.ContractContent;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ServiceLine;
import com.cambae.backend.m02.internal.contract.domain.NonStandardTerm;
import com.cambae.backend.m02.internal.contract.domain.NonStandardTerm.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongFunction;
import tools.jackson.databind.JsonNode;

/**
 * Xác định điều khoản khác chuẩn của hợp đồng Nháp (FR-025a; research T-04; hàm thuần): giá riêng khác phiên bản đơn giá
 * tại ngày bắt đầu (FR-036); dòng ghi đè chính sách phí khi vắng hoặc giữ giường khác bảng CFG-M02-05 (FR-051). Một dòng
 * ghi đè khác chuẩn ở cả tỷ lệ phí lẫn giữ giường cho hai điều khoản. Dòng không khớp dòng nào của bảng (loại lưu trú, loại
 * vắng, khoảng ngày) là điều khoản chính sách vắng không có giá trị chuẩn để so. Trường {@code bedHoldPolicy} dạng văn bản
 * chỉ mô tả, không so với bảng.
 */
final class NonStandardTermDetector {

    private NonStandardTermDetector() {}

    /**
     * @param standardPrice đơn giá chuẩn của dịch vụ tại ngày bắt đầu hợp đồng, rỗng nếu chưa có phiên bản hiệu lực
     * @param absenceTable giá trị CFG-M02-05 dạng {@code {"rows":[...]}}
     */
    static List<NonStandardTerm> detect(
            ContractContent content, LongFunction<Optional<BigDecimal>> standardPrice, JsonNode absenceTable) {
        List<NonStandardTerm> terms = new ArrayList<>();
        for (ServiceLine line : content.services()) {
            if (line.customUnitPrice() == null) {
                continue;
            }
            Optional<BigDecimal> standard = standardPrice.apply(line.serviceId());
            if (standard.isPresent() && standard.get().compareTo(line.customUnitPrice()) == 0) {
                continue;
            }
            terms.add(new NonStandardTerm(
                    Type.PRICE,
                    "Đơn giá riêng của dịch vụ " + line.serviceId(),
                    standard.map(NonStandardTermDetector::plain).orElse(null),
                    plain(line.customUnitPrice())));
        }
        for (AbsencePolicyLine line : content.absencePolicyOverrides()) {
            policyTerms(line, absenceTable, terms);
        }
        return terms;
    }

    private static void policyTerms(AbsencePolicyLine line, JsonNode table, List<NonStandardTerm> terms) {
        String description = "Chính sách vắng " + line.residenceType() + " / " + line.absenceType() + " từ ngày "
                + line.fromDay() + (line.toDay() == null ? " trở đi" : " đến ngày " + line.toDay());
        JsonNode row = matchingRow(line, table);
        if (row == null) {
            terms.add(new NonStandardTerm(Type.ABSENCE_POLICY, description, null, line.coefficient()));
            return;
        }
        String standardCoefficient = coefficient(row.get("feeRatePercent"));
        if (!Objects.equals(standardCoefficient, line.coefficient())) {
            terms.add(new NonStandardTerm(Type.ABSENCE_POLICY, description, standardCoefficient, line.coefficient()));
        }
        String standardHold = coefficient(row.get("holdBed"));
        if (!Objects.equals(standardHold, line.holdBed())) {
            terms.add(new NonStandardTerm(Type.BED_HOLD_POLICY, description, standardHold, line.holdBed()));
        }
    }

    private static JsonNode matchingRow(AbsencePolicyLine line, JsonNode table) {
        JsonNode rows = table == null ? null : table.get("rows");
        if (rows == null) {
            return null;
        }
        String residency = residency(line.residenceType());
        String absence = absence(line.absenceType());
        for (JsonNode row : rows) {
            String rowAbsence = text(row.get("absenceType"));
            if (residency.equals(text(row.get("residencyType")))
                    && (absence.equals(rowAbsence) || "ANY".equals(rowAbsence))
                    && Objects.equals(line.fromDay(), integer(row.get("fromDay")))
                    && Objects.equals(line.toDay(), integer(row.get("toDay")))) {
                return row;
            }
        }
        return null;
    }

    private static String residency(ResidenceType type) {
        return switch (type) {
            case LONG_TERM_RESIDENTIAL -> "LONG_TERM";
            case SHORT_TERM_RESIDENTIAL -> "SHORT_TERM";
            case DAY_CARE -> "DAY_CARE";
        };
    }

    /** Bốn loại vắng tạm gộp thành {@code HOME_OR_OUTING} trong bảng cơ sở (6.7). */
    private static String absence(String absenceType) {
        return switch (absenceType) {
            case "HOME_VISIT", "FAMILY_OUTING", "SAME_DAY_MEDICAL_VISIT", "OTHER" -> "HOME_OR_OUTING";
            default -> absenceType;
        };
    }

    /** Bảng cơ sở viết {@code BY_CONTRACT}, hợp đồng viết {@code PER_CONTRACT}. */
    private static String coefficient(JsonNode node) {
        String value = text(node);
        return "BY_CONTRACT".equals(value) ? "PER_CONTRACT" : value;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }

    private static Integer integer(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
