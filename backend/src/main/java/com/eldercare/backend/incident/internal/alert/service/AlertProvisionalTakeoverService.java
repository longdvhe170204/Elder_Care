package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tạm nhận theo phút (FR-037 ngoại lệ, Q-94, research S-18, S-21): mỗi cặp (ca sau, tầng) đã bắt đầu mà bàn giao chưa
 * xác nhận được xử lý một lần (dấu {@code reminder_marker}), trong transaction riêng theo tầng.
 */
@Service
public class AlertProvisionalTakeoverService {
    private static final Logger log = LoggerFactory.getLogger(AlertProvisionalTakeoverService.class);

    private final AlertAssigneeTransfer transfer;
    private final TransactionTemplate transaction;
    private final ReminderMarkers markers;
    private final Optional<AlertStaffPort> staff;
    /** Khoảng nhìn lại để làm bù khi Bộ lập lịch ngừng chạy (FR-084); thông số kỹ thuật, dấu đã làm chặn lặp. */
    private final Duration lookBack;

    public AlertProvisionalTakeoverService(
            AlertAssigneeTransfer transfer,
            TransactionTemplate transaction,
            ReminderMarkers markers,
            @Autowired(required = false) AlertStaffPort staff,
            @Value("${eldercare.jobs.provisional-handover.look-back:P1D}") Duration lookBack) {
        this.transfer = transfer;
        this.transaction = transaction;
        this.markers = markers;
        this.staff = Optional.ofNullable(staff);
        this.lookBack = lookBack;
    }

    /** Số mục đã chuyển tạm; lỗi của một tầng không chặn tầng khác. */
    public int run(Instant now) {
        if (staff.isEmpty()) return 0;
        int moved = 0;
        for (var pending : staff.get().startedWithoutConfirmedHandover(now.minus(lookBack), now)) {
            try {
                Integer count = transaction.execute(status -> markers.mark(
                                "M05.PROVISIONAL_HANDOVER",
                                "M09.SHIFT",
                                pending.nextShiftId(),
                                "FLOOR:" + pending.floorAreaId(),
                                null)
                        ? transfer.takeOverProvisionally(pending.previousShiftId(), pending.floorAreaId(), now)
                        : 0);
                moved += count == null ? 0 : count;
            } catch (RuntimeException exception) {
                log.warn(
                        "Tạm nhận bàn giao ca {} tầng {} lỗi", pending.nextShiftId(), pending.floorAreaId(), exception);
            }
        }
        return moved;
    }
}
