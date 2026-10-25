package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.api.dto.ProgressStepView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Tiến trình tiếp nhận (contracts/api-004.yaml, schema {@code Progress}; FR-003). */
@Schema(name = "Progress")
public record ProgressBody(List<StepBody> steps) {

    public record StepBody(String name, boolean done, String basis) {}

    public static ProgressBody of(List<ProgressStepView> steps) {
        return new ProgressBody(steps.stream()
                .map(step -> new StepBody(step.name(), step.done(), step.basis()))
                .toList());
    }
}
