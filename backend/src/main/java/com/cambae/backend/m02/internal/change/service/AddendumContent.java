package com.cambae.backend.m02.internal.change.service;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.change.domain.ChangeType;
import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent;
import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent.ArrivalChange;
import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent.ServiceChange;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Áp một yêu cầu thay đổi lưu trú lên nội dung phiên bản hiện hành của hợp đồng (FR-024, FR-042; research T-06): trả nội
 * dung mới (cùng dạng JSON với {@code ContractSnapshot}) và hai bản chỉ gồm các thuộc tính bị đổi cho phụ lục ({@code
 * contentBefore}, {@code contentAfter}). Hàm thuần: giá và dịch vụ chăm sóc tra qua {@link Pricing}.
 */
final class AddendumContent {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    /** Tra cứu phụ thuộc danh mục dịch vụ, tách khỏi cơ sở dữ liệu để dùng cả lúc duyệt (chỉ đọc) và lúc áp dụng. */
    interface Pricing {

        /** Dịch vụ chăm sóc Hiệu lực của mức (Q-297). */
        Optional<Long> careServiceOf(CareLevel level);

        /** Đơn giá chuẩn theo ngày hiệu lực của phụ lục; rỗng khi chưa có. */
        Optional<BigDecimal> unitPrice(long serviceId);

        /** Dịch vụ thuộc loại chăm sóc. */
        boolean isCareService(long serviceId);
    }

    record Built(ObjectNode content, ObjectNode before, ObjectNode after) {}

    private AddendumContent() {}

    static Built build(JsonNode base, ResidencyChangeContent change, Pricing pricing) {
        ObjectNode content = (ObjectNode) base.deepCopy();
        ObjectNode before = F.objectNode();
        ObjectNode after = F.objectNode();
        ResidenceType baseType = ResidenceType.valueOf(base.get("residenceType").asString());
        ResidenceType resultType = change.residenceType() != null ? change.residenceType() : baseType;

        if (change.has(ChangeType.RESIDENCE_TYPE)) {
            replace(content, before, after, "residenceType", F.stringNode(resultType.name()));
            if (resultType == ResidenceType.SHORT_TERM_RESIDENTIAL && change.endDate() != null) {
                replace(
                        content,
                        before,
                        after,
                        "endDate",
                        F.stringNode(change.endDate().toString()));
            }
            if (resultType == ResidenceType.DAY_CARE) {
                replace(content, before, after, "roomTypeId", F.nullNode());
                replace(content, before, after, "bedId", F.nullNode());
                if (!change.arrivalSchedule().isEmpty()) {
                    replace(content, before, after, "arrivalSchedule", schedule(change.arrivalSchedule()));
                }
            } else if (baseType == ResidenceType.DAY_CARE) {
                if (change.newBedId() != null) {
                    replace(content, before, after, "bedId", F.numberNode(change.newBedId()));
                }
                replace(content, before, after, "arrivalSchedule", F.arrayNode());
            }
        }
        if (change.has(ChangeType.DURATION)) {
            replace(
                    content,
                    before,
                    after,
                    "endDate",
                    F.stringNode(change.endDate().toString()));
        }
        if (change.has(ChangeType.BED)) {
            replace(content, before, after, "bedId", F.numberNode(change.newBedId()));
        }
        if (change.has(ChangeType.ARRIVAL_SCHEDULE)) {
            replace(content, before, after, "arrivalSchedule", schedule(change.arrivalSchedule()));
        }
        // Khoản cọc mới đi kèm: nêu số tiền mà không nêu cờ thì hợp đồng nay yêu cầu cọc
        Boolean noDeposit = change.noDepositRequired() != null
                ? change.noDepositRequired()
                : change.depositAmount() != null ? Boolean.FALSE : null;
        if (noDeposit != null) {
            replace(content, before, after, "noDepositRequired", F.booleanNode(noDeposit));
        }
        if (change.depositAmount() != null) {
            replace(content, before, after, "depositAmount", F.numberNode(change.depositAmount()));
        } else if (Boolean.TRUE.equals(change.noDepositRequired())) {
            replace(content, before, after, "depositAmount", F.nullNode());
        }
        if (change.has(ChangeType.CARE_LEVEL) || change.has(ChangeType.SERVICE)) {
            services(base, content, before, after, change, pricing);
        }
        return new Built(content, before, after);
    }

