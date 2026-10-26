package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m02.api.dto.ConditionListView;
import com.cambae.backend.m02.api.dto.ConditionStatus;
import com.cambae.backend.m02.api.dto.ConditionView;
import com.cambae.backend.m02.internal.application.service.AdmissionConditionService.Evaluation;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;

/** Danh sách điều kiện tiếp nhận (contracts/api-004.yaml, schema {@code ConditionList}; FR-004, FR-004a). */
@Schema(name = "ConditionList")
public record ConditionListBody(
        List<ConditionLineBody> conditions,
        List<Warning> warnings,

        @Schema(description = "AdmissionOpinion hiện hành hoặc null (FR-004a)")
        Object admissionOpinion) {

    /**
     * Một dòng điều kiện (schema {@code ConditionLine}); {@code exception} chỉ có ở điều kiện kết thúc đã được miễn bằng ngoại lệ
     * (FR-064).
     */
    @Schema(name = "ConditionLine")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConditionLineBody(
            String code, String name, ConditionStatus status, String basis, String module, ExceptionRefBody exception) {

        static ConditionLineBody of(ConditionView view, LongFunction<Optional<String>> approverOf) {
            ExceptionRefBody exception = view.exceptionRequestId() == null
                    ? null
                    : new ExceptionRefBody(
                            view.exceptionRequestId(),
                            approverOf.apply(view.exceptionRequestId()).orElse(null));
            return new ConditionLineBody(
                    view.code(), view.name(), view.status(), view.basis(), view.module(), exception);
        }
    }

    /** Yêu cầu ngoại lệ đã duyệt của một điều kiện kết thúc và người duyệt. */
    @Schema(name = "ConditionLineException")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExceptionRefBody(long approvalRequestId, String approver) {}

    /** Danh sách điều kiện kết thúc lưu trú (a) → (h) kèm người duyệt ngoại lệ (FR-063, FR-064); không có ý kiến vào ở. */
    public static ConditionListBody ofEnding(ConditionListView list, LongFunction<Optional<String>> approverOf) {
        return new ConditionListBody(
                list.conditions().stream()
                        .map(view -> ConditionLineBody.of(view, approverOf))
                        .toList(),
                List.of(),
                null);
    }

    public static ConditionListBody of(Evaluation evaluation, LongFunction<String> names) {
        return new ConditionListBody(
                evaluation.conditions().stream()
                        .map(view -> ConditionLineBody.of(view, id -> Optional.empty()))
                        .toList(),
                evaluation.warnings(),
                evaluation
                        .opinion()
                        .map(opinion -> AdmissionOpinionBody.of(opinion, names))
                        .orElse(null));
    }
}
