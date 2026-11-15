package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.service.TripPreparationService.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.correction.api.dto.*;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.care.api.dto.CareTimeRange;
import com.eldercare.backend.care.api.dto.TripInterval;
import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import com.eldercare.backend.care.api.events.ActivityCostSourceChanged;
import com.eldercare.backend.care.api.events.TripChanged;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.task.service.CareAbsenceTasks;
import com.eldercare.backend.care.internal.trip.domain.*;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 014 T020 (FR-031a, Q-170, Q-240; module-interfaces 014 mục 1.1 {@code DinhChinhDuocProvider} {@code M04.DIEM_DANH_CHUYEN}):
 * điểm danh chuyến chỉ Trưởng tầng của chuyến đính chính. Cài đổi thời điểm thực tế (a, sửa nội dung trường {@code at}; 001
 * {@code ResidentStatusApi.correctRollCallTime}, đổi api chủ dự án duyệt 2026-11-11), Hủy ghi nhận rời viện (b) và về (c,
 * lệnh 001 {@code CANCEL_RETURN_RECORD}).
 */
@Component
public class TripAttendanceCorrectionProvider implements CorrectableRecordProvider {
    private final RecordEventApi records;
    private final SimpleRecordApi simple;
    private final ActivityTripStore trips;
    private final ResidentStatusApi status;
    private final ObjectProvider<CurrentUser> user;
    private final ObjectProvider<TripMedicationPreparationPort> medications;
    private final ObjectProvider<VehicleScheduleCommandApi> vehicleCommands;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;
    private final BusinessTime time;
    private final TripAttendanceTimes attendanceTimes;
    private final CareAbsenceTasks absence;

    public TripAttendanceCorrectionProvider(
            RecordEventApi records,
            SimpleRecordApi simple,
            ActivityTripStore trips,
            ResidentStatusApi status,
            ObjectProvider<CurrentUser> user,
            ObjectProvider<TripMedicationPreparationPort> medications,
            ObjectProvider<VehicleScheduleCommandApi> vehicleCommands,
            ApplicationEventPublisher events,
            JsonMapper json,
            BusinessTime time,
            TripAttendanceTimes attendanceTimes,
            CareAbsenceTasks absence) {
        this.records = records;
        this.simple = simple;
        this.trips = trips;
        this.status = status;
        this.user = user;
        this.medications = medications;
        this.vehicleCommands = vehicleCommands;
        this.events = events;
        this.json = json;
        this.time = time;
        this.attendanceTimes = attendanceTimes;
        this.absence = absence;
    }

    @Override
    public String recordType() {
        return TripKinds.ATTENDANCE.code();
    }

    private RecordEventRow row(long id) {
        var e = records.require(TripKinds.ATTENDANCE, id);
        return new RecordEventRow(e.content(), e.actorId(), e.floorAreaId(), e.confirmedAt() != null);
    }

    private record RecordEventRow(TripKinds.Attendance content, Long actorId, Long floorAreaId, boolean confirmed) {}

