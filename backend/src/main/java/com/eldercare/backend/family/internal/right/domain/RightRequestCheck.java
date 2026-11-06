package com.eldercare.backend.family.internal.right.domain;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Kiểm yêu cầu thay đổi quyền ({@code KiemTraYeuCau}; research F-06, F-06a): hàm thuần chạy lúc lập, lúc xác nhận, lúc
 * duyệt và cho từng dòng phiếu đăng ký. Kiểm giấy tờ, bản đồng ý (FR-007, FR-011) cần dữ liệu nên ở service.
 */
public final class RightRequestCheck {

    /** Mong muốn Hiệu lực; {@code fromResident} khi nguồn là chính người cao tuổi. */
    public record ActiveWish(
            long id,
            Long relationshipId,
            String idDocumentNumber,
            VisitPickupWish.Type type,
            boolean wanted,
            boolean fromResident) {}

    /** Người được cấp hoặc bị tắt quyền: quan hệ và số giấy tờ của hồ sơ người thân. */
    public record Person(long relationshipId, String idDocumentNumber) {}

    public record HeldRights(long relationshipId, String idDocumentNumber, Set<FamilyRight> enabled) {}

    public record ConflictingRight(long relationshipId, FamilyRight right, long wishId) {}

    private RightRequestCheck() {}

    /** FR-017, FR-013: tắt "được phép đón", tắt "được tự về" có hiệu lực ngay khi lập, yêu cầu vẫn Chờ xác nhận. */
    public static boolean appliesOnCreation(Type type, FamilyRight right, boolean newValue) {
        return !newValue && (type == Type.SELF_RETURN || (type == Type.RIGHT && right == FamilyRight.PICKUP_ALLOWED));
    }

    /** FR-014: người thân bất kỳ chỉ được tự bỏ mình khỏi danh sách đón hoặc tắt quyền của chính mình. */
    public static boolean selfRestriction(Type type, FamilyRight right, boolean newValue, boolean ownRelationship) {
        return type == Type.RIGHT && right != null && !newValue && ownRelationship;
    }

    /**
     * FR-020b: mong muốn Hiệu lực có nguồn người cao tuổi cùng loại, cùng người mà yêu cầu bật quyền khi người cao tuổi
     * không muốn, hoặc tắt khi người cao tuổi muốn. Cùng người theo quan hệ hoặc số giấy tờ (plan, điểm báo lại 8).
     * Yêu cầu do người đại diện theo pháp luật lập không bị coi là trái (FR-020c).
     */
    public static Optional<ActiveWish> conflictingWish(
            Type type,
            FamilyRight right,
            boolean newValue,
            Person person,
            List<ActiveWish> wishes,
            boolean byLegalRepresentative) {
        if (type != Type.RIGHT || byLegalRepresentative) {
            return Optional.empty();
        }
        return wishes.stream()
                .filter(ActiveWish::fromResident)
                .filter(w -> w.type().right() == right)
                .filter(w -> samePerson(w, person.relationshipId(), person.idDocumentNumber()))
                .filter(w -> w.wanted() != newValue)
                .findFirst();
    }

    /** FR-020b: yêu cầu trái chỉ Hiệu lực qua Quản lý viện; ngoại lệ yêu cầu áp ngay khi lập (bỏ người đón). */
    public static boolean needsManagerApproval(
            Type type, FamilyRight right, boolean newValue, Optional<ActiveWish> conflict) {
        return conflict.isPresent() && !appliesOnCreation(type, right, newValue);
    }

    /** FR-020b câu cuối: quyền đang bật trái với mong muốn "không muốn" Hiệu lực, tính lúc đọc (Q-307). */
    public static List<ConflictingRight> conflictingRights(List<HeldRights> relationships, List<ActiveWish> wishes) {
        List<ConflictingRight> found = new ArrayList<>();
        for (HeldRights held : relationships) {
            for (ActiveWish wish : wishes) {
                if (wish.fromResident()
                        && !wish.wanted()
                        && held.enabled().contains(wish.type().right())
                        && samePerson(wish, held.relationshipId(), held.idDocumentNumber())) {
                    found.add(new ConflictingRight(
                            held.relationshipId(), wish.type().right(), wish.id()));
                }
            }
        }
        return found;
    }

    /** FR-005: đặt, thôi người đại diện chỉ do người đại diện khác người bị tác động xác nhận. */
    public static boolean mayConfirm(Type type, long confirmerRelationshipId, Long affectedRelationshipId) {
        boolean representativeChange = type == Type.SET_REPRESENTATIVE || type == Type.REMOVE_REPRESENTATIVE;
        return !representativeChange
                || affectedRelationshipId == null
                || confirmerRelationshipId != affectedRelationshipId;
    }

    private static boolean samePerson(ActiveWish wish, long relationshipId, String idDocumentNumber) {
        if (wish.relationshipId() != null) {
            return wish.relationshipId() == relationshipId;
        }
        return wish.idDocumentNumber() != null && wish.idDocumentNumber().equals(idDocumentNumber);
    }
}
