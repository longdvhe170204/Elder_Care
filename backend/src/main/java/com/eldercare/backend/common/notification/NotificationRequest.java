package com.eldercare.backend.common.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Khuôn yêu cầu thông báo duy nhất (009 contracts/module-interfaces.md mục 1.1, research B-02; 000 T034). Module nguồn sở hữu mức, nhóm người nhận và nội dung; 009 kiểm tra và gửi.
 *
 * @param level thiếu thì 009 coi là {@code LOW} và gắn dấu (009 FR-003)
 * @param shortContent nội dung rút gọn cho kênh ngoài ứng dụng, thuộc loại {@code GENERAL} (009 FR-019)
 * @param title không chứa thông tin sức khỏe (009 FR-034)
 * @param afterDeath dấu việc sau qua đời của 004 (009 FR-015)
 * @param informationSubject bắt buộc khi có nhóm người thân (009 FR-001a)
 * @param q354Case chỉ khi nội dung có thể vượt phạm vi đồng ý (009 FR-001a, FR-014a)
 */
public record NotificationRequest(
        String sourceModule,
        String eventType,
        SourceGroup sourceGroup,
        SourceRecord sourceRecord,
        String eventKey,
        Optional<NotificationLevel> level,
        Optional<Long> residentId,
        Optional<Long> floorAreaId,
        List<RecipientGroup> recipientGroups,
        List<ContentPart> content,
        Optional<String> shortContent,
        Optional<String> title,
        Optional<Long> causedBy,
        boolean notifyCauser,
        Instant occurredAt,
        Map<String, Object> displayParams,
        boolean afterDeath,
        Optional<InformationSubject> informationSubject,
        Optional<Q354Case> q354Case) {}
