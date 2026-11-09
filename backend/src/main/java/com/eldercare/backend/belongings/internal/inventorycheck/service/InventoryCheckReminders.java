package com.eldercare.backend.belongings.internal.inventorycheck.service;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.service.ValuableTypes;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCheck;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryPeriod;
import com.eldercare.backend.belongings.internal.inventorycheck.persistence.InventoryCheckStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc kiểm kê (013 T025; FR-028a, FR-028c, BR-M12-07, Q-306): đầu kỳ nhắc Hành chính số vị trí chưa có phiếu; sau hạn
 * nhắc mỗi ngày vị trí chưa có phiếu và phiếu chưa Hoàn thành, báo Quản lý viện một lần. Chỉ nhắc, không tạo phiếu.
 * Job hằng ngày của M12 (T006, chờ A) gọi {@link #remind()}.
 */
@Component
public class InventoryCheckReminders {

    static final String START_MARKER = "M12.INVENTORY_PERIOD_START";
    static final String MANAGER_MARKER = "M12.INVENTORY_OVERDUE_MANAGER";

    private final InventoryCheckService checks;
    private final InventoryCheckStore store;
    private final ValuableTypes valuableTypes;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    InventoryCheckReminders(
            InventoryCheckService checks,
            InventoryCheckStore store,
            ValuableTypes valuableTypes,
            ReminderMarkers markers,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.checks = checks;
        this.store = store;
        this.valuableTypes = valuableTypes;
        this.markers = markers;
        this.events = events;
        this.time = time;
    }

    @Transactional
    public void remind() {
        InventoryPeriod period = checks.currentPeriod();
        LocalDate today = time.today();
        Instant now = time.now();
        List<SimpleRecord<InventoryCheck>> ofPeriod = checks.ofPeriod(period.code());
        Set<Long> withCheck = new HashSet<>();
        ofPeriod.forEach(c -> withCheck.add(c.content().storageLocationId()));
        List<Long> missing = store.countByLocation(valuableTypes.typeIds()).keySet().stream()
                .filter(l -> !withCheck.contains(l))
                .toList();
        List<SimpleRecord<InventoryCheck>> waiting = ofPeriod.stream()
                .filter(c -> InventoryCheck.WAITING.equals(c.status()))
                .toList();
        long periodKey = period.start().toEpochDay();
        if (!missing.isEmpty() && markers.mark(START_MARKER, "M12.INVENTORY_PERIOD", periodKey, "START", null)) {
            events.publishEvent(BelongingNotices.notice(
                    "INVENTORY_PERIOD_STARTED",
                    "M12.INVENTORY_PERIOD",
                    periodKey,
                    "INVENTORY_PERIOD_STARTED:" + period.code(),
                    NotificationLevel.LOW,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                    missing.size() + " vị trí có đồ có giá trị Đang giữ chưa có phiếu kiểm kê kỳ " + period.code(),
                    null,
                    now,
                    false));
        }
        if (!period.overdueOn(today)) {
            return;
        }
        for (long location : missing) {
            events.publishEvent(BelongingNotices.notice(
                    "INVENTORY_OVERDUE",
                    "M12.STORAGE_LOCATION",
                    location,
                    "INVENTORY_OVERDUE:" + location + ":" + period.code() + ":" + today,
                    NotificationLevel.MEDIUM,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                    "Quá hạn kiểm kê kỳ " + period.code() + ": vị trí chưa có phiếu",
                    null,
                    now,
                    false));
        }
        for (SimpleRecord<InventoryCheck> check : waiting) {
            events.publishEvent(BelongingNotices.notice(
                    "INVENTORY_OVERDUE",
                    InventoryCheckService.RECORD_TYPE,
                    check.id(),
                    "INVENTORY_OVERDUE:" + check.id() + ":" + today,
                    NotificationLevel.MEDIUM,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                    "Quá hạn kiểm kê kỳ " + period.code() + ": phiếu chưa hoàn thành",
                    null,
                    now,
                    false));
        }
        if ((!missing.isEmpty() || !waiting.isEmpty())
                && markers.mark(MANAGER_MARKER, "M12.INVENTORY_PERIOD", periodKey, "OVERDUE", null)) {
            events.publishEvent(BelongingNotices.notice(
                    "INVENTORY_OVERDUE_MANAGER",
                    "M12.INVENTORY_PERIOD",
                    periodKey,
                    "INVENTORY_OVERDUE_MANAGER:" + period.code(),
                    NotificationLevel.LOW,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                    "Hết hạn kiểm kê kỳ " + period.code() + ": " + missing.size() + " vị trí chưa lập phiếu, "
                            + waiting.size() + " phiếu chưa hoàn thành",
                    null,
                    now,
                    false));
        }
    }
}
