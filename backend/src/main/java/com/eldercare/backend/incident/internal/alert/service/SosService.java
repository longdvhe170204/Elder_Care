package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.SosTripView;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.api.spi.SosTripPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.SosPlace;
import com.eldercare.backend.incident.internal.alert.domain.SosPlaceContent;
import com.eldercare.backend.incident.internal.alert.domain.SosPressContent;
import com.eldercare.backend.incident.internal.alert.domain.SosStatusView;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nút SOS của người cao tuổi (US12; FR-026 (d), FR-047d → FR-047g, BR-M05-27; research S-23). Người cao tuổi đã được
 * {@code SosController} xác định từ token thiết bị (002 {@code BoundDeviceApi}); lệnh không cần phiên. Lần bấm ghi một
 * dòng {@code M05.SOS_PRESS} rồi tạo hoặc gộp cảnh báo Khẩn cấp khóa {@code SOS} qua đường tạo sẵn có (S-08). Không có
 * lệnh tắt nút (FR-047g).
 */
@Service
public class SosService {
    public static final String ALERT_TYPE = "RESIDENT_HELP_REQUEST";
    static final String PRESS_SOURCE = "M05.SOS_PRESS";
    static final SharedKind<SosPressContent> PRESS =
            SharedKind.of(PRESS_SOURCE, SosPressContent.class).notDeletable();
    static final SharedKind<SosPlaceContent> PLACE =
            SharedKind.of("M05.SOS_PLACE", SosPlaceContent.class).notDeletable();
    private static final Set<ResidentStatus> NOT_AT_FACILITY = Set.of(
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.HOSPITALIZED,
            ResidentStatus.RESIDENCY_ENDED,
            ResidentStatus.DECEASED,
            ResidentStatus.ADMISSION_CANCELLED);

    private final AlertService alerts;
    private final AlertStore store;
    private final RecordEventApi records;
    private final MonitoringLock monitoringLock;
    private final BusinessTime time;
    private final AlertResponsibility responsibility;
    private final ResidentApi residents;
    private final ContractApi contracts;
    private final DayCareAttendanceApi attendance;
    private final PressStatusViews views;
    private final Optional<ResidentLocationApi> locations;
    private final Optional<AlertStaffPort> staff;
    private final Optional<SosTripPort> trips;

