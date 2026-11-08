package com.eldercare.backend.incident.internal.trend.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.events.MeasurementTaskMissed;
import com.eldercare.backend.care.api.events.SleepResultRecorded;
import com.eldercare.backend.care.api.events.SleepResultVoided;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.health.api.events.VitalSignCorrected;
import com.eldercare.backend.incident.api.events.WeightLossDetected;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleView;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.trend.domain.ProlongedInsomniaCounter;
import com.eldercare.backend.incident.internal.trend.domain.WeightLoss;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cảnh báo theo xu hướng (FR-039 → FR-039b, BR-M05-04; research S-10): chạy khi dữ liệu nguồn đổi, không quét. Quy tắc
 * tắt thì không tạo; bật lại không xét bù. Mất ngủ kéo dài (FR-039c) dựng từ các sự kiện kết quả ngủ của 005, giữ mỗi lần ghi, đính chính, hủy thành một dòng
 * {@code M05.SLEEP_NIGHT}; từ chối
 * thuốc do 006 đếm và gửi yêu cầu cảnh báo, ở đây chỉ áp bật/tắt và mức của quy tắc.
 */
@Component
public class TrendRuleService {
    static final String WEIGHT_LOSS = "WEIGHT_LOSS";
    static final String MISSED_MEASUREMENT = "MISSED_MEASUREMENT";
    static final String MEDICATION_REFUSAL = "MEDICATION_REFUSAL";
    static final String WEIGHT_CODE = "WEIGHT";
    /** Loại yêu cầu cảnh báo "từ chối liên tiếp" của 006 (module-interfaces 006, {@code YeuCauCanhBao}). */
    static final String CONSECUTIVE_REFUSAL = "TU_CHOI_LIEN_TIEP";
    /** Nguồn là liều bị từ chối ({@code dose:<lieuId>}); khóa chống trùng theo thuốc ({@code refusal:<nctId>:<thuocId>}). */
    private static final Pattern REFUSAL_SOURCE = Pattern.compile("dose:(\\d+)");

    private static final Pattern REFUSAL_KEY = Pattern.compile("refusal:\\d+:(\\d+)");

    private static final String MARKER_KIND = "M05.TREND";
    static final String PROLONGED_INSOMNIA = "PROLONGED_INSOMNIA";
    private static final SharedKind<Map> SLEEP_NIGHT = SharedKind.of("M05.SLEEP_NIGHT", Map.class);

    private final IncidentCatalogService catalog;
    private final ParameterApi parameters;
    private final VitalSignApi vitalSigns;
    private final AlertService alerts;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;
    private final RecordEventApi recordEvents;

    public TrendRuleService(
            IncidentCatalogService catalog,
            ParameterApi parameters,
            VitalSignApi vitalSigns,
            AlertService alerts,
            ReminderMarkers markers,
            ApplicationEventPublisher events,
            RecordEventApi recordEvents) {
        this.catalog = catalog;
        this.parameters = parameters;
        this.vitalSigns = vitalSigns;
        this.alerts = alerts;
        this.markers = markers;
        this.events = events;
        this.recordEvents = recordEvents;
    }

    /** Sụt cân (FR-039a): cân nặng mới được xác nhận so với giá trị cao nhất trong cửa sổ trước đó. */
    @ApplicationModuleListener
    public void onVitalSignConfirmed(VitalSignConfirmed event) {
        Instant at = event.measuredAt() != null ? event.measuredAt() : event.occurredAt();
        checkWeight(event.residentId(), event.indicatorId(), event.recordId(), at, "WEIGHT_LOSS:" + event.recordId());
    }

    /**
     * Cân nặng đã đính chính được xét lại với giá trị hiện hành (research S-10); bản bị hủy không còn trong
     * {@code withinRange} nên không chạy. Cảnh báo đã tạo không tự đóng (FR-019).
     */
    @ApplicationModuleListener
    public void onVitalSignCorrected(VitalSignCorrected event) {
        checkWeight(
                event.residentId(),
                event.indicatorId(),
                event.originalRecordId(),
                event.measuredAt(),
                "WEIGHT_LOSS:" + event.eventKey());
    }

