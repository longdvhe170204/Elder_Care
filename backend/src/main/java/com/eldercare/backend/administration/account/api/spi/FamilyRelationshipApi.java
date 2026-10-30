package com.eldercare.backend.administration.account.api.spi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * SPI: quan hệ người thân cho phạm vi và quyền của người thân (FR-045 → FR-052). Khai báo ở 002 để tránh vòng M15 ↔ M10;
 * M10 (012) cài; 001, 006, 011 dùng cùng khai báo (012 contracts mục 1.1, research F-14).
 */
public interface FamilyRelationshipApi {

    /** Quan hệ Hiệu lực của người thân (FR-045). */
    List<FamilyRelationshipView> active(long familyMemberId);

    /** Số điện thoại đã đăng ký, để gọi lại xác minh (FR-050). */
    Optional<String> registeredPhoneNumber(long familyMemberId);

    boolean isActiveRepresentative(long familyMemberId, long residentId, Instant at);

    /** Người thân có quan hệ Hiệu lực với người cao tuổi. */
    boolean belongsToResident(long familyMemberId, long residentId);

    /**
     * Loại quan hệ Hiệu lực của người thân với người cao tuổi; rỗng khi không có quan hệ Hiệu lực (001 phiếu nguyện vọng khi
     * điều trị, 007 phiếu tóm tắt chuyển viện). Web hiện nhãn tiếng Việt theo mã.
     */
    Optional<FamilyRelationshipType> relationship(long familyMemberId, long residentId);

    /** Họ tên trên hồ sơ người thân, cho {@code AccountApi.fullName} (002 T032a). */
    Optional<String> familyMemberName(long familyMemberId);
}
