package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.incident.api.events.AssigneeChanged;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.HandoverItem;
import com.eldercare.backend.incident.internal.alert.persistence.HandoverItemStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Chuyển người phụ trách cảnh báo, sự cố theo bàn giao ca (FR-037, BR-M05-05, Q-94, CHK016; research S-18). Chạy trong
 * transaction của bên gọi: xác nhận bàn giao của 008 hoặc lần chạy tạm nhận theo tầng.
 */
@Component
public class AlertAssigneeTransfer {
    private static final String INCIDENT_SOURCE = "M05.INCIDENT";

    private final HandoverItemStore items;
    private final AlertResponsibility responsibility;
    private final AccountApi accounts;
    private final ApplicationEventPublisher events;
    private final Optional<AlertStaffPort> staff;
    private final AlertHistory history;

    public AlertAssigneeTransfer(
            HandoverItemStore items,
            AlertResponsibility responsibility,
            AccountApi accounts,
            ApplicationEventPublisher events,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) RecordEventApi records) {
        this.items = items;
        this.responsibility = responsibility;
        this.accounts = accounts;
        this.events = events;
        this.staff = Optional.ofNullable(staff);
        this.history = new AlertHistory(records);
    }

    /**
     * Bàn giao đã xác nhận: mục có người phụ trách thuộc ca trước (không phải Bác sĩ, Quản lý viện) hoặc đang tạm nhận
     * chuyển sang Điều dưỡng phụ trách người cao tuổi lúc này, không có thì Người phụ trách ca; gỡ dấu tạm nhận.
     */
    public int onConfirmed(long previousShiftId, long floorAreaId, Instant now) {
        int moved = 0;
        for (var item : items.lockOpenByFloor(floorAreaId)) {
            if (!item.provisional() && !ofPreviousShift(item, previousShiftId)) continue;
            var next = responsibility.initialAssignee(item.residentId(), floorAreaId, now);
            if (next.isEmpty()) continue;
            if (Objects.equals(next.get(), item.assigneeId()) && !item.provisional()) continue;
            move(item, next.get(), false, "HANDOVER_CONFIRMED", now);
            moved++;
        }
        return moved;
    }

    /**
     * Ca sau đã bắt đầu mà bàn giao chưa xác nhận: mục có người phụ trách thuộc ca trước và không có tên trong ca nào
     * đang diễn ra chuyển tạm sang Người phụ trách ca sau, không có thì Trưởng tầng (Q-94).
     */
    public int takeOverProvisionally(long previousShiftId, long floorAreaId, Instant now) {
        int moved = 0;
        for (var item : items.lockOpenByFloor(floorAreaId)) {
            if (item.provisional() || !ofPreviousShift(item, previousShiftId)) continue;
            var staffId = responsibility.staffOfAccount(item.assigneeId()).orElse(null);
            if (staffId == null || staff.get().inProgressShiftMember(staffId, now)) continue;
            var next = responsibility.provisionalAssignee(floorAreaId, now);
            if (next.isEmpty() || Objects.equals(next.get(), item.assigneeId())) continue;
            move(item, next.get(), true, "PROVISIONALLY_TAKEN_OVER", now);
            moved++;
        }
        return moved;
    }

    /** "Thuộc ca trước": người phụ trách có tên trong ca vừa bàn giao; Bác sĩ, Quản lý viện không chuyển (CHK016). */
    private boolean ofPreviousShift(HandoverItem item, long previousShiftId) {
        if (item.assigneeId() == null || staff.isEmpty()) return false;
        if (accounts.hasRole(item.assigneeId(), Role.DOCTOR, Role.NURSING_HOME_MANAGER)) return false;
        return responsibility
                .staffOfAccount(item.assigneeId())
                .map(staffId -> staff.get().listedInShift(staffId, previousShiftId))
                .orElse(false);
    }

    private void move(HandoverItem item, long assigneeId, boolean provisional, String action, Instant now) {
        items.reassign(item, assigneeId, provisional);
        if (HandoverItem.ALERT.equals(item.group())) {
            history.add(item.id(), action, Map.of("from", item.assigneeId(), "to", assigneeId), now);
        }
        String source = HandoverItem.ALERT.equals(item.group()) ? AlertNotices.SOURCE_TYPE : INCIDENT_SOURCE;
        String key = source + ":" + item.id() + ":" + action + ":" + assigneeId + ":" + now.toEpochMilli();
        events.publishEvent(new AssigneeChanged(
                source, item.id(), assigneeId, key, now, notice(item, source, key, assigneeId, provisional, now)));
    }

    /** Báo người phụ trách mới: mức Nhẹ; tạm nhận bàn giao mức Trung bình (FR-085, Q-311; research S-18). */
    private List<NotificationRequest> notice(
            HandoverItem item, String source, String key, long assigneeId, boolean provisional, Instant now) {
        var staffId = responsibility.staffOfAccount(assigneeId).orElse(null);
        if (staffId == null) return List.of();
        String content = provisional
                ? "Bạn tạm nhận một mục chờ xác nhận bàn giao ca. Mở mục để xem và xử lý."
                : "Bạn là người phụ trách mới của một mục sau bàn giao ca. Mở mục để xem và xử lý.";
        return List.of(new NotificationRequest(
                "M05",
                provisional ? "HANDOVER_PROVISIONAL_ASSIGNEE" : "HANDOVER_ASSIGNEE_CHANGED",
                SourceGroup.HEALTH_ALERT,
                new SourceRecord(source, Long.toString(item.id())),
                key,
                Optional.of(provisional ? NotificationLevel.MEDIUM : NotificationLevel.LOW),
                Optional.ofNullable(item.residentId()),
                Optional.ofNullable(item.floorAreaId()),
                List.of(new RecipientGroup.Staff(staffId, "HANDOVER_ASSIGNEE")),
                List.of(new ContentPart(InformationType.HEALTH, content)),
                Optional.of("Người phụ trách đã đổi"),
                Optional.of("Bàn giao ca"),
                Optional.empty(),
                false,
                now,
                Map.of("sourceId", item.id()),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