    /**
     * Từ chối cùng một thuốc (FR-039, BR-M07-13): 006 đếm và yêu cầu; cảnh báo theo thuốc (FR-028), mức của quy tắc
     * {@code MEDICATION_REFUSAL}, quy tắc tắt thì không tạo. Mỗi lần 006 yêu cầu là một lần gộp.
     */
    @ApplicationModuleListener
    public void onMedicationAlertRequested(MedicationAlertRequestedEvent event) {
        if (!CONSECUTIVE_REFUSAL.equals(event.alertType())) return;
        var rule = enabled(MEDICATION_REFUSAL);
        if (rule.isEmpty()) return;
        var source = REFUSAL_SOURCE.matcher(event.sourceRecord());
        var medication = REFUSAL_KEY.matcher(event.deduplicationKey());
        if (!source.matches() || !medication.matches()) {
            throw new IllegalArgumentException(
                    "Yêu cầu từ chối thuốc không đúng dạng: " + event.sourceRecord() + ", " + event.deduplicationKey());
        }
        long doseId = Long.parseLong(source.group(1));
        long medicationId = Long.parseLong(medication.group(1));
        // Mỗi liều bị từ chối là một nguồn: giao lại sự kiện không gộp hai lần, dòng nguồn không trùng khóa.
        String key = event.deduplicationKey() + ":" + doseId;
        if (!markers.mark(MARKER_KIND, "M01.RESIDENT", event.residentId(), key, null)) return;
        alerts.requestAlert(new CreateAlertCommand(
                event.residentId(),
                null,
                MEDICATION_REFUSAL,
                TypeLock.forMedication(CONSECUTIVE_REFUSAL, medicationId),
                rule.get().level(),
                event.occurredAt(),
                "DOSE",
                doseId,
                "Từ chối cùng một thuốc liên tiếp",
                null));
    }

    private void checkWeight(long residentId, long indicatorId, long recordId, Instant at, String key) {
        var rule = enabled(WEIGHT_LOSS);
        if (rule.isEmpty()) return;
        var latestOfType = vitalSigns.latestPerType(residentId).get(indicatorId);
        if (latestOfType == null || !WEIGHT_CODE.equals(latestOfType.indicatorCode())) return;
        var setting = parameters.read(ParameterCode.CFG_M05_03).asTable(WeightLossSetting.class);
        var current = vitalSigns.withinRange(residentId, indicatorId, at, at.plusMillis(1)).stream()
                .filter(reading -> reading.id() == recordId)
                .findFirst();
        if (current.isEmpty()) return;
        var latest = reading(current.get());
        if (latest.isEmpty()) return;
        var previous =
                vitalSigns
                        .withinRange(residentId, indicatorId, at.minus(setting.windowDays(), ChronoUnit.DAYS), at)
                        .stream()
                        .map(TrendRuleService::reading)
                        .flatMap(Optional::stream)
                        .toList();
        WeightLoss.detect(latest.get(), previous, setting.percent()).ifPresent(detection -> {
            if (!markers.mark(MARKER_KIND, "M01.RESIDENT", residentId, key, null)) return;
            alerts.requestAlert(new CreateAlertCommand(
                    residentId,
                    null,
                    WEIGHT_LOSS,
                    TypeLock.forTrend(WEIGHT_LOSS),
                    rule.get().level(),
                    at,
                    "CHI_SO",
                    recordId,
                    "Sụt cân " + detection.dropPercent() + "% trong " + setting.windowDays() + " ngày ("
                            + detection.highest().value().stripTrailingZeros().toPlainString() + " → "
                            + detection.latest().value().stripTrailingZeros().toPlainString() + ")",
                    null));
            events.publishEvent(
                    new WeightLossDetected(residentId, detection.highest().recordId(), recordId, key, at));
        });
    }

    /**
     * Mất ngủ kéo dài (FR-039c): ghi đêm của kết quả ngủ rồi đếm đêm liên tiếp thuộc nhóm mất ngủ lùi từ đêm này. Đính chính
     * của 005 phát lại sự kiện cùng mã kết quả nên dòng mới nhất của mỗi kết quả thắng.
     */
    @ApplicationModuleListener
    void onSleepResultRecordedEvent(SleepResultRecorded event) {
        onSleepResultRecorded(event);
    }

    /** Phần xử lý đồng bộ của {@link #onSleepResultRecordedEvent} (test gọi tuần tự). */
    @Transactional
    public void onSleepResultRecorded(SleepResultRecorded event) {
        var context = event.context();
        saveNight(event.resultId(), event.residentId(), event.date(), event.valueGroup(), false, context);
        var rule = enabled(PROLONGED_INSOMNIA);
        if (rule.isEmpty()) return;
        int needed = parameters.read(ParameterCode.CFG_M05_05).asInt();
        int nights = ProlongedInsomniaCounter.consecutiveNights(
                event.date(), currentNights(event.residentId()), Set.<LocalDate>of());
        if (needed <= 0 || nights < needed) return;
        String key = "INSOMNIA:" + event.resultId() + ":" + context.milestone();
        if (!markers.mark(MARKER_KIND, "M01.RESIDENT", event.residentId(), key, null)) return;
        alerts.requestAlert(new CreateAlertCommand(
                event.residentId(),
                context.floorAreaId().orElse(null),
                PROLONGED_INSOMNIA,
                TypeLock.forTrend(PROLONGED_INSOMNIA),
                rule.get().level(),
                context.occurredAt(),
                context.sourceType(),
                context.sourceId(),
                "Mất ngủ kéo dài " + nights + " đêm liên tiếp",
                null));
    }

