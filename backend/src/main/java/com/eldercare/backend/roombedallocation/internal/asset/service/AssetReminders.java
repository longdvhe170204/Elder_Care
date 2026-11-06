package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetReminder;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetRow;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T018 lượt 2 (FR-014, BR-M03-17, CFG-M03-09; research W-06, W-09): hạn bảo trì (gồm kiểm định), đăng kiểm, bảo
 * hiểm. Từ ngày (hạn − CFG-M03-09) tới hạn: nhắc Phụ trách bộ phận một lần cho mỗi hạn. Qua hạn: gắn dấu quá hạn, nhắc
 * mỗi ngày (dấu ở reminder_marker theo ngày). Dấu gỡ khi ghi lần mới Đạt (AssetService). Không tạo bản ghi (Q-306).
 */
@Service
public class AssetReminders {
    static final String KIND = "M03.ASSET_REMINDER";

    private record Due(String name, LocalDate on, String flagColumn) {}

    private final AssetStore store;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;

    public AssetReminders(
            AssetStore store, ParameterApi parameters, ReminderMarkers markers, ApplicationEventPublisher events) {
        this.store = store;
        this.parameters = parameters;
        this.markers = markers;
        this.events = events;
    }

    @Transactional
    public int remind(Instant now) {
        var today = LocalDate.ofInstant(now, BusinessTime.ZONE);
        int before = parameters.read(ParameterCode.CFG_M03_09).asPeriod().getDays();
        int count = 0;
        for (var row : store.withDueDates())
            for (var due : dues(row)) {
                if (due.on() == null) continue;
                if (today.isAfter(due.on())) {
                    store.flagOverdue(row.id(), due.flagColumn());
                    count += notify(
                            row,
                            "OVERDUE:" + due.name() + ":" + due.on() + ":" + today,
                            "ASSET_OVERDUE",
                            "Tài sản " + row.code() + " quá hạn " + label(due.name()) + " từ " + due.on(),
                            now);
                } else if (!today.isBefore(due.on().minusDays(before)))
                    count += notify(
                            row,
                            "BEFORE:" + due.name() + ":" + due.on(),
                            "ASSET_DUE_SOON",
                            "Tài sản " + row.code() + " tới hạn " + label(due.name()) + " ngày " + due.on(),
                            now);
            }
        return count;
    }

    private static List<Due> dues(AssetRow row) {
        return List.of(
                new Due("MAINTENANCE", row.maintenanceDueOn(), "maintenance_overdue"),
                new Due("VEHICLE_INSPECTION", row.vehicleInspectionDueOn(), "vehicle_inspection_overdue"),
                new Due("INSURANCE", row.insuranceDueOn(), "insurance_overdue"));
    }

    private static String label(String name) {
        return switch (name) {
            case "MAINTENANCE" -> "bảo trì, kiểm định";
            case "VEHICLE_INSPECTION" -> "đăng kiểm";
            default -> "bảo hiểm";
        };
    }

    private int notify(AssetRow row, String marker, String event, String title, Instant now) {
        if (!markers.mark(KIND, AssetKinds.ENTITY, row.id(), marker, Map.of("event", event))) return 0;
        events.publishEvent(new AssetReminder(
                row.id(),
                List.of(new NotificationRequest(
                        "M03",
                        event,
                        SourceGroup.OTHER,
                        new SourceRecord(AssetKinds.ENTITY, Long.toString(row.id())),
                        event + ":" + row.id() + ":" + marker,
                        Optional.of(NotificationLevel.LOW),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(new RecipientGroup.DepartmentLead(row.departmentId(), new Substitution.Default())),
                        List.of(new ContentPart(InformationType.GENERAL, title)),
                        Optional.of(title),
                        Optional.of(title),
                        Optional.empty(),
                        false,
                        now,
                        Map.of("assetId", row.id()),
                        false,
                        Optional.empty(),
                        Optional.empty()))));
        return 1;
    }
}