    @Override
    public boolean isConfirmed(long id) {
        try {
            return row(id).confirmed();
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public long originalRecorder(long id) {
        var actor = row(id).actorId();
        return actor == null ? 0 : actor;
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.ofNullable(row(id).floorAreaId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(row(id).content().residentId());
    }

    /** FR-031a: chỉ Trưởng tầng; tầng của chuyến do khung 000 xét qua {@link #floorArea}. */
    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.FLOOR_MANAGER);
    }

    @Override
    public boolean supports(CorrectionType type) {
        return type == CorrectionType.VOID || type == CorrectionType.AMEND_CONTENT;
    }

    /** FR-031a: không đổi người cao tuổi hay loại bản ghi, nên nội dung sửa chỉ có thời điểm thực tế {@code at}. */
    @Override
    public void validateContent(Object correctedContent) {
        correctedAt(correctedContent);
    }

    private Instant correctedAt(Object content) {
        Map<?, ?> map;
        try {
            map = json.convertValue(content, Map.class);
        } catch (RuntimeException e) {
            map = null;
        }
        if (map == null
                || !map.keySet().equals(Set.of(TripAttendanceTimes.AT))
                || map.get(TripAttendanceTimes.AT) == null)
            throw new InvalidValueException("Đính chính điểm danh chuyến chỉ sửa được thời điểm thực tế (trường at)");
        try {
            return Instant.parse(map.get(TripAttendanceTimes.AT).toString());
        } catch (java.time.format.DateTimeParseException e) {
            throw new InvalidValueException("Thời điểm thực tế không hợp lệ");
        }
    }

    @Override
    public Object originalContent(long id) {
        var c = row(id).content();
        var map = new LinkedHashMap<String, Object>();
        map.put("tripId", c.tripId());
        map.put("residentId", c.residentId());
        map.put("type", c.type());
        map.put("at", c.at().toString());
        return map;
    }

    /**
     * Kiểm trước khi lập bản đính chính, để cách C không lập yêu cầu không áp dụng được. Hàm không biết kiểu đính chính nên
     * cho qua khi bản ghi còn đổi được thời điểm hoặc còn Hủy ghi nhận được; lúc áp dụng kiểm đúng kiểu.
     */
    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        var a = row(id).content();
        try {
            checkAmendable(a, id);
        } catch (BusinessException e) {
            check(a);
        }
        return true;
    }

    /**
     * FR-031a (a): bản ghi là lần rời viện hoặc về mới nhất của đăng ký, chưa bị Hủy ghi nhận (người tham gia còn ở trạng
     * thái của bản ghi), chuyến Đang đi hoặc Đã về.
     */
    private void checkAmendable(TripKinds.Attendance a, long id) {
        boolean ret = "RETURN".equals(a.type());
        var trip = trips.find(a.tripId()).orElseThrow(NotFoundException::new);
        if (!Set.of("IN_PROGRESS", "RETURNED").contains(trip.status()))
            throw conflict("STATE_CHANGED", "Chỉ đính chính thời điểm khi chuyến Đang đi hoặc Đã về");
        var p = trips.participants(a.tripId()).stream()
                .filter(r -> r.registrationId() == a.registrationId())
                .findFirst()
                .orElseThrow(() -> conflict("STATE_CHANGED", "Đăng ký của bản ghi điểm danh đã được thay"));
        var allowed = ret ? Set.of("RETURNED") : Set.of("DEPARTED", "MISSING_ON_RETURN", "RETURNED");
        boolean latest = trips.latestAttendanceId(a.registrationId(), a.type())
                .map(latestId -> latestId == id)
                .orElse(false);
        if (!allowed.contains(p.status()) || !latest)
            throw conflict("STATE_CHANGED", "Bản ghi điểm danh đã bị Hủy ghi nhận hoặc đã có bản ghi mới hơn");
    }