    /** Kết quả ngủ bị Hủy ghi nhận không còn được tính (FR-037 của 005); không tạo cảnh báo. */
    @ApplicationModuleListener
    void onSleepResultVoidedEvent(SleepResultVoided event) {
        onSleepResultVoided(event);
    }

    @Transactional
    public void onSleepResultVoided(SleepResultVoided event) {
        saveNight(event.resultId(), event.residentId(), event.date(), "", true, event.context());
    }

    private void saveNight(
            long resultId,
            long residentId,
            LocalDate night,
            String group,
            boolean voided,
            com.eldercare.backend.care.api.dto.CareEventContext context) {
        String uniqueKey = "SLEEP_NIGHT:" + resultId + ":" + context.milestone();
        if (recordEvents.findByUniqueKey(SLEEP_NIGHT, uniqueKey).isPresent()) return;
        Map<String, Object> content = new HashMap<>();
        content.put("resultId", resultId);
        content.put("night", night.toString());
        content.put("group", group == null ? "" : group);
        content.put("voided", voided);
        recordEvents.add(
                SLEEP_NIGHT,
                new EntityRef("M01.RESIDENT", residentId),
                new NewRecordEvent<>(
                        content,
                        null,
                        null,
                        true,
                        context.occurredAt(),
                        context.occurredAt(),
                        context.occurredAt(),
                        context.floorAreaId().orElse(null),
                        uniqueKey));
    }

    private List<ProlongedInsomniaCounter.SleepResult> currentNights(long residentId) {
        Map<Long, RecordEvent<Map>> latest =
                recordEvents.list(SLEEP_NIGHT, new EntityRef("M01.RESIDENT", residentId)).stream()
                        .collect(Collectors.toMap(
                                e -> ((Number) e.content().get("resultId")).longValue(),
                                e -> e,
                                (a, b) -> b.occurredAt().isBefore(a.occurredAt()) ? a : b));
        return latest.values().stream()
                .filter(e -> !Boolean.TRUE.equals(e.content().get("voided")))
                .map(e -> new ProlongedInsomniaCounter.SleepResult(
                        LocalDate.parse((String) e.content().get("night")), e.occurredAt(), (String)
                                e.content().get("group")))
                .toList();
    }

    /** Bỏ lỡ lần đo theo lịch (FR-039b): khóa {@code BO_LO_LAN_DO:<chiSo>}; Bắt buộc thì Trung bình trở lên. */
    @ApplicationModuleListener
    public void onMeasurementTaskMissed(MeasurementTaskMissed event) {
        var rule = enabled(MISSED_MEASUREMENT);
        if (rule.isEmpty()) return;
        IncidentLevel level = rule.get().level();
        if (event.importance() == Importance.MANDATORY && level.ordinal() < IncidentLevel.MEDIUM.ordinal()) {
            level = IncidentLevel.MEDIUM;
        }
        for (long indicatorId : event.vitalSignIds()) {
            String key = "MISSED:" + event.taskId() + ":" + indicatorId;
            if (!markers.mark(MARKER_KIND, "M01.RESIDENT", event.residentId(), key, null)) continue;
            alerts.requestAlert(new CreateAlertCommand(
                    event.residentId(),
                    event.context().floorAreaId().orElse(null),
                    MISSED_MEASUREMENT,
                    TypeLock.forMissedMeasurement(indicatorId),
                    level,
                    event.context().occurredAt(),
                    event.context().sourceType(),
                    event.context().sourceId(),
                    "Bỏ lỡ lần đo theo lịch",
                    null));
        }
    }

    private Optional<TrendRuleView> enabled(String code) {
        return catalog.trendRules().stream()
                .filter(rule -> rule.code().equals(code) && rule.enabled())
                .findFirst();
    }

    private static Optional<WeightLoss.Reading> reading(VitalSignReadingView view) {
        if (view.values().size() != 1) return Optional.empty();
        BigDecimal value = view.values().values().iterator().next();
        return Optional.of(new WeightLoss.Reading(view.id(), value));
    }

    /** Giá trị CFG-M05-03: {@code {"percent":5,"window":{"value":30,"unit":"DAYS"}}}. */
    public record WeightLossSetting(BigDecimal percent, Window window) {
        public long windowDays() {
            return window.value();
        }

        public record Window(long value, String unit) {}
    }
}
