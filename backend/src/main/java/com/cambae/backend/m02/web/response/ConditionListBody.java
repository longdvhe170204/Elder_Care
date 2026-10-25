package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m02.api.dto.ConditionStatus;
import com.cambae.backend.m02.api.dto.ConditionView;
import com.cambae.backend.m02.internal.application.service.AdmissionConditionService.Evaluation;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.LongFunction;

/** Danh sách điều kiện tiếp nhận (contracts/api-004.yaml, schema {@code ConditionList}; FR-004, FR-004a). */
@Schema(name = "ConditionList")
public record ConditionListBody(
        List<ConditionLineBody> conditions, List<Warning> warnings, AdmissionOpinionBody admissionOpinion) {

    /** Một dòng điều kiện (schema {@code ConditionLine}); ngoại lệ được duyệt của điều kiện kết thúc thuộc user story 8. */
    @Schema(name = "ConditionLine")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConditionLineBody(String code, String name, ConditionStatus status, String basis, String module) {

        static ConditionLineBody of(ConditionView view) {
            return new ConditionLineBody(view.code(), view.name(), view.status(), view.basis(), view.module());
        }
    }

    public static ConditionListBody of(Evaluation evaluation, LongFunction<String> names) {
        return new ConditionListBody(
                evaluation.conditions().stream().map(ConditionLineBody::of).toList(),
                evaluation.warnings(),
                evaluation
                        .opinion()
                        .map(opinion -> AdmissionOpinionBody.of(opinion, names))
                        .orElse(null));
    }
}
