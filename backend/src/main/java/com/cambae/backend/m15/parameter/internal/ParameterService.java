package com.cambae.backend.m15.parameter.internal;

import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.parameter.api.ParameterKind;
import com.cambae.backend.m15.parameter.api.ParameterValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc tham số (research R-07): giá trị hiện hành giữ trong bộ đệm của tiến trình; lệnh đổi giá trị (000 T079) xóa bộ
 * đệm sau commit. Giá trị quá khứ đọc từ parameter_history.
 */
@Service
class ParameterService implements ParameterApi {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final Map<ParameterCode, ParameterValue> cache = new ConcurrentHashMap<>();

    ParameterService(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public ParameterValue read(ParameterCode code) {
        return cache.computeIfAbsent(code, this::load);
    }

    @Override
    public ParameterValue readAt(ParameterCode code, Instant at) {
        List<String> values = jdbc.queryForList(
                "SELECT value FROM parameter_history WHERE code = ? AND effective_from <= ?"
                        + " ORDER BY effective_from DESC LIMIT 1",
                String.class,
                code.code(),
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
        if (values.isEmpty()) {
            throw new IllegalStateException(code.code() + " chưa có giá trị tại " + at);
        }
        return new ParameterValue(code, kindOf(code), json.readTree(values.getFirst()));
    }

    private ParameterValue load(ParameterCode code) {
        Map<String, Object> row =
                jdbc.queryForMap("SELECT kind, current_value FROM parameter WHERE code = ?", code.code());
        return new ParameterValue(
                code,
                ParameterKind.valueOf((String) row.get("kind")),
                json.readTree(row.get("current_value").toString()));
    }

    private ParameterKind kindOf(ParameterCode code) {
        return ParameterKind.valueOf(
                jdbc.queryForObject("SELECT kind FROM parameter WHERE code = ?", String.class, code.code()));
    }
}
