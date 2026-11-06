package com.eldercare.backend.family.internal.right.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import java.time.Instant;

/**
 * Mong muốn của người cao tuổi về khách thăm, người đón ({@code mong_muon_nct}; data-model mục 3a, 0.1; FR-020a, research
 * F-06a). Theo R-15 là một dòng {@code simple_record} loại {@link #KIND}, trạng thái {@link Status} ở cột {@code status};
 * khóa duy nhất khi Hiệu lực theo (người cao tuổi, loại, người được nói tới). Nội dung không sửa: ghi mới thì bản cũ
 * chuyển Được thay thế và khóa dòng.
 *
 * @param relationshipId người thân có quan hệ; {@code null} khi là người ngoài (họ tên, số giấy tờ)
 */
public record VisitPickupWish(
        Long relationshipId,
        String fullName,
        String idDocumentNumber,
        Type type,
        boolean wanted,
        OpinionSource source,
        long recordedBy,
        Instant at,
        Long supersededById) {

    public static final SharedKind<VisitPickupWish> KIND =
            SharedKind.of("M10.VISIT_PICKUP_WISH", VisitPickupWish.class);

    public enum Type {
        VISIT,
        PICKUP;

        public FamilyRight right() {
            return this == VISIT ? FamilyRight.REGISTER_VISITS : FamilyRight.PICKUP_ALLOWED;
        }
    }

    public enum Status {
        ACTIVE,
        SUPERSEDED
    }

    /** Khóa Hiệu lực ({@code khoa_hieu_luc}): một mong muốn mỗi (người cao tuổi, loại, người được nói tới). */
    public String activeKey(long residentId) {
        return "M10.WISH:" + residentId + ":" + type + ":"
                + (relationshipId != null ? relationshipId : "G" + idDocumentNumber);
    }

    public VisitPickupWish supersededBy(long id) {
        return new VisitPickupWish(
                relationshipId, fullName, idDocumentNumber, type, wanted, source, recordedBy, at, id);
    }
}
