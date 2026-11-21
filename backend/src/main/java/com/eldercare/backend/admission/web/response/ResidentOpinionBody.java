package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.change.domain.ResidentOpinion;
import com.eldercare.backend.admission.internal.change.service.ResidencyChangeService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Ý kiến người cao tuổi về yêu cầu thay đổi lưu trú (api-004 {@code ResidentOpinion}; FR-043h). {@code outdated}: ghi trước
 * khi phần thực chất của yêu cầu đổi (R18). {@code recordedBy} là tên người ghi.
 */
@Schema(name = "ResidentOpinion")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidentOpinionBody(
        long id,
        ResidentOpinion.Opinion opinion,
        ResidentOpinion.Source source,
        String note,
        String recordedBy,
        Instant recordedAt,
        boolean outdated) {

    public static ResidentOpinionBody of(ResidencyChangeService.OpinionView view) {
        ResidentOpinion o = view.opinion();
        return new ResidentOpinionBody(
                o.id(), o.opinion(), o.source(), o.note(), view.recordedBy(), o.recordedAt(), view.outdated());
    }
}
