package com.eldercare.backend.family.internal.stay.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay.Status;
import com.eldercare.backend.family.internal.stay.domain.NightCount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Đêm tính phí của lượt ở lại (FR-042 (b), FR-043; research F-09): việc nửa đêm của M10, mỗi lượt Đang ở lại (hoặc đã
 * kết thúc sau mốc mà việc chưa chạy tới) có lúc bắt đầu trước mốc phát một {@link CostSourceBilled} loại
 * {@code OVERNIGHT_STAY} cho ngày bắt đầu của đêm; dấu {@code M10.STAY_NIGHT} ({@code dem_o_lai}, R-15) chống phát trùng
 * khi chạy bù. Kết thúc ghi giờ thực tế sớm hơn mốc đã phát thì báo hủy đêm đó. 012 không tạo khoản (Q-306); dịch vụ và
 * đơn giá do 010 chọn nên không gửi {@code serviceId}.
 */
@Component
class StayNights implements M10TimedTask {

    static final String SOURCE_TYPE = "OVERNIGHT_STAY";
    static final String UNIT = "NIGHT";
    static final String NIGHT = "M10.STAY_NIGHT";
    static final String NIGHT_CANCELLED = "M10.STAY_NIGHT_CANCELLED";

    private final SimpleRecordApi records;
    private final ReminderMarkers markers;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    StayNights(SimpleRecordApi records, ReminderMarkers markers, BusinessTime time, ApplicationEventPublisher events) {
        this.records = records;
        this.markers = markers;
        this.time = time;
        this.events = events;
    }

    @Override
    public String name() {
        return "FAMILY_STAY_NIGHTS";
    }

    @Override
    public Cadence cadence() {
        return Cadence.MIDNIGHT;
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        LocalDate night = scheduledAt.atZone(BusinessTime.ZONE).toLocalDate().minusDays(1);
        int billed = 0;
        for (SimpleRecord<FamilyStay> stay :
                records.listByStatus(FamilyStay.KIND, Set.of(Status.STAYING.name(), Status.ENDED.name()))) {
            FamilyStay s = stay.content();
            boolean stayingAtMidnight = s.actualStartAt() != null
                    && s.actualStartAt().isBefore(scheduledAt)
                    && (s.actualEndAt() == null || !s.actualEndAt().isBefore(scheduledAt));
            if (stayingAtMidnight
                    && markers.mark(NIGHT, FamilyStay.KIND.code(), stay.id(), night.toString(), Map.of())) {
                events.publishEvent(new CostSourceBilled(
                        SOURCE_TYPE,
                        sourceId(stay.id(), night),
                        UNIT,
                        stay.residentId(),
                        Optional.empty(),
                        BigDecimal.ONE,
                        night,
                        scheduledAt,
                        eventKey(stay.id(), night)));
                billed++;
            }
        }
        return billed;
    }

    /** Đêm đã phát có mốc 00:00 sau {@code endAt} thì báo hủy cho 010 (Q32). */
    void cancelAfter(SimpleRecord<FamilyStay> stay, Instant endAt) {
        for (LocalDate night : NightCount.nights(endAt, time.now())) {
            if (markers.isMarked(NIGHT, FamilyStay.KIND.code(), stay.id(), night.toString())
                    && markers.mark(NIGHT_CANCELLED, FamilyStay.KIND.code(), stay.id(), night.toString(), Map.of())) {
                events.publishEvent(new CostSourceChanged(
                        SOURCE_TYPE,
                        sourceId(stay.id(), night),
                        UNIT,
                        Optional.empty(),
                        true,
                        "Kết thúc ở lại trước mốc 00:00 của đêm " + night,
                        eventKey(stay.id(), night) + ":CANCELLED"));
            }
        }
    }

    static String sourceId(long stayId, LocalDate night) {
        return stayId + ":" + night;
    }

    private static String eventKey(long stayId, LocalDate night) {
        return FamilyStay.KIND.code() + ":" + sourceId(stayId, night);
    }
}
