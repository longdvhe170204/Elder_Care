package com.eldercare.backend.belongings.internal.ending.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.belongings.api.dto.BelongingEndingConditionView;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.ending.domain.OpenBelonging;
import com.eldercare.backend.belongings.internal.ending.persistence.BelongingEndingStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc đồ gửi chưa trả sau khi hồ sơ ở trạng thái cuối (013 T024; FR-025, FR-026a, FR-027, FR-027a; research K-08, K-09;
 * Q-306: chỉ nhắc, không tạo bản ghi nào). Job hằng ngày của M12 (T006, chờ A đăng ký) gọi {@link #remindUnreturned()};
 * ở đây không có {@code ScheduledJob} (M12ArchTest). Người đã qua đời có nhắc của 004 FR-072 (CFG-M02-09) thay thế, chỉ
 * đồ Tìm thấy sau khi mục sau qua đời đã Hoàn thành (mốc {@code disposal_clock_start_at}, FR-026a) vẫn nhắc ở đây.
 */
@Component
public class FinalStateReminders {

    static final String ELIGIBLE_MARKER = "M12.DISPOSAL_ELIGIBLE";
    static final String ENDING_MARKER = "M12.UNRETURNED_AT_ENDING";
    private static final String SOURCE = "M12.BELONGING_RESIDENT";

    private static final Set<ResidentStatus> ENDED =
            EnumSet.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.ADMISSION_CANCELLED);

    private final BelongingEndingStore store;
    private final BelongingEndingService ending;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    FinalStateReminders(
            BelongingEndingStore store,
            BelongingEndingService ending,
            ResidentApi residents,
            ParameterApi parameters,
            ReminderMarkers markers,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.ending = ending;
        this.residents = residents;
        this.parameters = parameters;
        this.markers = markers;
        this.events = events;
        this.time = time;
    }

    /**
     * FR-027: mỗi chu kỳ CFG-M12-02 nhắc Hành chính (Nhẹ) cho từng người cao tuổi còn đồ Đang giữ, Đang được sử dụng, Hư
     * hỏng; FR-027a: một lần mỗi đồ khi đủ CFG-M12-03 để lập đề nghị xử lý. Không nhắc đồ Thất lạc (sự cố theo 007).
     * Trả số người cao tuổi đã nhắc.
     */
    @Transactional
    public int remindUnreturned() {
        Instant now = time.now();
        Duration cycle = parameters.read(ParameterCode.CFG_M12_02).asDuration();
        Duration wait = parameters.read(ParameterCode.CFG_M12_03).asDuration();
        Map<Long, List<Long>> due = new LinkedHashMap<>();
        Map<Long, List<Long>> eligible = new LinkedHashMap<>();
        collect(residents.byStatus(ENDED), false, now, cycle, wait, due, eligible);
        collect(residents.byStatus(Set.of(ResidentStatus.DECEASED)), true, now, cycle, wait, due, eligible);
        due.forEach((residentId, ids) -> {
            events.publishEvent(BelongingNotices.notice(
                    "BELONGING_UNRETURNED_AFTER_FINAL",
                    SOURCE,
                    residentId,
                    "BELONGING_UNRETURNED_AFTER_FINAL:" + residentId + ":" + time.today(),
                    NotificationLevel.LOW,
                    residentId,
                    List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                    "Còn " + ids.size() + " đồ gửi chưa trả sau khi hồ sơ ở trạng thái cuối: liên hệ người nhận",
                    null,
                    now,
                    false));
            store.markReminded(ids, now);
        });
        eligible.forEach((residentId, ids) -> {
            List<Long> fresh = new ArrayList<>();
            ids.forEach(id -> {
                if (markers.mark(ELIGIBLE_MARKER, "M12.BELONGING", id, "ELIGIBLE", null)) {
                    fresh.add(id);
                }
            });
            if (!fresh.isEmpty()) {
                events.publishEvent(BelongingNotices.notice(
                        "BELONGING_DISPOSAL_ELIGIBLE",
                        SOURCE,
                        residentId,
                        "BELONGING_DISPOSAL_ELIGIBLE:" + residentId + ":" + fresh.getFirst(),
                        NotificationLevel.LOW,
                        residentId,
                        List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                        fresh.size() + " đồ gửi đủ thời gian chờ: có thể lập đề nghị xử lý đồ không người nhận",
                        null,
                        now,
                        false));
            }
        });
        return due.size();
    }

    private void collect(
            List<Long> residentIds,
            boolean deceased,
            Instant now,
            Duration cycle,
            Duration wait,
            Map<Long, List<Long>> due,
            Map<Long, List<Long>> eligible) {
        Map<Long, Instant> finalAt = new LinkedHashMap<>();
        for (long residentId : residentIds) {
            residents.finalStatusAt(residentId).ifPresent(at -> finalAt.put(residentId, at));
        }
        for (OpenBelonging open : store.openOf(finalAt.keySet())) {
            if (deceased && open.clockStartAt() == null) {
                continue;
            }
            Instant since = open.clockStartAt() != null ? open.clockStartAt() : finalAt.get(open.residentId());
            if (!since.plus(cycle).isAfter(now)
                    && (open.lastRemindedAt() == null
                            || !open.lastRemindedAt().plus(cycle).isAfter(now))) {
                due.computeIfAbsent(open.residentId(), k -> new ArrayList<>()).add(open.id());
            }
            if (!since.plus(wait).isAfter(now)
                    && (open.status() == BelongingStatus.HELD || open.status() == BelongingStatus.DAMAGED)) {
                eligible.computeIfAbsent(open.residentId(), k -> new ArrayList<>())
                        .add(open.id());
            }
        }
    }

    /**
     * Hồ sơ kết thúc lưu trú được lập khi còn đồ chưa trả → báo người đại diện (Nhẹ, FR-025 phần thông báo). Một lần mỗi
     * hồ sơ kết thúc.
     */
    @Transactional
    public void endingCreated(long residencyEndingId, long residentId) {
        BelongingEndingConditionView view = ending.endingCondition(residentId);
        if (view.met() || !markers.mark(ENDING_MARKER, "M02.RESIDENCY_ENDING", residencyEndingId, "NOTIFIED", null)) {
            return;
        }
        events.publishEvent(BelongingNotices.notice(
                "BELONGING_UNRETURNED_AT_ENDING",
                "M02.RESIDENCY_ENDING",
                residencyEndingId,
                NotificationLevel.LOW,
                residentId,
                List.of(new RecipientGroup.Representative(residentId)),
                "Còn " + view.blockingBelongingIds().size() + " đồ gửi chưa trả: liên hệ viện để nhận lại",
                null,
                time.now()));
    }
}
