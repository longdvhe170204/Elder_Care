package com.eldercare.backend.roombedallocation.internal.vehicle.service;

import static com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule.*;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleScheduleNotice;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T022, T003 phần lịch xe (FR-018; research W-07, W-09 {@code ViecTheoPhutM03LichXe}): lịch Đang dùng quá giờ về dự
 * kiến thì nhắc người đặt một lần cho mỗi giờ về (gia hạn đặt giờ mới thì nhắc lại); lịch Đã đặt kế tiếp của cùng xe đã
 * tới giờ đi thì báo Quản lý viện. Dấu ở {@code reminder_marker} loại {@code M03.VEHICLE_SCHEDULE_REMINDER}. Không tạo
 * lịch (Q-306).
 */
@Service
public class VehicleScheduleReminders {
    static final String MARKER = "M03.VEHICLE_SCHEDULE_REMINDER";

    private final SimpleRecordApi records;
    private final VehicleScheduleService schedules;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;

    public VehicleScheduleReminders(
            SimpleRecordApi records,
            VehicleScheduleService schedules,
            ReminderMarkers markers,
            ApplicationEventPublisher events) {
        this.records = records;
        this.schedules = schedules;
        this.markers = markers;
        this.events = events;
    }

    @Transactional
    public int remind(Instant now) {
        int count = 0;
        for (var row : records.listByStatus(KIND, Set.of(IN_USE))) {
            var c = row.content();
            if (!now.isAfter(c.plannedReturnAt())) continue;
            if (!markers.mark(MARKER, KIND.code(), row.id(), "OVERDUE:" + c.plannedReturnAt(), Map.of())) continue;
            var notices = new java.util.ArrayList<>(List.of(schedules.request(
                    "VEHICLE_RETURN_OVERDUE",
                    row.id(),
                    "Lịch xe quá giờ trả xe dự kiến",
                    List.of(new RecipientGroup.Staff(c.bookedByStaffId(), "VEHICLE_SCHEDULE_BOOKER")))));
            boolean conflict = schedules.others(c.vehicleId(), row.id()).stream()
                    .anyMatch(s -> s.from().isBefore(now) && s.to().isAfter(c.plannedReturnAt()));
            if (conflict)
                notices.add(schedules.request(
                        "VEHICLE_RETURN_OVERDUE_CONFLICT",
                        row.id(),
                        "Xe chưa trả, chồng lịch kế tiếp của cùng xe",
                        List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))));
            events.publishEvent(new VehicleScheduleNotice(row.id(), notices));
            count++;
        }
        return count;
    }
}
