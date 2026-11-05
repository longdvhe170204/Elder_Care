package com.eldercare.backend.care.internal.qualitycheck.service;

import static com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.*;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.QualityCheckManagerNotice;
import com.eldercare.backend.care.api.events.QualityCheckReminder;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.internal.qualitycheck.persistence.QualityCheckStore;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T024 (FR-059a, FR-063; research L-10; Q-306): bước {@code CARE_MINUTE}, chỉ nhắc, chuyển trạng thái theo giờ và báo;
 * không lập danh sách hay thêm mục. Tại mốc CFG-M09-04 trước kết ca nhắc người kiểm tra khi chưa có danh sách mà đã có
 * công việc đủ điều kiện, hoặc danh sách thiếu tỷ lệ. Hết ca: mục chưa có kết quả thành Quá hạn kiểm tra, báo Quản lý viện
 * một lần mỗi danh sách; ca có công việc đủ điều kiện mà không có danh sách ghi dấu {@code M04.QUALITY_NO_CHECKLIST} (bảng
 * {@code reminder_marker}, data-model {@code ca_khong_danh_sach}) kèm lý do, báo Quản lý viện một lần.
 */
@Component
public class QualityCheckClockStep implements CareMinuteWork {
    static final String REASON_NO_INSPECTOR = "NO_INSPECTOR";
    static final String REASON_NOT_CREATED = "NOT_CREATED";

    private final QualityCheckStore store;
    private final QualityCheckService checks;
    private final SimpleRecordApi lists;
    private final RecordEventApi records;
    private final ObjectProvider<ShiftStaffPort> shifts;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;

    public QualityCheckClockStep(
            QualityCheckStore store,
            QualityCheckService checks,
            SimpleRecordApi lists,
            RecordEventApi records,
            ObjectProvider<ShiftStaffPort> shifts,
            ParameterApi parameters,
            ReminderMarkers markers,
            ApplicationEventPublisher events) {
        this.store = store;
        this.checks = checks;
        this.lists = lists;
        this.records = records;
        this.shifts = shifts;
        this.parameters = parameters;
        this.markers = markers;
        this.events = events;
    }

    @Override
    @Transactional
    public int run(Instant now) {
        var source = shifts.getIfAvailable();
        if (source == null) return 0;
        var window = parameters.read(ParameterCode.CFG_M09_04).asDuration();
        int count = 0;
        // Giới hạn kỹ thuật: ca có công việc hoàn thành từ đầu ngày hôm qua (ca đêm qua nửa đêm, chạy bù sau khi dừng).
        var since = now.atZone(BusinessTime.ZONE)
                .toLocalDate()
                .minusDays(1)
                .atStartOfDay(BusinessTime.ZONE)
                .toInstant();
        for (var pair : store.recentShifts(since)) {
            var shift = source.shift(pair[0]).filter(s -> s.floorAreaId() == pair[1]);
            if (shift.isEmpty()) continue;
            var s = shift.get();
            if (!now.isBefore(s.endsAt().minus(window)) && now.isBefore(s.endsAt())) count += remind(s, now);
            else if (!now.isBefore(s.endsAt())) count += close(s, now);
        }
        return count;
    }

    private int remind(ShiftStaffPort.ShiftView shift, Instant now) {
        var status = checks.status(shift, now);
        if (status.eligible() == 0 || status.inspectorStaffIds().isEmpty()) return 0;
        var list = lists.findByUniqueKey(CHECKLISTS, shift.id() + ":" + shift.floorAreaId());
        int items = list.map(l -> records.list(ITEMS, new EntityRef("M04.QUALITY_CHECKLIST", l.id()))
                        .size())
                .orElse(0);
        int needed = minimumItems(
                status.eligible(), parameters.read(ParameterCode.CFG_M04_11).asPercent());
        if (list.isPresent() && items >= needed) return 0;
        if (!markers.mark("M04.QUALITY_REMINDER", "M04.SHIFT", shift.id(), "FLOOR:" + shift.floorAreaId(), null))
            return 0;
        var recipients = new ArrayList<RecipientGroup>();
        for (long staff : status.inspectorStaffIds())
            recipients.add(new RecipientGroup.Staff(staff, "QUALITY_INSPECTOR"));
        events.publishEvent(new QualityCheckReminder(
                shift.id(), shift.floorAreaId(), context(shift, "QUALITY_REMINDER", now, recipients)));
        return 1;
    }

    private int close(ShiftStaffPort.ShiftView shift, Instant now) {
        var list = lists.findByUniqueKey(CHECKLISTS, shift.id() + ":" + shift.floorAreaId());
        if (list.isPresent()) {
            int overdue = 0;
            for (var row : records.list(
                    ITEMS, new EntityRef("M04.QUALITY_CHECKLIST", list.get().id()))) {
                if (row.confirmedAt() != null || !PENDING.equals(row.content().status())) continue;
                var item = row.content();
                records.updateDraft(
                        ITEMS, row.id(), item.result(OVERDUE, null, item.photoFileIds(), null, null, shift.endsAt()));
                records.confirm(ITEMS, row.id(), now);
                overdue++;
            }
            if (overdue == 0) return 0;
            if (!markers.mark(
                    "M04.QUALITY_OVERDUE", "M04.QUALITY_CHECKLIST", list.get().id(), "NOTIFIED", null)) return overdue;
            var locked = lists.lockForUpdate(CHECKLISTS, list.get().id());
            lists.change(
                    CHECKLISTS,
                    locked.id(),
                    locked.version(),
                    new RecordChange<>("CLOSED", locked.content(), null, locked.uniqueKey(), true));
            managerNotice(shift, "OVERDUE_ITEMS", now);
            return overdue;
        }
        var status = checks.status(shift, shift.endsAt().minusNanos(1000));
        if (status.eligible() == 0) return 0;
        String reason = status.noInspector() ? REASON_NO_INSPECTOR : REASON_NOT_CREATED;
        if (!markers.mark(
                "M04.QUALITY_NO_CHECKLIST",
                "M04.SHIFT",
                shift.id(),
                "FLOOR:" + shift.floorAreaId(),
                Map.of("floorAreaId", shift.floorAreaId(), "reason", reason, "eligible", status.eligible()))) return 0;
        managerNotice(shift, "NO_CHECKLIST", now);
        return 1;
    }

    private void managerNotice(ShiftStaffPort.ShiftView shift, String cause, Instant now) {
        events.publishEvent(new QualityCheckManagerNotice(
                shift.id(),
                shift.floorAreaId(),
                cause,
                context(shift, cause, now, List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
    }

    private static CareEventContext context(
            ShiftStaffPort.ShiftView shift, String milestone, Instant now, List<RecipientGroup> recipients) {
        return new CareEventContext(
                "M04.SHIFT",
                shift.id(),
                milestone + ":" + shift.id() + ":" + shift.floorAreaId(),
                now,
                Optional.of(shift.floorAreaId()),
                recipients);
    }
}