    /**
     * FR-031a (a): 001 ghi thời điểm đúng vào lịch sử trạng thái; 006 nhận khoảng mang thuốc mới, 005 tính lại công việc
     * trong khoảng vắng (hủy việc chưa tới hạn trong khoảng mới, báo lập lại việc đã hủy nay ngoài khoảng), 011 qua
     * {@link TripChanged}. Trạng thái người cao tuổi và người tham gia không đổi.
     */
    private void amendTime(CorrectionView correction, TripKinds.Attendance a, Optional<Reason> reason) {
        long id = correction.originalRecordId();
        boolean ret = "RETURN".equals(a.type());
        Instant at = correctedAt(correction.correctedContent());
        checkAmendable(a, id);
        Instant previous = status.correctRollCallTime(
                a.residentId(),
                ret ? StatusCommand.ROLL_CALL_RETURN : StatusCommand.ROLL_CALL_DEPARTURE,
                a.tripId(),
                at,
                user.getObject(),
                reason);
        var trip = trips.lock(a.tripId());
        checkAmendable(a, id);
        var medication = medications.getIfAvailable();
        if (medication == null) throw unavailable("Chưa có nguồn thuốc để tính lại khoảng mang theo");
        Instant departed = ret
                ? attendanceTimes
                        .departedAt(a.registrationId())
                        .orElse(trip.plan().plannedDepartureAt())
                : at;
        Optional<Instant> returned = ret ? Optional.of(at) : returnedAt(a);
        medication.recalculateInterval(
                a.tripId(),
                a.residentId(),
                new TripInterval(departed, returned.orElse(trip.plan().plannedReturnAt())));
        var before = ret ? new CareTimeRange(departed, previous) : new CareTimeRange(previous, returned.orElse(null));
        var after = new CareTimeRange(departed, returned.orElse(null));
        absence.recalculate(a.residentId(), Optional.of(before), Optional.of(after));
        events.publishEvent(new TripChanged(
                a.tripId(),
                "CHANGED",
                trip.plan().facilityVehicle(),
                trip.plan().plannedDepartureAt(),
                trip.plan().plannedReturnAt(),
                ret ? Optional.of(at) : Optional.empty(),
                "TRIP_ATTENDANCE_TIME_CORRECTED:" + correction.id()));
    }

    /** Thời điểm về hiện hành khi người tham gia đã về. */
    private Optional<Instant> returnedAt(TripKinds.Attendance a) {
        boolean back = trips.participants(a.tripId()).stream()
                .anyMatch(p -> p.registrationId() == a.registrationId() && "RETURNED".equals(p.status()));
        return back ? attendanceTimes.returnedAt(a.registrationId()) : Optional.empty();
    }

    private TripParticipant check(TripKinds.Attendance a) {
        boolean ret = "RETURN".equals(a.type());
        var trip = trips.find(a.tripId()).orElseThrow(NotFoundException::new);
        if (!"IN_PROGRESS".equals(trip.status()))
            throw conflict("STATE_CHANGED", "Chỉ Hủy ghi nhận điểm danh khi chuyến chưa Đã về");
        var p = trips.participants(a.tripId()).stream()
                .filter(r -> r.registrationId() == a.registrationId())
                .findFirst()
                .orElseThrow(() -> conflict("STATE_CHANGED", "Đăng ký của bản ghi điểm danh đã được thay"));
        if (ret
                ? !"RETURNED".equals(p.status())
                : !Set.of("DEPARTED", "MISSING_ON_RETURN").contains(p.status()))
            throw conflict(
                    "STATE_CHANGED",
                    ret ? "Người cao tuổi không còn ở Đã về" : "Người cao tuổi đã có bản ghi về hoặc đã rời đoàn");
        return p;
    }

    /**
     * FR-031a (c): ghi về nhầm người chưa về; 001 đưa người về Hoạt động bên ngoài (lệnh {@code CANCEL_RETURN_RECORD}, đổi
     * api duyệt 2026-11-11), người tham gia về Đã rời viện; 005, 006, 011 nhận qua lệnh trạng thái và {@link TripChanged}.
     */
    private void voidReturn(CorrectionView correction, TripKinds.Attendance a, Optional<Reason> reason) {
        status.execute(
                a.residentId(),
                StatusCommand.CANCEL_RETURN_RECORD,
                user.getObject(),
                reason,
                json.createObjectNode().put("tripId", a.tripId()).put("correctionId", correction.id()),
                new Basis(null, null, a.tripId(), null));
        var trip = trips.lock(a.tripId());
        check(a);
        if (!trips.undoReturn(a.registrationId()))
            throw conflict("STATE_CHANGED", "Người tham gia vừa thay đổi, tải lại để đính chính");
        events.publishEvent(new TripChanged(
                a.tripId(),
                "CHANGED",
                trip.plan().facilityVehicle(),
                trip.plan().plannedDepartureAt(),
                trip.plan().plannedReturnAt(),
                Optional.empty(),
                "TRIP_RETURN_VOIDED:" + correction.id()));
    }

