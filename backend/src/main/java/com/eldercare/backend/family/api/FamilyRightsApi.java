package com.eldercare.backend.family.api;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.api.dto.IdDocumentView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Quan hệ, vai trò và quyền của người thân ({@code QuyenNguoiThanApi}; 012 contracts mục 1.2, research F-14) cho phần
 * cổng của 013, 014, 017, 018 và 013 xác minh người nhận đồ. Hàm có {@code at} xét tại thời điểm đó (FR-068).
 */
public interface FamilyRightsApi {

    boolean hasActiveRelationship(long familyMemberId, long residentId);

    /** Quyền đang bật; xem sức khỏe còn cần bản đồng ý Hiệu lực bao gồm người thân (FR-011, DBR-03). */
    boolean rightEffective(long familyMemberId, long residentId, FamilyRight right, Instant at);

    boolean isRepresentative(long familyMemberId, long residentId, Instant at);

    /** FR-046b: người đại diện hoặc người thân có quyền "được phép đón" đang bật. */
    boolean canViewBelongings(long familyMemberId, long residentId, Instant at);

    /** Người đại diện Hiệu lực theo thứ tự liên hệ (FR-012). */
    List<FamilyMemberView> representatives(long residentId);

    Optional<FamilyMemberView> primaryContact(long residentId);

    /** Danh sách đón: người thân có quyền "được phép đón" tại thời điểm (FR-016 của 013, FR-025). */
    List<FamilyMemberView> allowedPickersAt(long residentId, Instant at);

    /** Giấy tờ tùy thân đã đăng ký; rỗng khi hồ sơ chưa có (013 xác minh người nhận). */
    Optional<IdDocumentView> idDocument(long familyMemberId);
}
