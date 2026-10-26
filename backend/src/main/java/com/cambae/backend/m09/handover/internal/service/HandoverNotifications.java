package com.cambae.backend.m09.handover.internal.service;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

/** M09 chỉ phát khuôn chung; M13 phân giải quyền và gửi sau commit. */
@Service
public class HandoverNotifications {
    public List<NotificationRequest> corrected(long id, long giver, Long receiver, WorkShift shift, Instant at) {
        var recipients = new ArrayList<RecipientGroup>();
        recipients.add(new RecipientGroup.Staff(giver, "HANDOVER_CORRECTED"));
        if (receiver != null && receiver != giver)
            recipients.add(new RecipientGroup.Staff(receiver, "HANDOVER_CORRECTED"));
        recipients.add(new RecipientGroup.FloorManager(shift.scopeId(), new Substitution.Default()));
        return List.of(new NotificationRequest(
                "008",
                "HANDOVER_CORRECTED",
                SourceGroup.OTHER,
                new SourceRecord("M09.HANDOVER", Long.toString(id)),
                "HANDOVER_CORRECTED:" + id + ":" + at,
                Optional.of(NotificationLevel.LOW),
                Optional.empty(),
                Optional.ofNullable(shift.scopeId()),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, "Bàn giao có nội dung đính chính")),
                Optional.of("Bàn giao có nội dung đính chính"),
                Optional.of("Bàn giao có nội dung đính chính"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }

    public List<NotificationRequest> prepared(long id, WorkShift previous, WorkShift next, Instant at) {
        var recipients = new ArrayList<RecipientGroup>();
        if (next != null && next.leadStaffId() != null && !next.missingLead())
            recipients.add(new RecipientGroup.Staff(next.leadStaffId(), "HANDOVER_PREPARED"));
        else recipients.add(new RecipientGroup.FloorManager(previous.scopeId(), new Substitution.Default()));
        return List.of(request(
                "HANDOVER_PREPARED",
                id,
                "M09.HANDOVER",
                previous.scopeId(),
                at,
                "Bàn giao ca đã lập, cần kiểm tra và xác nhận",
                recipients));
    }

    public List<NotificationRequest> feedback(long id, long giver, WorkShift s, Instant at) {
        return List.of(request(
                "HANDOVER_FEEDBACK_RECEIVED",
                id,
                "M09.HANDOVER",
                s.scopeId(),
                at,
                "Bàn giao có ý kiến, cần bổ sung và gửi lại",
                List.of(
                        new RecipientGroup.Staff(giver, "HANDOVER_FEEDBACK_RECEIVED"),
                        new RecipientGroup.FloorManager(s.scopeId(), new Substitution.Default()))));
    }

    public List<NotificationRequest> reminder(
            String event, long source, String type, WorkShift s, Instant at, String title, boolean sender) {
        var recipients = new ArrayList<RecipientGroup>();
        if (sender && s.leadStaffId() != null) recipients.add(new RecipientGroup.Staff(s.leadStaffId(), event));
        recipients.add(new RecipientGroup.FloorManager(s.scopeId(), new Substitution.Default()));
        return List.of(request(event, source, type, s.scopeId(), at, title, recipients));
    }

    private NotificationRequest request(
            String event, long id, String type, Long floor, Instant at, String title, List<RecipientGroup> recipients) {
        return new NotificationRequest(
                "008",
                event,
                SourceGroup.OTHER,
                new SourceRecord(type, Long.toString(id)),
                event + ":" + id + ":" + at,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.ofNullable(floor),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
