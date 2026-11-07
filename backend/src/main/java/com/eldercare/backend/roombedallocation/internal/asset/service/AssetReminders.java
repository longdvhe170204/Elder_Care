package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.BedStatusApi;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetReminder;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetRow;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommandPort;
import java.time.Duration;
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
    private final RecordEventApi records;
    private final BedAssetCommandPort beds;
    private final BedStatusApi bedStatus;

    public AssetReminders(
            AssetStore store,
            ParameterApi parameters,
            ReminderMarkers markers,
            ApplicationEventPublisher events,
            RecordEventApi records,
            BedAssetCommandPort beds,
            BedStatusApi bedStatus) {
        this.store = store;
        this.parameters = parameters;
        this.markers = markers;
        this.events = events;
        this.records = records;
        this.beds = beds;
        this.bedStatus = bedStatus;
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
        return count + remindTransfers(now);
    }

    /**
     * FR-015, Q-232, Q-235 (CFG-M03-02): giường mang dấu "chờ chuyển người" còn người (Đang sử dụng, Đang giữ chỗ) thì nhắc
     * Trưởng tầng chuyển người mỗi chu kỳ kể từ lúc gắn dấu (lần báo hỏng đã báo ngay); giường mang dấu về Trống thì nhắc
     * báo hỏng lại từ lúc về Trống, rồi mỗi chu kỳ. Trạng thái khác (Chờ sẵn sàng…) không nhắc. Một lần mỗi chu kỳ.
     */
    private int remindTransfers(Instant now) {
        var period = parameters.read(ParameterCode.CFG_M03_02).asDuration();
        int count = 0;
        for (var row : store.awaitingTransfer()) {
            var flag = records.list(AssetKinds.DAMAGE, new EntityRef(AssetKinds.ENTITY, row.id())).stream()
                    .filter(e -> e.confirmedAt() == null
                            && Boolean.TRUE.equals(e.content().awaitingTransfer()))
                    .findFirst();
            if (flag.isEmpty() || row.floorAreaId() == null) continue;
            Instant flaggedAt = flag.get().occurredAt();
            var state = beds.bedState(row.bedId());
            if (state == BedStatus.OCCUPIED || state == BedStatus.HELD) {
                long cycle = Duration.between(flaggedAt, now).dividedBy(period);
                if (cycle < 1) continue;
                count += notify(
                        row,
                        "MOVE:" + flag.get().id() + ":" + cycle,
                        "BED_TRANSFER_REMINDER",
                        "Giường " + row.code() + " hỏng, còn người nằm: cần chuyển người sang giường khác",
                        now,
                        new RecipientGroup.FloorManager(row.floorAreaId(), new Substitution.Default()));
            } else if (state == BedStatus.EMPTY) {
                var since = bedStatus.emptyPeriodStarted(row.bedId()).orElse(flaggedAt);
                if (since.isBefore(flaggedAt)) since = flaggedAt;
                if (now.isBefore(since)) continue;
                long cycle = Duration.between(since, now).dividedBy(period);
                count += notify(
                        row,
                        "REREPORT:" + flag.get().id() + ":" + since + ":" + cycle,
                        "BED_REREPORT_REMINDER",
                        "Giường " + row.code() + " mang dấu chờ chuyển người đã Trống: cần báo hỏng lại hoặc gỡ dấu",
                        now,
                        new RecipientGroup.FloorManager(row.floorAreaId(), new Substitution.Default()));
            }
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
        return notify(
                row,
                marker,
                event,
                title,
                now,
                new RecipientGroup.DepartmentLead(row.departmentId(), new Substitution.Default()));
    }

    private int notify(AssetRow row, String marker, String event, String title, Instant now, RecipientGroup to) {
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
                        List.of(to),
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