    private static void services(
            JsonNode base,
            ObjectNode content,
            ObjectNode before,
            ObjectNode after,
            ResidencyChangeContent change,
            Pricing pricing) {
        List<ObjectNode> lines = new ArrayList<>();
        for (JsonNode line : base.get("services")) {
            lines.add((ObjectNode) line.deepCopy());
        }
        ArrayNode removed = F.arrayNode();
        ArrayNode added = F.arrayNode();
        if (change.has(ChangeType.CARE_LEVEL)) {
            replace(
                    content,
                    before,
                    after,
                    "careLevel",
                    F.stringNode(change.careLevel().name()));
            for (ObjectNode line : new ArrayList<>(lines)) {
                if (pricing.isCareService(line.get("serviceId").asLong())) {
                    lines.remove(line);
                    removed.add(describe(line));
                }
            }
            long careService = pricing.careServiceOf(change.careLevel()).orElseThrow();
            ObjectNode care = F.objectNode();
            care.put("serviceId", careService);
            care.put("inPackage", true);
            care.put("quantity", 1);
            care.set("customUnitPrice", F.nullNode());
            care.set("recordedUnitPrice", price(pricing.unitPrice(careService)));
            lines.add(care);
            added.add(describe(care));
        }
        if (change.has(ChangeType.SERVICE)) {
            Set<Long> removedIds = new HashSet<>();
            for (ServiceChange service : change.services()) {
                if (service.action() == ServiceChange.Action.REMOVE) {
                    removedIds.add(service.serviceId());
                }
            }
            for (ObjectNode line : new ArrayList<>(lines)) {
                if (removedIds.contains(line.get("serviceId").asLong())) {
                    lines.remove(line);
                    removed.add(describe(line));
                }
            }
            for (ServiceChange service : change.services()) {
                if (service.action() != ServiceChange.Action.ADD) {
                    continue;
                }
                ObjectNode line = F.objectNode();
                line.put("serviceId", service.serviceId());
                line.put("inPackage", service.inPackage());
                line.set(
                        "quantity",
                        F.numberNode(BigDecimal.valueOf(service.quantity()).stripTrailingZeros()));
                line.set(
                        "customUnitPrice",
                        service.customUnitPrice() == null ? F.nullNode() : F.numberNode(service.customUnitPrice()));
                line.set(
                        "recordedUnitPrice",
                        service.customUnitPrice() != null
                                ? F.numberNode(service.customUnitPrice())
                                : price(pricing.unitPrice(service.serviceId())));
                lines.add(line);
                added.add(describe(line));
            }
        }
        lines.sort(Comparator.comparingLong(line -> line.get("serviceId").asLong()));
        ArrayNode array = F.arrayNode();
        lines.forEach(array::add);
        content.set("services", array);
        before.set("services", removed);
        after.set("services", added);
    }

    /** Dòng dịch vụ cho phụ lục: không kèm đơn giá ghi vì giá chốt ở nội dung phiên bản, không ở diff. */
    private static ObjectNode describe(JsonNode line) {
        ObjectNode node = F.objectNode();
        node.put("serviceId", line.get("serviceId").asLong());
        node.put("inPackage", line.get("inPackage").asBoolean());
        node.set("quantity", line.get("quantity"));
        node.set("customUnitPrice", line.has("customUnitPrice") ? line.get("customUnitPrice") : F.nullNode());
        return node;
    }

    private static JsonNode price(Optional<BigDecimal> price) {
        if (price.isEmpty()) {
            return F.nullNode();
        }
        BigDecimal stripped = price.get().stripTrailingZeros();
        return stripped.scale() <= 0 ? F.numberNode(stripped.toBigInteger()) : F.numberNode(stripped);
    }

    private static ArrayNode schedule(List<ArrivalChange> lines) {
        ArrayNode array = F.arrayNode();
        lines.stream()
                .sorted(Comparator.comparing((ArrivalChange l) -> l.dayOfWeek() == null ? 0 : l.dayOfWeek())
                        .thenComparing(l -> l.date() == null ? "" : l.date().toString())
                        .thenComparing(ArrivalChange::arrivalTime))
                .forEach(line -> {
                    ObjectNode node = array.addObject();
                    node.set("dayOfWeek", line.dayOfWeek() == null ? F.nullNode() : F.numberNode(line.dayOfWeek()));
                    node.set(
                            "date",
                            line.date() == null
                                    ? F.nullNode()
                                    : F.stringNode(line.date().toString()));
                    node.put("arrivalTime", line.arrivalTime().toString());
                    node.put("departureTime", line.departureTime().toString());
                });
        return array;
    }

    /** Đặt giá trị mới; ghi giá trị cũ và mới vào hai bản diff khi khác nhau (lần đặt đầu của khóa giữ giá trị cũ ban đầu). */
    private static void replace(ObjectNode content, ObjectNode before, ObjectNode after, String key, JsonNode value) {
        JsonNode old = content.has(key) ? content.get(key) : F.nullNode();
        if (same(old, value)) {
            return;
        }
        if (!before.has(key)) {
            before.set(key, old);
        }
        after.set(key, value);
        content.set(key, value);
    }

    /** Số so theo giá trị, vì nút số nguyên 32 và 64 bit của Jackson không bằng nhau dù cùng giá trị. */
    private static boolean same(JsonNode a, JsonNode b) {
        return a.isNumber() && b.isNumber() ? a.decimalValue().compareTo(b.decimalValue()) == 0 : a.equals(b);
    }
}
