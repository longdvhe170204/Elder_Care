package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.ConflictingRight;
import com.eldercare.backend.family.internal.right.domain.VisitPickupWish;
import com.eldercare.backend.family.internal.right.service.VisitPickupWishService.WishView;
import com.eldercare.backend.family.internal.right.service.VisitPickupWishService.Wishes;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Mong muốn của người cao tuổi về khách thăm, người đón (api-012 {@code VisitPickupWish}; FR-020a). */
@Schema(name = "VisitPickupWish")
public record VisitPickupWishResponse(
        long id,
        Long relationshipId,
        String fullName,
        VisitPickupWish.Type type,
        boolean wanted,
        OpinionSource source,
        String recordedBy,
        Instant at,
        VisitPickupWish.Status status,
        Long supersededById) {

    public static VisitPickupWishResponse of(WishView view) {
        VisitPickupWish w = view.record().content();
        return new VisitPickupWishResponse(
                view.record().id(),
                w.relationshipId(),
                w.fullName(),
                w.type(),
                w.wanted(),
                w.source(),
                view.recordedByName(),
                w.at(),
                VisitPickupWish.Status.valueOf(view.record().status()),
                w.supersededById());
    }

    /** Mong muốn Hiệu lực, lịch sử và quyền trái mong muốn đang hiệu lực (FR-020b; tính lúc đọc). */
    @Schema(name = "VisitPickupWishes")
    public record Read(
            List<VisitPickupWishResponse> effective,
            List<VisitPickupWishResponse> history,
            List<Conflict> conflictingRights) {

        public static Read of(Wishes wishes) {
            return new Read(
                    wishes.effective().stream().map(VisitPickupWishResponse::of).toList(),
                    wishes.history().stream().map(VisitPickupWishResponse::of).toList(),
                    wishes.conflictingRights().stream().map(Conflict::of).toList());
        }
    }

    @Schema(name = "VisitPickupWishConflict")
    public record Conflict(
            long relationshipId,

            @Schema(allowableValues = {"REGISTER_VISITS", "PICKUP_ALLOWED"})
            String right,

            long wishId) {

        static Conflict of(ConflictingRight c) {
            return new Conflict(c.relationshipId(), c.right().name(), c.wishId());
        }
    }
}
