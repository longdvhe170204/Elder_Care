package com.eldercare.backend.family.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.family.web.request.FamilyStayRegistrationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

// Hợp đồng ghi allOf(FamilyStayRegistration, {trạng thái}); JSON vẫn là một object phẳng
@Schema(
        name = "FamilyStay",
        allOf = {FamilyStayRegistrationRequest.class, FamilyStayResponse.State.class})
public record FamilyStayResponse(
        long familyMemberId,
        long residentId,
        Instant expectedStartAt,
        Instant expectedEndAt,
        String location,
        String reason,
        boolean mealRegistered,
        long id,
        FamilyStay.Status status,
        Instant actualStartAt,
        Instant actualEndAt,
        int nightCount,
        String familyMemberName,
        RoommateOpinion roommateOpinion) {

    /**
     * Phần trạng thái của lượt ở lại (data-model mục 6); {@code familyMemberName} là họ tên người thân ở lại, chỉ để hiển
     * thị (test BF-12 2026-11-24); {@code roommateOpinion} là ý kiến người cùng phòng ghi lúc xác nhận, chỉ trả cho nhân
     * viên (góp ý test BF-12 2026-11-25).
     */
    @Schema(name = "FamilyStayState")
    public record State(
            long id,
            FamilyStay.Status status,
            Instant actualStartAt,
            Instant actualEndAt,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            int nightCount,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            String familyMemberName,

            RoommateOpinion roommateOpinion) {}

    /** Ý kiến người cùng phòng (FR-040): người được hỏi, kết quả {@code AGREED}/{@code DISAGREED}, ghi chú. */
    @Schema(name = "RoommateOpinion", accessMode = Schema.AccessMode.READ_ONLY)
    public record RoommateOpinion(String askedPerson, String result, String note) {

        static RoommateOpinion of(Map<String, Object> opinion) {
            if (opinion == null || opinion.isEmpty()) {
                return null;
            }
            return new RoommateOpinion(text(opinion, "askedPerson"), text(opinion, "result"), text(opinion, "note"));
        }

        private static String text(Map<String, Object> opinion, String key) {
            Object value = opinion.get(key);
            return value == null ? null : value.toString();
        }
    }

    /** {@code roommateOpinion} đã lọc theo người xem ({@code null} với người thân). */
    public static FamilyStayResponse of(
            SimpleRecord<FamilyStay> record,
            int nightCount,
            String familyMemberName,
            Map<String, Object> roommateOpinion) {
        FamilyStay s = record.content();
        return new FamilyStayResponse(
                s.familyMemberId(),
                record.residentId(),
                s.expectedStartAt(),
                s.expectedEndAt(),
                s.location(),
                s.reason(),
                s.mealRegistered(),
                record.id(),
                FamilyStay.Status.valueOf(record.status()),
                s.actualStartAt(),
                s.actualEndAt(),
                nightCount,
                familyMemberName,
                RoommateOpinion.of(roommateOpinion));
    }
}
