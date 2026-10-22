package com.cambae.backend.support.fake.m15;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.parameter.api.spi.ParameterValueCheck;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Bản giả {@link ParameterValueCheck} (module sở hữu tham số cài, ví dụ 017 cho CFG-M11-10): nhận mọi giá trị cho tới
 * khi test gọi {@link #reject}; ghi lại các giá trị đã kiểm. Test khai một bean cho mã tham số mình cần.
 */
public class FakeParameterValueCheck implements ParameterValueCheck {

    private final ParameterCode code;
    private final List<JsonNode> checked = new ArrayList<>();
    private String rejectReason;

    public FakeParameterValueCheck(ParameterCode code) {
        this.code = code;
    }

    /** Từ chối mọi giá trị với lý do {@code reason}. */
    public FakeParameterValueCheck reject(String reason) {
        this.rejectReason = reason;
        return this;
    }

    public FakeParameterValueCheck accept() {
        this.rejectReason = null;
        return this;
    }

    public List<JsonNode> checked() {
        return List.copyOf(checked);
    }

    @Override
    public ParameterCode code() {
        return code;
    }

    @Override
    public void check(JsonNode value) {
        checked.add(value);
        if (rejectReason != null) {
            throw new InvalidValueException(rejectReason);
        }
    }
}
