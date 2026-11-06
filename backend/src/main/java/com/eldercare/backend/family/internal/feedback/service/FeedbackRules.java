package com.eldercare.backend.family.internal.feedback.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.family.internal.feedback.domain.FeedbackDeadline;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Tham số của phản hồi: CFG-M10-01 (hạn theo mức), CFG-M10-02 (tự đóng); đọc lúc tính (research F-13). */
@Component
class FeedbackRules {

    record DueConfig(Amount high, Amount normal) {}

    record Amount(long value, String unit) {
        Duration duration() {
            return Duration.of(value, ChronoUnit.valueOf(unit));
        }
    }

    private final ParameterApi parameters;

    FeedbackRules(ParameterApi parameters) {
        this.parameters = parameters;
    }

    FeedbackDeadline.Durations due() {
        DueConfig config = parameters.read(ParameterCode.CFG_M10_01).asTable(DueConfig.class);
        return new FeedbackDeadline.Durations(
                config.high().duration(), config.normal().duration());
    }

    Duration autoClose() {
        return parameters.read(ParameterCode.CFG_M10_02).asDuration();
    }
}
