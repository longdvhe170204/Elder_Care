package com.eldercare.backend.incident.internal.trend.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.events.MeasurementTaskMissed;
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
import com.eldercare.backend.incident.internal.trend.domain.WeightLoss;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Cảnh báo theo xu hướng (FR-039 → FR-039b, BR-M05-04; research S-10): chạy khi dữ liệu nguồn đổi, không quét. Quy tắc
 * tắt thì không tạo; bật lại không xét bù. Mất ngủ kéo dài (FR-039c) chờ API đọc kết quả ngủ theo khoảng của 005; từ chối
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

    private final IncidentCatalogService catalog;
    private final ParameterApi parameters;
    private final VitalSignApi vitalSigns;
    private final AlertService alerts;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;

    public TrendRuleService(
            IncidentCatalogService catalog,
            ParameterApi parameters,
            VitalSignApi vitalSigns,
            AlertService alerts,
            ReminderMarkers markers,
            ApplicationEventPublisher events) {
        this.catalog = catalog;
        this.parameters = parameters;
        this.vitalSigns = vitalSigns;
        this.alerts = alerts;
        this.markers = markers;
        this.events = events;
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
