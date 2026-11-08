package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementReminder;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T007 (BR-M11-20, FR-021; research W-05, W-09): Đã duyệt từ ngày D, tới ngày D + CFG-M11-08 chưa ghi đã mua thì nhắc
 * Phụ trách bộ phận; Chờ đối chiếu từ ngày D, tới D + CFG-M11-09 thì nhắc Kế toán. Mỗi lần vào trạng thái nhắc một lần
 * (dấu ở reminder_marker theo mốc vào trạng thái); chạy bù bắt kịp vì xét mọi yêu cầu đã quá mốc. Không tạo yêu cầu
 * (Q-306).
 */
@Service
public class ProcurementReminders {
    static final String KIND = "M11.PROCUREMENT_REMINDER";

    private final ProcurementRequestStore store;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final ProcurementNotices notices;
    private final ApplicationEventPublisher events;

    public ProcurementReminders(
            ProcurementRequestStore store,
            ParameterApi parameters,
            ReminderMarkers markers,
            ProcurementNotices notices,
            ApplicationEventPublisher events) {
        this.store = store;
        this.parameters = parameters;
        this.markers = markers;
        this.notices = notices;
        this.events = events;
    }

    /** Số lần nhắc đã gửi trong lượt chạy. */
    @Transactional
    public int remind(Instant now) {
        var today = LocalDate.ofInstant(now, BusinessTime.ZONE);
        int count = 0;
        for (var row : store.waitingSince(
                ProcurementStatus.APPROVED,
                "approved_at",
                cutoff(
                        today,
                        parameters.read(ParameterCode.CFG_M11_08).asPeriod().getDays())))
            count += remind(
                    row,
                    "PURCHASE:" + row.approvedAt(),
                    "PROCUREMENT_PURCHASE_OVERDUE",
                    "Yêu cầu mua sắm #" + row.id() + " đã duyệt quá hạn, chưa ghi đã mua",
                    new RecipientGroup.DepartmentLead(row.departmentId(), new Substitution.Default()));
        for (var row : store.waitingSince(
                ProcurementStatus.PENDING_RECONCILIATION,
                "purchase_recorded_at",
                cutoff(
                        today,
                        parameters.read(ParameterCode.CFG_M11_09).asPeriod().getDays())))
            count += remind(
                    row,
                    "RECONCILE:" + row.purchaseRecordedAt(),
                    "PROCUREMENT_RECONCILIATION_OVERDUE",
                    "Yêu cầu mua sắm #" + row.id() + " chờ đối chiếu quá hạn",
                    ProcurementNotices.accountant());
        return count;
    }

    /** Vào trạng thái ngày D, nhắc từ ngày D + n: mốc vào trạng thái trước đầu ngày (hôm nay − n + 1). */
    private static Instant cutoff(LocalDate today, int days) {
        return today.minusDays(days - 1L).atStartOfDay(BusinessTime.ZONE).toInstant();
    }

    private int remind(ProcurementRequestRow row, String marker, String event, String title, RecipientGroup to) {
        if (!markers.mark(KIND, ProcurementRequestService.ENTITY, row.id(), marker, Map.of("event", event))) return 0;
        events.publishEvent(
                new ProcurementReminder(row.id(), notices.request(event, row.id(), marker, title, List.of(to))));
        return 1;
    }
}
