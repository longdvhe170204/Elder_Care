package com.eldercare.backend.administration.parameter.api;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Period;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Giá trị bất biến của một tham số tại một thời điểm (research R-07). Mỗi lần kiểm tra quy tắc đọc một lần vào biến cục
 * bộ để dùng trọn một giá trị (FR-018). Thời lượng lưu dạng {@code {"value":48,"unit":"HOURS"}}; giá trị {@code null}
 * nghĩa là viện chưa đặt ("Theo cơ sở").
 */
public final class ParameterValue {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final ParameterCode code;
    private final ParameterKind kind;
    private final JsonNode raw;

    public ParameterValue(ParameterCode code, ParameterKind kind, JsonNode raw) {
        this.code = code;
        this.kind = kind;
        this.raw = raw;
    }

    public ParameterCode code() {
        return code;
    }

    public ParameterKind kind() {
        return kind;
    }

    /** Giá trị dạng JSON như lưu trong cơ sở dữ liệu. */
    public JsonNode raw() {
        return raw;
    }

    public boolean isSet() {
        return raw != null && !raw.isNull();
    }

    /** Thời lượng theo giây, phút, giờ, ngày. Tháng, năm không đổi được sang Duration: dùng {@link #asPeriod()}. */
    public Duration asDuration() {
        JsonNode v = require(ParameterKind.DURATION);
        long amount = v.get("value").longValue();
        return switch (v.get("unit").asString()) {
            case "SECONDS" -> Duration.ofSeconds(amount);
            case "MINUTES" -> Duration.ofMinutes(amount);
            case "HOURS" -> Duration.ofHours(amount);
            case "DAYS" -> Duration.ofDays(amount);
            default -> throw new IllegalStateException(code.code() + " tính theo lịch, dùng asPeriod()");
        };
    }

    /** Thời lượng theo lịch: ngày, tháng, năm (ví dụ thời hạn lưu giữ 10 năm). */
    public Period asPeriod() {
        JsonNode v = require(ParameterKind.DURATION);
        int amount = v.get("value").intValue();
        return switch (v.get("unit").asString()) {
            case "DAYS" -> Period.ofDays(amount);
            case "MONTHS" -> Period.ofMonths(amount);
            case "YEARS" -> Period.ofYears(amount);
            default -> throw new IllegalStateException(code.code() + " tính theo giờ phút, dùng asDuration()");
        };
    }

    public int asInt() {
        return require(ParameterKind.INTEGER).intValue();
    }

    public BigDecimal asPercent() {
        return require(ParameterKind.PERCENTAGE).decimalValue();
    }

    /** Số tiền theo đồng. */
    public long asMoney() {
        return require(ParameterKind.MONEY).longValue();
    }

    public LocalTime asLocalTime() {
        return LocalTime.parse(require(ParameterKind.TIME_OF_DAY).asString());
    }

    /** Tham số dạng bảng hoặc nhiều thành phần, đọc thành kiểu do module định nghĩa (FR-022). */
    public <T> T asTable(Class<T> type) {
        return MAPPER.treeToValue(require(ParameterKind.TABLE), type);
    }

    private JsonNode require(ParameterKind expected) {
        if (kind != expected) {
            throw new IllegalStateException(code.code() + " có kiểu " + kind + ", không phải " + expected);
        }
        if (!isSet()) {
            throw new IllegalStateException(code.code() + " chưa được đặt giá trị (Theo cơ sở)");
        }
        return raw;
    }

    @Override
    public String toString() {
        return code.code() + "=" + raw;
    }
}
