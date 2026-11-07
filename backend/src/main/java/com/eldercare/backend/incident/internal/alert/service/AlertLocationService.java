package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.AlertLocationContent;
import com.eldercare.backend.incident.internal.alert.domain.AlertLocationReport;
import com.eldercare.backend.incident.internal.alert.domain.AlertLocationView;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vị trí GPS của cảnh báo từ nút SOS, nút hỗ trợ khẩn cấp (FR-047h, BR-M05-28; research S-24; T096): nhận mỗi lần app gửi
 * khi cảnh báo đang mở và chưa ghi đã tới nơi, lưu từng lần; trả tọa độ mới nhất cho nhân viên có quyền xem cảnh báo.
 * Vị trí không đổi người nhận và không có trong DTO, thông báo cho người thân.
 */
@Service
public class AlertLocationService {
    static final SharedKind<AlertLocationContent> KIND =
            SharedKind.of("M05.ALERT_LOCATION", AlertLocationContent.class).notDeletable();
    static final String STOP_SENDING = "STOP_SENDING_LOCATION";
    private static final Set<AlertStatus> OPEN =
            Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS);
    private static final BigDecimal LAT = BigDecimal.valueOf(90);
    private static final BigDecimal LNG = BigDecimal.valueOf(180);

    private final AlertStore alerts;
    private final RecordEventApi records;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final Optional<ResidentApi> residents;

    public AlertLocationService(
            AlertStore alerts,
            RecordEventApi records,
            ParameterApi parameters,
            BusinessTime time,
            @Autowired(required = false) ResidentApi residents) {
        this.alerts = alerts;
        this.records = records;
        this.parameters = parameters;
        this.time = time;
        this.residents = Optional.ofNullable(residents);
    }

    /** {@code POST /sos/{id}/location}: lần bấm của người cao tuổi khác coi như không có. */
    @Transactional
    public void reportSos(long residentId, long pressId, AlertLocationReport report) {
        var press = records.require(SosService.PRESS, pressId);
        if (press.content().residentId() != residentId) throw new NotFoundException();
        long alertId = alerts.bySource(SosService.PRESS_SOURCE, List.of(pressId)).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new)[1];
        record(
                alerts.findById(alertId).orElseThrow(NotFoundException::new),
                "SOS",
                pressId,
                press.content().accountId(),
                report);
    }

    /** {@code POST /emergency-assist/{id}/location} ({@code id} là cảnh báo như {@code /details}): chỉ người bấm gửi. */
    @Transactional
    public void reportAssist(long alertId, AlertLocationReport report, CurrentUser user) {
        var alert = alerts.findById(alertId).orElseThrow(NotFoundException::new);
        if (!EmergencyAssistService.ALERT_TYPE.equals(alert.type())
                || !Long.valueOf(user.accountId()).equals(alert.requesterId())) {
            throw new NotFoundException();
        }
        record(alert, "ASSIST", latestPress(alertId, EmergencyAssistService.PRESS_SOURCE), user.accountId(), report);
    }

    /** App gửi vị trí khi cảnh báo đang mở và chưa ghi đã tới nơi (FR-047h, FR-047j). */
    boolean sendLocation(AlertRecord alert) {
        return OPEN.contains(alert.status()) && alert.arrivedAt() == null;
    }

    /** CFG-M05-09 tính bằng giây. */
    int intervalSeconds() {
        return (int) interval().toSeconds();
    }

    /** Tọa độ mới nhất của cảnh báo; rỗng khi app chưa gửi lần nào. */
    @Transactional(readOnly = true)
    public Optional<AlertLocationView> latest(AlertRecord alert) {
        var rows = records.list(KIND, ref(alert.id()));
        if (rows.isEmpty()) return Optional.empty();
        var latest = rows.getLast().content();
        boolean stale = latest.measuredAt().isBefore(time.now().minus(interval().multipliedBy(2)));
        boolean primary = alert.residentId() != null
                && residents.map(api -> api.status(alert.residentId())).orElse(null)
                        == ResidentStatus.OFF_SITE_ACTIVITY;
        return Optional.of(new AlertLocationView(
                latest.latitude(), latest.longitude(), latest.accuracyMeters(), latest.measuredAt(), stale, primary));
    }

    private void record(AlertRecord alert, String pressType, Long pressId, long actorId, AlertLocationReport report) {
        validate(report);
        if (!sendLocation(alert)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    STOP_SENDING,
                    "Cảnh báo đã có người tới nơi hoặc đã kết thúc, dừng gửi vị trí");
        }
        Instant now = time.now();
        records.add(
                KIND,
                ref(alert.id()),
                new NewRecordEvent<>(
                        new AlertLocationContent(
                                pressType,
                                pressId,
                                report.latitude(),
                                report.longitude(),
                                report.accuracyMeters(),
                                report.measuredAt()),
                        null,
                        actorId,
                        false,
                        now,
                        report.measuredAt(),
                        now,
                        alert.floorAreaId(),
                        null));
    }

    private static void validate(AlertLocationReport report) {
        if (report == null
                || report.latitude() == null
                || report.longitude() == null
                || report.accuracyMeters() == null
                || report.measuredAt() == null) {
            throw new InvalidValueException("Thiếu vĩ độ, kinh độ, sai số hoặc thời điểm đo");
        }
        if (report.latitude().abs().compareTo(LAT) > 0
                || report.longitude().abs().compareTo(LNG) > 0
                || report.accuracyMeters().signum() < 0) {
            throw new InvalidValueException("Vĩ độ trong [-90, 90], kinh độ trong [-180, 180], sai số không âm");
        }
    }

    private Duration interval() {
        return parameters.read(ParameterCode.CFG_M05_09).asDuration();
    }

    private long latestPress(long alertId, String pressSource) {
        return records.list(SharedKind.of("M05.ALERT_SOURCE", Map.class), ref(alertId)).stream()
                .filter(row -> pressSource.equals(row.content().get("sourceType")))
                .map(row -> ((Number) row.content().get("sourceId")).longValue())
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private static EntityRef ref(long alertId) {
        return new EntityRef(AlertNotices.SOURCE_TYPE, alertId);
    }
}
