package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.staffing.api.ShiftApi;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc theo sự kiện của module khác (013 T014; research K-11): hết ca còn giữ đồ có giá trị (FR-014a), chuyển giường
 * sang tầng khác còn đồ ở vị trí tầng cũ (FR-028). Chỉ thông báo, không đổi đồ, không ghi mục bàn giao (Q-306, Q-307).
 * Mỗi sự kiện xử lý một lần nhờ dấu {@code reminder_marker} loại {@link #MARKER} (chạy lại sau lỗi không nhắc lặp);
 * {@code HeldBelongingListeners} nhận sự kiện sau commit và gọi vào đây.
 */
@Component
public class HeldBelongingReminders {

    static final String MARKER = "M12.EVENT";

    private final BelongingStore belongings;
    private final ValuableTypes valuableTypes;
    private final CatalogApi catalogs;
    private final ShiftApi shifts;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    HeldBelongingReminders(
            BelongingStore belongings,
            ValuableTypes valuableTypes,
            CatalogApi catalogs,
            ShiftApi shifts,
            ReminderMarkers markers,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.belongings = belongings;
        this.valuableTypes = valuableTypes;
        this.catalogs = catalogs;
        this.shifts = shifts;
        this.markers = markers;
        this.events = events;
        this.time = time;
    }

    /**
     * FR-014a: nhân viên của ca vừa kết thúc còn là người giữ đồ có giá trị Đang giữ → nhắc nhân viên đó và Người phụ
     * trách ca (Trung bình). Thành viên ca xét qua {@code ShiftApi.staffShifts} quanh lúc kết thúc vì {@code ShiftApi}
     * không có danh sách thành viên theo ca.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void shiftEnded(ShiftEnded event) {
        if (!markers.mark(MARKER, "M09.SHIFT", event.shiftId(), event.eventKey(), null)) {
            return;
        }
        Map<Long, List<BelongingView>> byHolder = new LinkedHashMap<>();
        for (BelongingView b : belongings.heldByStaff(valuableTypes.typeIds(), null)) {
            byHolder.computeIfAbsent(b.holderId(), k -> new ArrayList<>()).add(b);
        }
        byHolder.forEach((holder, held) ->
                shifts.staffShifts(holder, event.occurredAt().minusSeconds(1), event.occurredAt()).stream()
                        .filter(s -> s.id() == event.shiftId())
                        .findFirst()
                        .ifPresent(shift -> remindAtShiftEnd(event, shift, holder, held)));
    }

    private void remindAtShiftEnd(ShiftEnded event, ShiftView shift, long holder, List<BelongingView> held) {
        List<RecipientGroup> recipients = new ArrayList<>();
        recipients.add(new RecipientGroup.Staff(holder, "HOLDER"));
        if (shift.leadStaffId() != null && !Objects.equals(shift.leadStaffId(), holder)) {
            recipients.add(new RecipientGroup.Staff(shift.leadStaffId(), "SHIFT_LEAD"));
        }
        String eventType = "BELONGING_HELD_AT_SHIFT_END";
        events.publishEvent(BelongingNotices.notice(
                eventType,
                "M09.SHIFT",
                event.shiftId(),
                eventType + ":" + event.shiftId() + ":" + holder,
                NotificationLevel.MEDIUM,
                held.size() == 1 ? held.getFirst().residentId() : null,
                recipients,
                "Còn giữ " + held.size()
                        + " đồ gửi có giá trị khi hết ca: Chuyển giữ cho Hành chính hoặc nhân viên ca sau và ghi vào"
                        + " bàn giao ca",
                null,
                time.now(),
                false));
    }

    /**
     * FR-028: chuyển sang tầng khác mà còn đồ Đang giữ ở vị trí gắn tầng cũ → báo Hành chính và điều dưỡng phụ trách
     * (phân giải lúc gửi, nên là điều dưỡng của tầng mới); trạng thái, vị trí đồ không đổi.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void bedTransferred(BedTransferred event) {
        if (event.previousFloorId() == event.nextFloorId()) {
            return;
        }
        if (!markers.mark(MARKER, "M03.ALLOCATION", event.nextAllocationId(), "BED_TRANSFERRED", null)) {
            return;
        }
        List<Long> oldFloorLocations = catalogs.list(BelongingCatalogs.STORAGE_LOCATION, null, false).stream()
                .filter(l ->
                        l.attributes() != null && Objects.equals(l.attributes().floorAreaId(), event.previousFloorId()))
                .map(CatalogItem::id)
                .toList();
        List<BelongingView> held = belongings.heldAt(event.residentId(), oldFloorLocations);
        if (held.isEmpty()) {
            return;
        }
        events.publishEvent(BelongingNotices.notice(
                "BELONGINGS_AT_PREVIOUS_FLOOR",
                "M03.ALLOCATION",
                event.nextAllocationId(),
                NotificationLevel.LOW,
                event.residentId(),
                List.of(
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                        new RecipientGroup.AssignedNurse(event.residentId(), new Substitution.Default())),
                held.size() + " đồ gửi Đang giữ ở vị trí tầng cũ: cần Chuyển giữ",
                null,
                time.now()));
    }
}
