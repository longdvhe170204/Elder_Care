package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckService.ShiftOverview;
import java.time.Instant;
import java.util.List;

/** Schema {@code QualityCheckShiftView} của api-014 (GET /quality-checks/shifts/{shiftId}, duyệt 07/11/2026). */
public record QualityCheckShiftResponse(
        long shiftId,
        long floorAreaId,
        int minimumItems,
        int eligibleTaskCount,
        Checklist checklist,
        List<EligibleTask> eligibleTasks,
        List<Item> items) {

    public record Checklist(long id, String status, Instant createdAt, String inspectorRole) {}

    public record EligibleTask(
            long taskId,
            long taskTypeId,
            Long residentId,
            Long locationId,
            Long performerStaffId,
            Instant performedAt) {}

    public record Item(long id, long taskId, String status, String note, String inspectorRole, Instant recordedAt) {}

    public static QualityCheckShiftResponse of(ShiftOverview view) {
        return new QualityCheckShiftResponse(
                view.shiftId(),
                view.floorAreaId(),
                view.minimumItems(),
                view.eligibleTaskCount(),
                view.checklist()
                        .map(l -> new Checklist(
                                l.id(),
                                l.status(),
                                l.content().createdAt(),
                                l.content().inspectorRole()))
                        .orElse(null),
                view.eligibleTasks().stream()
                        .map(t -> new EligibleTask(
                                t.id(),
                                t.taskTypeId(),
                                t.residentId(),
                                t.locationId(),
                                t.performerStaffId(),
                                t.performedAt()))
                        .toList(),
                view.items().stream()
                        .map(i -> new Item(i.id(), i.taskId(), i.status(), i.note(), i.inspectorRole(), i.recordedAt()))
                        .toList());
    }
}
