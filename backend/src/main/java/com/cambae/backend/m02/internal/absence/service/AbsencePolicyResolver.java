package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.Attendance;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import java.math.BigDecimal;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Tra chính sách phí khi vắng (BR-M02-06, FR-050 → FR-053, FR-056, FR-060; research T-08), hàm thuần. Thứ tự: dòng của hợp
 * đồng (loại lưu trú, loại vắng, khoảng ngày), rồi dòng bảng cơ sở CFG-M02-05. Dòng cơ sở "Theo hợp đồng" mà hợp đồng
 * không có dòng thì áp FR-053: 100%, giữ giường, dấu "không có dòng chính sách". Dòng "Quản lý quyết định" lấy hệ số của
 * dòng liền trước (100% nếu là ngày 1, BR-M02-17) kèm dấu "chờ quyết định".
 *
 * <p>Bảng viết loại vắng nhóm {@code HOME_OR_OUTING} cho Về nhà, Đi chơi với gia đình; {@code ANY} cho mọi loại. Đi khám
 * trong ngày, Lý do khác chỉ có dòng khi bảng hoặc hợp đồng nêu riêng hoặc dùng {@code ANY} (6.7).
 */
final class AbsencePolicyResolver {

    enum Source {
        CONTRACT,
        FACILITY,
        DEFAULT
    }

    enum Hold {
        YES,
        NO,
        MANAGER_DECIDES
    }

    /** {@code line} là mô tả dòng đã áp (JSON văn bản) để giải trình trên ngày vắng. */
    record Outcome(
            BigDecimal coefficient,
            Hold hold,
            Source source,
            String line,
            boolean pendingDecision,
            boolean noPolicyLine) {

        /** Giường đang được giữ theo dòng này; "Quản lý quyết định" vẫn giữ tạm tới khi có quyết định (FR-056). */
        boolean holdBed() {
            return hold != Hold.NO;
        }
    }

    private static final BigDecimal FULL = new BigDecimal("100");
    private static final String MANAGER_DECIDES = "MANAGER_DECIDES";
    private static final String BY_CONTRACT = "BY_CONTRACT";
    private static final String PER_CONTRACT = "PER_CONTRACT";

    private AbsencePolicyResolver() {}

    /**
     * Ngày vắng thứ {@code sequenceNo} của lượt Tạm vắng hoặc Bệnh viện.
     *
     * @param previousCoefficient hệ số của ngày liền trước, rỗng ở ngày đầu
     */
    static Outcome resolve(
            ResidenceType residence,
            AbsenceType type,
            int sequenceNo,
            List<AbsencePolicyLine> contractLines,
            JsonNode table,
            BigDecimal previousCoefficient) {
        for (AbsencePolicyLine line : contractLines) {
            if (line.residenceType() == residence
                    && matches(line.absenceType(), type)
                    && inRange(line.fromDay(), line.toDay(), sequenceNo)
                    && !PER_CONTRACT.equals(line.coefficient())) {
                return build(line.coefficient(), line.holdBed(), Source.CONTRACT, describe(line), previousCoefficient);
            }
        }
        JsonNode row = facilityRow(residence, type, sequenceNo, table);
        if (row != null) {
            String coefficient = text(row.get("feeRatePercent"));
            String hold = text(row.get("holdBed"));
            if (!BY_CONTRACT.equals(coefficient) && !BY_CONTRACT.equals(hold)) {
                return build(coefficient, hold, Source.FACILITY, row.toString(), previousCoefficient);
            }
        }
        return noLine();
    }

    /**
     * Hệ số của một buổi bán trú theo trạng thái có mặt (FR-060): Có mặt 100%; Vắng có báo, Vắng không báo tra dòng bán trú
     * của hợp đồng rồi của bảng cơ sở. Không có dòng thì 100% kèm dấu.
     */
    static Outcome dayCare(Attendance attendance, List<AbsencePolicyLine> contractLines, JsonNode table) {
        if (attendance == Attendance.PRESENT) {
            return new Outcome(FULL, Hold.YES, Source.DEFAULT, null, false, false);
        }
        String key = attendance == Attendance.ABSENT_NOTIFIED ? "NOTIFIED_IN_ADVANCE" : "UNANNOUNCED";
        for (AbsencePolicyLine line : contractLines) {
            if (line.residenceType() == ResidenceType.DAY_CARE && key.equals(line.absenceType())) {
                return build(line.coefficient(), null, Source.CONTRACT, describe(line), null);
            }
        }
        JsonNode rows = table == null ? null : table.get("rows");
        if (rows != null) {
            for (JsonNode row : rows) {
                if ("DAY_CARE".equals(text(row.get("residencyType"))) && key.equals(text(row.get("absenceType")))) {
                    return build(text(row.get("feeRatePercent")), null, Source.FACILITY, row.toString(), null);
                }
            }
        }
        return noLine();
    }

    private static Outcome build(
            String coefficient, String hold, Source source, String line, BigDecimal previousCoefficient) {
        boolean pending = MANAGER_DECIDES.equals(coefficient);
        BigDecimal value =
                pending ? (previousCoefficient == null ? FULL : previousCoefficient) : new BigDecimal(coefficient);
        return new Outcome(value, hold(hold), source, line, pending, false);
    }

    /** Không có dòng chính sách (FR-053): 100%, giữ giường, kèm dấu. */
    static Outcome noLine() {
        return new Outcome(FULL, Hold.YES, Source.DEFAULT, null, false, true);
    }

    private static Hold hold(String value) {
        if ("NO".equals(value)) {
            return Hold.NO;
        }
        return MANAGER_DECIDES.equals(value) ? Hold.MANAGER_DECIDES : Hold.YES;
    }

    private static JsonNode facilityRow(ResidenceType residence, AbsenceType type, int sequenceNo, JsonNode table) {
        JsonNode rows = table == null ? null : table.get("rows");
        if (rows == null) {
            return null;
        }
        String residency = residency(residence);
        for (JsonNode row : rows) {
            if (residency.equals(text(row.get("residencyType")))
                    && matches(text(row.get("absenceType")), type)
                    && inRange(integer(row.get("fromDay")), integer(row.get("toDay")), sequenceNo)) {
                return row;
            }
        }
        return null;
    }

    private static boolean matches(String lineType, AbsenceType type) {
        if (lineType == null) {
            return false;
        }
        return lineType.equals(type.name())
                || "ANY".equals(lineType)
                || ("HOME_OR_OUTING".equals(lineType)
                        && (type == AbsenceType.HOME_VISIT || type == AbsenceType.FAMILY_OUTING));
    }

    private static boolean inRange(Integer from, Integer to, int day) {
        return (from == null || day >= from) && (to == null || day <= to);
    }

    private static String residency(ResidenceType type) {
        return switch (type) {
            case LONG_TERM_RESIDENTIAL -> "LONG_TERM";
            case SHORT_TERM_RESIDENTIAL -> "SHORT_TERM";
            case DAY_CARE -> "DAY_CARE";
        };
    }

    private static String describe(AbsencePolicyLine line) {
        return "{\"residencyType\":\"" + residency(line.residenceType()) + "\",\"absenceType\":\"" + line.absenceType()
                + "\",\"fromDay\":" + line.fromDay() + ",\"toDay\":" + line.toDay() + ",\"feeRatePercent\":\""
                + line.coefficient() + "\",\"holdBed\":"
                + (line.holdBed() == null ? "null" : "\"" + line.holdBed() + "\"")
                + "}";
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }

    private static Integer integer(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }
}
