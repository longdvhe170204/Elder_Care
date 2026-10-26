package com.cambae.backend.m02.internal.change.domain;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Nội dung của yêu cầu {@code M02.RESIDENCY_CHANGE} (research T-06): {@code contractId}, {@code changeTypes}, {@code
 * newValue} theo loại, {@code reference} tùy chọn; hệ thống thêm {@code previousValue}, {@code costImpact} lúc lập. Lớp này
 * đọc và kiểm hình dạng (422); điều kiện nghiệp vụ lúc duyệt (FR-041) ở handler.
 *
 * <p>{@code services} là thay đổi theo dòng ({@code ADD} hoặc {@code REMOVE}), không phải danh sách đích, để hai phụ lục
 * dịch vụ nối tiếp không ghi đè nhau. {@code depositAmount}, {@code noDepositRequired} trong {@code newValue} là khoản cọc
 * mới đi kèm (FR-030 "kể cả khi phụ lục đổi khoản cọc").
 */
public record ResidencyChangeContent(
        long contractId,
        Set<ChangeType> changeTypes,
        ResidenceType residenceType,
        CareLevel careLevel,
        List<ServiceChange> services,
        LocalDate endDate,
        Long newBedId,
        List<ArrivalChange> arrivalSchedule,
        Boolean noDepositRequired,
        Long depositAmount,
        JsonNode reference) {

    public record ServiceChange(
            long serviceId, Action action, boolean inPackage, double quantity, Long customUnitPrice) {
        public enum Action {
            ADD,
            REMOVE
        }
    }

    public record ArrivalChange(Integer dayOfWeek, LocalDate date, LocalTime arrivalTime, LocalTime departureTime) {}

    public boolean has(ChangeType type) {
        return changeTypes.contains(type);
    }

    /** Có đổi khoản cọc đi kèm. */
    public boolean changesDeposit() {
        return noDepositRequired != null || depositAmount != null;
    }

    public static ResidencyChangeContent parse(JsonNode content) {
        if (content == null || !content.isObject()) {
            throw new InvalidValueException("Nội dung yêu cầu phải là object");
        }
        JsonNode contractNode = content.get("contractId");
        if (contractNode == null || !contractNode.isIntegralNumber() || contractNode.asLong() <= 0) {
            throw new InvalidValueException("Nội dung cần contractId là mã hợp đồng");
        }
        JsonNode typeNodes = content.get("changeTypes");
        if (typeNodes == null || !typeNodes.isArray() || typeNodes.isEmpty()) {
            throw new InvalidValueException("changeTypes cần ít nhất một loại thay đổi");
        }
        Set<ChangeType> types = EnumSet.noneOf(ChangeType.class);
        for (JsonNode node : typeNodes) {
            try {
                types.add(ChangeType.valueOf(node.asString()));
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException("changeTypes[] không hợp lệ: " + node.asString());
            }
        }
        JsonNode value = content.get("newValue");
        if (value == null || !value.isObject()) {
            throw new InvalidValueException("Nội dung cần newValue là object");
        }
        ResidenceType residenceType = null;
        CareLevel careLevel = null;
        List<ServiceChange> services = List.of();
        if (types.contains(ChangeType.RESIDENCE_TYPE)) {
            residenceType = enumOf(ResidenceType.class, value, "residenceType");
        }
        if (types.contains(ChangeType.CARE_LEVEL)) {
            careLevel = enumOf(CareLevel.class, value, "careLevel");
        }
        if (types.contains(ChangeType.SERVICE)) {
            services = services(value.get("services"));
        }
        LocalDate endDate = date(value, "endDate", types.contains(ChangeType.DURATION));
        Long bed = positive(value, "newBedId", types.contains(ChangeType.BED));
        List<ArrivalChange> schedule = List.of();
        if (types.contains(ChangeType.ARRIVAL_SCHEDULE) || value.hasNonNull("arrivalSchedule")) {
            schedule = schedule(value.get("arrivalSchedule"), types.contains(ChangeType.ARRIVAL_SCHEDULE));
        }
        Boolean noDeposit = null;
        if (value.has("noDepositRequired")) {
            if (!value.get("noDepositRequired").isBoolean()) {
                throw new InvalidValueException("noDepositRequired phải là true hoặc false");
            }
            noDeposit = value.get("noDepositRequired").asBoolean();
        }
        Long deposit = null;
        if (value.hasNonNull("depositAmount")) {
            JsonNode node = value.get("depositAmount");
            if (!node.isIntegralNumber() || node.asLong() < 0) {
                throw new InvalidValueException("depositAmount phải là số tiền nguyên không âm");
            }
            deposit = node.asLong();
        }
        if (Boolean.TRUE.equals(noDeposit) && deposit != null) {
            throw new InvalidValueException("Không thể vừa không yêu cầu cọc vừa nêu depositAmount");
        }
        JsonNode reference = content.get("reference");
        if (reference != null && !reference.isNull()) {
            String type = reference.path("type").asString("");
            if (!type.equals("ASSESSMENT") && !type.equals("BED_TRANSFER_ORDER")) {
                throw new InvalidValueException("reference.type phải là ASSESSMENT hoặc BED_TRANSFER_ORDER");
            }
            if (!reference.path("id").isIntegralNumber() || reference.path("id").asLong() <= 0) {
                throw new InvalidValueException("reference.id phải là mã dương");
            }
        } else {
            reference = null;
        }
        return new ResidencyChangeContent(
                contractNode.asLong(),
                types,
                residenceType,
                careLevel,
                services,
                endDate,
                bed,
                schedule,
                noDeposit,
                deposit,
                reference);
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, JsonNode value, String field) {
        JsonNode node = value.get(field);
        try {
            return Enum.valueOf(type, node == null ? "" : node.asString());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("newValue." + field + " không hợp lệ");
        }
    }

    private static List<ServiceChange> services(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new InvalidValueException("newValue.services cần ít nhất một dòng ADD hoặc REMOVE");
        }
        List<ServiceChange> lines = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (JsonNode line : node) {
            JsonNode id = line.get("serviceId");
            if (id == null || !id.isIntegralNumber() || id.asLong() <= 0) {
                throw new InvalidValueException("services[].serviceId phải là mã dịch vụ");
            }
            if (!seen.add(id.asLong())) {
                throw new InvalidValueException("Dịch vụ " + id.asLong() + " xuất hiện hai lần trong services");
            }
            ServiceChange.Action action;
            try {
                action = ServiceChange.Action.valueOf(line.path("action").asString(""));
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException("services[].action phải là ADD hoặc REMOVE");
            }
            double quantity = line.has("quantity") ? line.get("quantity").asDouble(0) : 1;
            if (action == ServiceChange.Action.ADD && quantity <= 0) {
                throw new InvalidValueException("services[].quantity phải lớn hơn 0");
            }
            Long custom = null;
            if (line.hasNonNull("customUnitPrice")) {
                if (!line.get("customUnitPrice").isIntegralNumber()
                        || line.get("customUnitPrice").asLong() < 0) {
                    throw new InvalidValueException("services[].customUnitPrice phải là số tiền nguyên không âm");
                }
                custom = line.get("customUnitPrice").asLong();
            }
            lines.add(new ServiceChange(
                    id.asLong(), action, line.path("inPackage").asBoolean(false), quantity, custom));
        }
        return List.copyOf(lines);
    }

    private static List<ArrivalChange> schedule(JsonNode node, boolean required) {
        if (node == null || node.isNull()) {
            if (required) {
                throw new InvalidValueException("newValue.arrivalSchedule cần ít nhất một lịch đến");
            }
            return List.of();
        }
        if (!node.isArray() || (required && node.isEmpty())) {
            throw new InvalidValueException("newValue.arrivalSchedule cần ít nhất một lịch đến");
        }
        List<ArrivalChange> lines = new ArrayList<>();
        for (JsonNode line : node) {
            boolean weekly = line.hasNonNull("dayOfWeek");
            boolean dated = line.hasNonNull("date");
            if (weekly == dated) {
                throw new InvalidValueException("Mỗi lịch đến cần đúng một trong dayOfWeek hoặc date");
            }
            Integer day = null;
            if (weekly) {
                day = line.get("dayOfWeek").asInt(0);
                if (day < 1 || day > 7) {
                    throw new InvalidValueException("dayOfWeek phải từ 1 (thứ hai) đến 7");
                }
            }
            LocalTime arrival = time(line, "arrivalTime");
            LocalTime departure = time(line, "departureTime");
            if (!departure.isAfter(arrival)) {
                throw new InvalidValueException(
                        "Bán trú không qua đêm: giờ về phải sau giờ đến trong cùng ngày (Q-286)");
            }
            lines.add(new ArrivalChange(day, dated ? date(line, "date", true) : null, arrival, departure));
        }
        return List.copyOf(lines);
    }

    private static LocalDate date(JsonNode value, String field, boolean required) {
        JsonNode node = value.get(field);
        if (node == null || node.isNull()) {
            if (required) {
                throw new InvalidValueException(field + " là ngày bắt buộc");
            }
            return null;
        }
        try {
            return LocalDate.parse(node.asString());
        } catch (DateTimeParseException e) {
            throw new InvalidValueException(field + " phải có dạng yyyy-MM-dd");
        }
    }

    private static LocalTime time(JsonNode value, String field) {
        try {
            return LocalTime.parse(value.path(field).asString(""));
        } catch (DateTimeParseException e) {
            throw new InvalidValueException(field + " phải có dạng HH:mm");
        }
    }

    private static Long positive(JsonNode value, String field, boolean required) {
        JsonNode node = value.get(field);
        if (node == null || node.isNull()) {
            if (required) {
                throw new InvalidValueException("newValue." + field + " là mã bắt buộc");
            }
            return null;
        }
        if (!node.isIntegralNumber() || node.asLong() <= 0) {
            throw new InvalidValueException(field + " phải là mã dương");
        }
        return node.asLong();
    }
}
