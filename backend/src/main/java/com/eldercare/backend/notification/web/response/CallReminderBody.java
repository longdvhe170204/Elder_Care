package com.eldercare.backend.notification.web.response;

import com.eldercare.backend.notification.internal.urgent.service.CallReminderQueries.CallReminderView;
import com.eldercare.backend.notification.web.request.CallResultBody;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lượt nhắc gọi (schema {@code CallReminder}; FR-027, FR-027a, FR-030a). */
public record CallReminderBody(
        long id,
        long callChainId,
        PersonToCall personToCall,
        Long residentId,
        List<ContentToReport> contentToReport,
        String currentlyReminded,
        OffsetDateTime dueAt,
        Status status,
        String stopReason,
        List<Map<String, Object>> handoffs,
        List<CallResultBody> results,
        boolean canRecordResult) {

    public enum Status {
        AWAITING_RESULT,
        RESULT_RECORDED,
        STOPPED
    }

    public enum PersonType {
        STAFF,
        FAMILY_MEMBER
    }

    /** Người cần gọi; số điện thoại là số hiện hành lúc mở (FR-027). */
    public record PersonToCall(PersonType type, String fullName, String relationship, String phoneNumber) {}

    public record ContentToReport(OffsetDateTime occurredAt, String content) {}

    public static CallReminderBody of(CallReminderView v) {
        return new CallReminderBody(
                v.id(),
                v.callChainId(),
                new PersonToCall(
                        PersonType.valueOf(v.personToCall().type().name()),
                        v.personToCall().fullName(),
                        v.personToCall().relationship(),
                        v.personToCall().phoneNumber()),
                v.residentId(),
                v.contentToReport().stream()
                        .map(c -> new ContentToReport(c.occurredAt().atOffset(ZoneOffset.UTC), c.content()))
                        .toList(),
                v.currentlyReminded(),
                v.dueAt().atOffset(ZoneOffset.UTC),
                Status.valueOf(v.status()),
                v.stopReason(),
                v.handoffs().stream()
                        .map(h -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("previousStaffId", h.previousStaffId());
                            m.put("remindedStaffId", h.remindedStaffId());
                            m.put("tier", h.tier());
                            m.put("reason", h.reason());
                            m.put("dueAt", h.dueAt().atOffset(ZoneOffset.UTC));
                            m.put("occurredAt", h.occurredAt().atOffset(ZoneOffset.UTC));
                            return m;
                        })
                        .toList(),
                v.results().stream().map(CallResultBody::of).toList(),
                v.canRecordResult());
    }
}
