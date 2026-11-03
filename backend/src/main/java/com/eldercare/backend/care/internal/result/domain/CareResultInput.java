package com.eldercare.backend.care.internal.result.domain;

import com.eldercare.backend.common.Reason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Nội dung lệnh ghi nhận; deviceAt do khung đồng bộ cung cấp, không lấy từ HTTP body. */
public record CareResultInput(
        Instant performedAt,
        BigDecimal numericValue,
        String codeValue,
        Map<String, Object> jsonValue,
        boolean upperLimitConfirmed,
        String note,
        Reason reason) {
    public CareResultInput {
        jsonValue = jsonValue == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(jsonValue));
    }

    public Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (numericValue != null) values.put("numericValue", numericValue);
        if (codeValue != null) values.put("codeValue", codeValue);
        if (!jsonValue.isEmpty()) values.put("jsonValue", jsonValue);
        values.put("upperLimitConfirmed", upperLimitConfirmed);
        if (note != null) values.put("note", note);
        return Map.copyOf(values);
    }
}
