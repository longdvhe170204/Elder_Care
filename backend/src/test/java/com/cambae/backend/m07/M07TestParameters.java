package com.cambae.backend.m07;

import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.parameter.api.ParameterKind;
import com.cambae.backend.m15.parameter.api.ParameterValue;
import java.time.Instant;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Tham số của 006 cho test đơn vị không nạp context: giá trị mặc định như V000_04 (Phụ lục 25). */
final class M07TestParameters implements ParameterApi {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Map<ParameterCode, String> DEFAULTS = Map.of(
            ParameterCode.CFG_M07_01, "{\"value\":30,\"unit\":\"MINUTES\"}",
            ParameterCode.CFG_M07_02, "{\"value\":30,\"unit\":\"MINUTES\"}",
            ParameterCode.CFG_M07_04, "{\"value\":3,\"unit\":\"DAYS\"}",
            ParameterCode.CFG_M07_05, "{\"value\":4,\"unit\":\"HOURS\"}");

    static ParameterApi defaults() {
        return new M07TestParameters();
    }

    @Override
    public ParameterValue read(ParameterCode code) {
        String raw = DEFAULTS.get(code);
        if (raw == null) {
            throw new IllegalArgumentException("Chưa có giá trị test cho " + code);
        }
        return new ParameterValue(code, ParameterKind.DURATION, MAPPER.readTree(raw));
    }

    @Override
    public ParameterValue readAt(ParameterCode code, Instant at) {
        return read(code);
    }
}
