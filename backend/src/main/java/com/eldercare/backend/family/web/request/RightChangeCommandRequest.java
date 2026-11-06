package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.family.internal.right.service.RightChangeRequestService.DecisionData;
import jakarta.validation.constraints.Size;

/**
 * Thân lệnh trên yêu cầu thay đổi quyền: lý do (duyệt, từ chối, hủy); bản scan và người ký khi Hành chính ghi nhận bản ký
 * (FR-015 b); nội dung đã trao đổi với người cao tuổi khi duyệt yêu cầu trái mong muốn (FR-020b).
 */
public record RightChangeCommandRequest(
        Reason reason,
        Long evidenceFileId,
        Long signedByFamilyMemberId,

        @Size(max = 2000) String discussedWithResident) {

    public static final RightChangeCommandRequest EMPTY = new RightChangeCommandRequest(null, null, null, null);

    public DecisionData decision() {
        return new DecisionData(evidenceFileId, signedByFamilyMemberId, discussedWithResident);
    }
}