    public SosService(
            AlertService alerts,
            AlertStore store,
            RecordEventApi records,
            MonitoringLock monitoringLock,
            BusinessTime time,
            AlertResponsibility responsibility,
            ResidentApi residents,
            ContractApi contracts,
            DayCareAttendanceApi attendance,
            PressStatusViews views,
            @Autowired(required = false) ResidentLocationApi locations,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) SosTripPort trips) {
        this.alerts = alerts;
        this.store = store;
        this.records = records;
        this.monitoringLock = monitoringLock;
        this.time = time;
        this.responsibility = responsibility;
        this.residents = residents;
        this.contracts = contracts;
        this.attendance = attendance;
        this.views = views;
        this.locations = Optional.ofNullable(locations);
        this.staff = Optional.ofNullable(staff);
        this.trips = Optional.ofNullable(trips);
    }

    /**
     * Một lần bấm (FR-047d, FR-047e). Cùng thiết bị gửi lại cùng {@code Idempotency-Key} thì trả trạng thái của lần bấm đã
     * ghi, không tăng số lần (SC-017). Điều kiện xét lúc nhận lệnh, theo thứ tự: không ở viện, rồi bán trú không Có mặt.
     */
    @Transactional
    public SosStatusView press(long residentId, long accountId, String deviceId, String idempotencyKey) {
        monitoringLock.lock(residentId);
        String uniqueKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? null
                : "SOS_PRESS:" + deviceId + ":" + idempotencyKey.strip();
        if (uniqueKey != null) {
            var earlier = records.findByUniqueKey(PRESS, uniqueKey);
            if (earlier.isPresent()) {
                long alertId = alertOf(earlier.get().id());
                return status(earlier.get().id(), store.findById(alertId).orElseThrow());
            }
        }
        Instant now = time.now();
        ResidentStatus residentStatus = residents.status(residentId);
        if (NOT_AT_FACILITY.contains(residentStatus)) {
            throw refused(HttpStatus.CONFLICT, "NOT_AT_FACILITY", "Người cao tuổi không ở viện, hãy gọi số liên hệ");
        }
        if (contracts.currentResidenceType(residentId, time.today()).orElse(null) == ResidenceType.DAY_CARE
                && attendance
                        .onDate(residentId, time.today())
                        .filter(day -> day.status() == AttendanceStatus.PRESENT)
                        .isEmpty()) {
            throw refused(HttpStatus.CONFLICT, "NOT_PRESENT", "Người cao tuổi không có mặt tại viện hôm nay");
        }

        Optional<LocationView> location = locations.flatMap(api -> api.current(residentId));
        Long floor = responsibility.floorOf(residentId, now).orElse(null);
        var press = records.add(
                PRESS,
                new EntityRef("M01.RESIDENT", residentId),
                new NewRecordEvent<>(
                        new SosPressContent(
                                residentId,
                                accountId,
                                deviceId,
                                idempotencyKey,
                                location.map(LocationView::roomId).orElse(null),
                                location.map(LocationView::bedId).orElse(null),
                                residentStatus.name()),
                        null,
                        accountId,
                        false,
                        now,
                        now,
                        now,
                        floor,
                        uniqueKey));

        // Hoạt động bên ngoài thì thêm trưởng đoàn, người đi cùng; xét lúc nhận lệnh (FR-047e)
        Optional<SosTripView> trip = residentStatus == ResidentStatus.OFF_SITE_ACTIVITY
                ? trips.flatMap(port -> port.ongoing(residentId))
                : Optional.empty();
        boolean noOnDutyDoctor =
                staff.map(port -> port.onDutyDoctors(now).isEmpty()).orElse(true);
        var alert = alerts.requestAlert(
                        new CreateAlertCommand(
                                residentId,
                                floor,
                                ALERT_TYPE,
                                TypeLock.forSos(),
                                IncidentLevel.CRITICAL,
                                now,
                                PRESS_SOURCE,
                                press.id(),
                                "Người cao tuổi bấm nút SOS",
                                null),
                        created -> SosNotices.created(created, noOnDutyDoctor, trip, now))
                .orElseThrow(() -> refused(
                        HttpStatus.CONFLICT, "NOT_AT_FACILITY", "Người cao tuổi không ở viện, hãy gọi số liên hệ"));
        if (alert.occurrenceCount() == 1 && noOnDutyDoctor) store.markNoOnDutyDoctor(alert.id());
        return status(press.id(), alert);
    }

    /** Cảnh báo SOS đang mở gần nhất của người cao tuổi; rỗng khi không có (FR-047f). */
    @Transactional(readOnly = true)
    public Optional<SosStatusView> current(long residentId) {
        return store.findOpenAlert(residentId, TypeLock.forSos()).map(alert -> status(latestPress(alert.id()), alert));
    }

    /** Chọn nhanh nơi đang ở, một lần cho mỗi lần bấm (FR-047f). Lần bấm của người khác coi như không có. */
    @Transactional
    public void place(long residentId, long pressId, SosPlace place) {
        RecordEvent<SosPressContent> press = records.require(PRESS, pressId);
        if (press.content().residentId() != residentId) throw new NotFoundException();
        String uniqueKey = "SOS_PLACE:" + pressId;
        if (records.findByUniqueKey(PLACE, uniqueKey).isPresent()) {
            throw new AlreadyProcessedException("Đã ghi nơi đang ở cho lần bấm này");
        }
        Instant now = time.now();
        records.add(
                PLACE,
                new EntityRef("M01.RESIDENT", residentId),
                new NewRecordEvent<>(
                        new SosPlaceContent(place),
                        null,
                        press.content().accountId(),
                        false,
                        now,
                        now,
                        now,
                        press.floorAreaId(),
                        uniqueKey));
    }

    /** Lỗi kèm số liên hệ của viện và 115 để app hiện (FR-047d, 012 FR-085). */
    public BusinessException refused(HttpStatus status, String code, String detail) {
        return views.refused(status, code, detail);
    }

    private SosStatusView status(long pressId, AlertRecord alert) {
        return views.of(pressId, alert);
    }

    private long alertOf(long pressId) {
        return store.bySource(PRESS_SOURCE, java.util.List.of(pressId)).stream()
                .findFirst()
                .orElseThrow()[1];
    }

    private long latestPress(long alertId) {
        return records.list(SharedKind.of("M05.ALERT_SOURCE", Map.class), new EntityRef("M05.ALERT", alertId)).stream()
                .filter(row -> PRESS_SOURCE.equals(row.content().get("sourceType")))
                .map(row -> ((Number) row.content().get("sourceId")).longValue())
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
