package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.internal.assessment.domain.FamilyDeclaredTopic;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.FamilyDeclaredEntryView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Một mục "Gia đình khai", hiển thị "Tự khai, chưa được Bác sĩ kiểm chứng" (Q-412, FR-025a). */
@Schema(name = "FamilyDeclaredEntry")
public record FamilyDeclaredEntryBody(
        FamilyDeclaredTopic topic, String content, OpinionSource source, String recordedBy, Instant at) {

    public static FamilyDeclaredEntryBody of(FamilyDeclaredEntryView v) {
        return new FamilyDeclaredEntryBody(v.topic(), v.content(), v.source(), v.recordedBy(), v.at());
    }
}