    /**
     * FR-031a (b): 001 đưa người về Đang lưu trú (căn cứ đính chính điểm danh), người tham gia về Được đi, 006 nhận lại khoảng
     * dự kiến, 010 hủy nguồn tính phí của bản ghi, 011 qua {@link TripChanged}; không còn ai rời viện thì chuyến về Đã lên
     * lịch và lịch xe về Đã đặt (Q-240). Báo riêng 005 lập lại công việc chưa nối, như lệnh rời viện.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void afterCorrection(CorrectionView correction) {
        var a = row(correction.originalRecordId()).content();
        var reason = correction.reason() == null || correction.reason().isBlank()
                ? Optional.<Reason>empty()
                : Optional.of(new Reason(correction.reason()));
        if (correction.type() == CorrectionType.AMEND_CONTENT) {
            amendTime(correction, a, reason);
            return;
        }
        check(a);
        if ("RETURN".equals(a.type())) {
            voidReturn(correction, a, reason);
            return;
        }
        status.execute(
                a.residentId(),
                StatusCommand.CANCEL_DEPARTURE_RECORD,
                user.getObject(),
                reason,
                json.createObjectNode().put("tripId", a.tripId()).put("correctionId", correction.id()),
                new Basis(null, null, a.tripId(), null));
        var trip = trips.lock(a.tripId());
        check(a);
        var medication = medications.getIfAvailable();
        if (medication == null) throw unavailable("Chưa có nguồn thuốc để tính lại khoảng mang theo");
        if (!trips.undoDeparture(a.registrationId()))
            throw conflict("STATE_CHANGED", "Người tham gia vừa thay đổi, tải lại để đính chính");
        // Còn được rời viện sau (trước giờ về dự kiến, FR-042) thì về Được đi, ngược lại Không đi.
        if (time.now().isBefore(trip.plan().plannedReturnAt()))
            medication.recalculateInterval(
                    a.tripId(),
                    a.residentId(),
                    new TripInterval(
                            trip.plan().plannedDepartureAt(), trip.plan().plannedReturnAt()));
        else {
            trips.notGoing(a.registrationId(), "Hủy ghi nhận rời viện sau giờ về dự kiến");
            medication.onTripChanged(a.tripId(), a.residentId(), TripMedicationChangeType.NOT_ATTENDING);
        }
        if (simple.require(ActivityKinds.ACTIVITIES, trip.activityId())
                .content()
                .charged()) {
            String source = Long.toString(correction.originalRecordId());
            events.publishEvent(new ActivityCostSourceChanged(
                    "ACTIVITY_ATTENDANCE",
                    source,
                    "OCCURRENCE",
                    Optional.empty(),
                    true,
                    "CORRECTION:" + correction.id(),
                    "ACTIVITY_ATTENDANCE:" + source));
        }
        boolean last = !trips.anyoneDeparted(a.tripId());
        if (last) {
            trips.unstart(a.tripId());
            if (trip.plan().facilityVehicle()) {
                var vehicle = vehicleCommands.getIfAvailable();
                if (vehicle == null) throw unavailable("Chưa có nguồn cập nhật lịch xe");
                vehicle.followTrip(
                        a.tripId(),
                        TripVehicleChangeType.LAST_DEPARTURE_UNDONE,
                        trip.plan().plannedDepartureAt(),
                        trip.plan().plannedReturnAt(),
                        Optional.empty());
            }
        }
        events.publishEvent(new TripChanged(
                a.tripId(),
                last ? "LAST_DEPARTURE_UNDONE" : "CHANGED",
                trip.plan().facilityVehicle(),
                trip.plan().plannedDepartureAt(),
                trip.plan().plannedReturnAt(),
                Optional.empty(),
                "TRIP_DEPARTURE_VOIDED:" + correction.id()));
    }
}
