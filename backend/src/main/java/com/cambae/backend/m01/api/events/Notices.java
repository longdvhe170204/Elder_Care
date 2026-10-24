package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu thông báo của sự kiện 001 (contracts/module-interfaces.md mục 1.10; research H-14; 009 contracts mục 1.1).
 * Mức và nhóm người nhận theo H-14, FR-054; 001 chỉ khai, 009 phân giải người nhận, lọc theo quyền và chống gửi trùng
 * theo (bản ghi nguồn, khóa sự kiện). Họ tên không nằm trong tiêu đề hay nội dung rút gọn; phần sức khỏe chỉ ở nội dung
 * đầy đủ gắn loại {@code HEALTH} (009 FR-013, FR-019, FR-034).
 */
final class Notices {

    static final RecipientGroup OFFICE = new RecipientGroup.FacilityRole(Role.OFFICE_STAFF);
    static final RecipientGroup MANAGER = new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER);

    private Notices() {}

    /** Bác sĩ trực; không có thì 009 thay theo cách mặc định (toàn bộ Bác sĩ, H-14). */
    static RecipientGroup doctor() {
        return new RecipientGroup.OnDutyDoctor(new Substitution.Default());
    }

    static RecipientGroup assignedNurse(long residentId) {
        return new RecipientGroup.AssignedNurse(residentId, new Substitution.Default());
    }

    static NotificationRequest of(
            String eventType,
            NotificationLevel level,
            SourceRecord source,
            String eventKey,
            long residentId,
            Long floorAreaId,
            List<RecipientGroup> recipients,
            List<ContentPart> content,
            String title,
            String shortContent,
            Instant at) {
        return new NotificationRequest(
                "M01",
                eventType,
                SourceGroup.OTHER,
                source,
                eventKey,
                Optional.of(level),
                Optional.of(residentId),
                Optional.ofNullable(floorAreaId),
                recipients,
                content,
                Optional.of(shortContent),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }

    static List<ContentPart> general(String text) {
        return List.of(new ContentPart(InformationType.GENERAL, text));
    }

    static List<ContentPart> health(String text) {
        return List.of(new ContentPart(InformationType.HEALTH, text));
    }
}
