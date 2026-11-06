package com.eldercare.backend.family.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.family.web.request.VisitRegistrationRequest;
import com.eldercare.backend.family.web.request.VisitRegistrationRequest.CompanionBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// Hợp đồng ghi allOf(VisitRegistration, {trạng thái}); JSON vẫn là một object phẳng
@Schema(
        name = "Visit",
        allOf = {VisitRegistrationRequest.class, VisitResponse.State.class})
public record VisitResponse(
        long residentId,
        Long familyMemberId,
        LocalDate date,
        String startTime,
        String endTime,
        List<CompanionBody> companions,
        long id,
        int visitorCount,
        Visit.Status status,
        Instant checkedInAt,
        Instant checkedOutAt,
        String cancellationReason) {

    /** Phần trạng thái của lượt thăm (data-model mục 5). */
    @Schema(name = "VisitState")
    public record State(
            long id,
            int visitorCount,
            Visit.Status status,
            Instant checkedInAt,
            Instant checkedOutAt,
            String cancellationReason) {}

    public static VisitResponse of(SimpleRecord<Visit> record) {
        Visit v = record.content();
        // Đã vào thì chỉ còn người thực tế vào (FR-033)
        List<CompanionBody> people = v.companions().stream()
                .filter(c -> v.checkedInAt() == null || c.entered())
                .map(c -> new CompanionBody(c.fullName(), c.relationship(), c.phoneNumber()))
                .toList();
        return new VisitResponse(
                record.residentId(),
                v.familyMemberId(),
                v.date(),
                v.startTime().toString(),
                v.endTime().toString(),
                people,
                record.id(),
                v.visitorCount(),
                Visit.Status.valueOf(record.status()),
                v.checkedInAt(),
                v.checkedOutAt(),
                v.cancellationReason());
    }
}
