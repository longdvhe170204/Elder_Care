package com.cambae.backend.m15.parameter.internal.domain;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m15.parameter.api.ParameterKind;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import tools.jackson.databind.JsonNode;

/**
 * Kiểm giá trị mới của tham số (FR-015, Q-315; data-model mục 2): theo kiểu, ràng buộc kỹ thuật {@code min}, {@code max}
 * và ràng buộc có nguồn {@code greaterThan} (CFG-M15-06 > CFG-M15-05), kiểm cả hai chiều: đổi mã được tham chiếu cũng
 * kiểm mã tham chiếu tới nó. Sai thì 422 {@code INVALID_VALUE}, giá trị hiện hành giữ nguyên.
 */
public class ParameterValidator {

    /** Định nghĩa và giá trị hiện hành của một tham số. */
    public record Definition(
            String code, ParameterKind kind, String unit, JsonNode constraints, JsonNode currentValue) {}

    /** {@code related}: các tham số mà {@code target} tham chiếu, hoặc tham chiếu tới {@code target}. */
    public void validate(Definition target, JsonNode value, Collection<Definition> related) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            throw invalid(target, "thiếu giá trị");
        }
        switch (target.kind()) {
            case DURATION -> duration(target, value);
            case INTEGER, MONEY -> number(target, value, true);
            case PERCENTAGE -> number(target, value, false);
            case TIME_OF_DAY -> timeOfDay(target, value);
            case TABLE -> {
                if (!value.isObject() && !value.isArray()) {
                    throw invalid(target, "giá trị dạng bảng phải là đối tượng hoặc danh sách");
                }
            }
        }
        for (Definition other : related) {
            if (other.code().equals(greaterThan(target)) && !greater(value, other.currentValue())) {
                throw invalid(target, "phải lớn hơn " + other.code());
            }
            if (target.code().equals(greaterThan(other)) && !greater(other.currentValue(), value)) {
                throw invalid(target, "phải nhỏ hơn " + other.code());
            }
        }
    }

    private static void duration(Definition target, JsonNode value) {
        JsonNode amount = value.get("value");
        JsonNode unit = value.get("unit");
        if (!value.isObject() || amount == null || !amount.isIntegralNumber() || unit == null) {
            throw invalid(target, "thời lượng có dạng {\"value\": số nguyên, \"unit\": đơn vị}");
        }
        if (!unit.asString().equals(target.unit())) {
            throw invalid(target, "đơn vị phải là " + target.unit());
        }
        checkRange(target, amount.decimalValue());
    }

    private static void number(Definition target, JsonNode value, boolean integral) {
        if (!value.isNumber() || (integral && !value.isIntegralNumber())) {
            throw invalid(target, integral ? "phải là số nguyên" : "phải là số");
        }
        checkRange(target, value.decimalValue());
    }

    private static void timeOfDay(Definition target, JsonNode value) {
        try {
            if (!value.isString() || !value.asString().matches("\\d{2}:\\d{2}")) {
                throw new DateTimeParseException("HH:mm", value.toString(), 0);
            }
            LocalTime.parse(value.asString());
        } catch (DateTimeParseException e) {
            throw invalid(target, "giờ có dạng HH:mm");
        }
    }

    private static void checkRange(Definition target, BigDecimal amount) {
        JsonNode constraints = target.constraints();
        if (constraints == null) {
            return;
        }
        if (constraints.has("min") && amount.compareTo(constraints.get("min").decimalValue()) < 0) {
            throw invalid(target, "không nhỏ hơn " + constraints.get("min").asString());
        }
        if (constraints.has("max") && amount.compareTo(constraints.get("max").decimalValue()) > 0) {
            throw invalid(target, "không lớn hơn " + constraints.get("max").asString());
        }
    }

    private static String greaterThan(Definition definition) {
        JsonNode constraints = definition.constraints();
        return constraints != null && constraints.has("greaterThan")
                ? constraints.get("greaterThan").asString()
                : null;
    }

    /** {@code a > b} với hai thời lượng cùng đơn vị (ràng buộc có nguồn hiện chỉ giữa hai mã cùng đơn vị giờ). */
    private static boolean greater(JsonNode a, JsonNode b) {
        if (a == null || b == null || a.isNull() || b.isNull()) {
            return true;
        }
        if (!a.path("unit").asString().equals(b.path("unit").asString())) {
            throw new IllegalStateException("Ràng buộc greaterThan giữa hai đơn vị khác nhau: " + a + ", " + b);
        }
        return a.get("value").decimalValue().compareTo(b.get("value").decimalValue()) > 0;
    }

    private static InvalidValueException invalid(Definition target, String detail) {
        return new InvalidValueException("Giá trị của " + target.code() + " không hợp lệ: " + detail);
    }
}
