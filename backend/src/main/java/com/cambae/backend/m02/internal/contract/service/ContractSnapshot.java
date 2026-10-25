package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.m02.internal.contract.domain.ContractContent;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ArrivalLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ServiceLine;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Chụp nội dung hợp đồng thành JSON chuẩn hóa (thứ tự khóa cố định, dòng con theo thứ tự cố định, số không có phần thừa):
 * cùng một nội dung luôn cho cùng một chuỗi, nên băm SHA-256 của nó gắn bản duyệt điều khoản khác chuẩn với đúng nội dung
 * đã gửi (FR-025a) và phiên bản nội dung lúc ký lưu lại đúng chuỗi này (FR-024, FR-036). Dạng JSON theo api-004 {@code
 * ContractInput}; mỗi dịch vụ có thêm {@code recordedUnitPrice} khi đã ký.
 */
final class ContractSnapshot {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private ContractSnapshot() {}

    /** {@code recordedPrices}: đơn giá đã ghi theo mã dịch vụ lúc ký, {@code null} khi chụp để băm. */
    static ObjectNode of(long residentId, ContractContent content, Map<Long, BigDecimal> recordedPrices) {
        ObjectNode node = F.objectNode();
        node.put("residentId", residentId);
        node.put("residenceType", content.residenceType().name());
        node.put("startDate", content.startDate().toString());
        put(
                node,
                "endDate",
                content.endDate() == null ? null : content.endDate().toString());
        node.put("careLevel", content.careLevel().name());
        put(node, "roomTypeId", content.roomTypeId());
        put(node, "bedId", content.bedId());
        node.put(
                "residencyFeeBillingPeriod", content.residencyFeeBillingPeriod().name());
        ArrayNode services = node.putArray("services");
        content.services().stream()
                .sorted(Comparator.comparingLong(ServiceLine::serviceId))
                .forEach(line -> services.add(service(line, recordedPrices)));
        node.put("noDepositRequired", content.noDepositRequired());
        node.set("depositAmount", number(content.depositAmount()));
        put(node, "bedHoldPolicy", content.bedHoldPolicy());
        ArrayNode policies = node.putArray("absencePolicyOverrides");
        content.absencePolicyOverrides().stream()
                .sorted(Comparator.comparing(
                                (AbsencePolicyLine l) -> l.residenceType().name())
                        .thenComparing(AbsencePolicyLine::absenceType)
                        .thenComparing(l -> l.fromDay() == null ? -1 : l.fromDay())
                        .thenComparing(l -> l.toDay() == null ? Integer.MAX_VALUE : l.toDay()))
                .forEach(line -> policies.add(policy(line)));
        ArrayNode schedule = node.putArray("arrivalSchedule");
        content.arrivalSchedule().stream()
                .sorted(Comparator.comparing((ArrivalLine l) -> l.dayOfWeek() == null ? 0 : l.dayOfWeek())
                        .thenComparing(l -> l.date() == null ? "" : l.date().toString())
                        .thenComparing(ArrivalLine::arrivalTime))
                .forEach(line -> schedule.add(arrival(line)));
        put(node, "terminationCondition", content.terminationCondition());
        return node;
    }

    /** SHA-256 dạng thập lục phân (64 ký tự) của bản chụp không có đơn giá đã ghi. */
    static String hash(long residentId, ContractContent content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(of(residentId, content, null).toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ObjectNode service(ServiceLine line, Map<Long, BigDecimal> recordedPrices) {
        ObjectNode node = F.objectNode();
        node.put("serviceId", line.serviceId());
        node.put("inPackage", line.inPackage());
        node.set("quantity", number(line.quantity()));
        node.set("customUnitPrice", number(line.customUnitPrice()));
        if (recordedPrices != null) {
            node.set("recordedUnitPrice", number(recordedPrices.get(line.serviceId())));
        }
        return node;
    }

    private static ObjectNode policy(AbsencePolicyLine line) {
        ObjectNode node = F.objectNode();
        node.put("residenceType", line.residenceType().name());
        node.put("absenceType", line.absenceType());
        put(node, "fromDay", line.fromDay() == null ? null : line.fromDay().longValue());
        put(node, "toDay", line.toDay() == null ? null : line.toDay().longValue());
        node.put("coefficient", line.coefficient());
        put(node, "holdBed", line.holdBed());
        return node;
    }

    private static ObjectNode arrival(ArrivalLine line) {
        ObjectNode node = F.objectNode();
        put(
                node,
                "dayOfWeek",
                line.dayOfWeek() == null ? null : line.dayOfWeek().longValue());
        put(node, "date", line.date() == null ? null : line.date().toString());
        node.put("arrivalTime", line.arrivalTime().toString());
        node.put("departureTime", line.departureTime().toString());
        return node;
    }

    private static void put(ObjectNode node, String name, String value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value);
        }
    }

    private static void put(ObjectNode node, String name, Long value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value);
        }
    }

    /** Số nguyên khi không có phần thập phân (tiền là {@code DECIMAL(15,0)}), ngược lại số thập phân bỏ số 0 thừa. */
    private static JsonNode number(BigDecimal value) {
        if (value == null) {
            return F.nullNode();
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() <= 0 ? F.numberNode(stripped.toBigInteger()) : F.numberNode(stripped);
    }
}
