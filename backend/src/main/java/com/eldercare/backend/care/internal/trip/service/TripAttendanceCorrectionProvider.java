package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.service.TripPreparationService.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.correction.api.dto.*;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.care.api.dto.TripInterval;
import com.eldercare.backend.care.api.dto.TripMedicationChangeType;
import com.eldercare.backend.care.api.events.ActivityCostSourceChanged;
import com.eldercare.backend.care.api.events.TripChanged;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
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
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 014 T020 (FR-031a, Q-170, Q-240; module-interfaces 014 mục 1.1 {@code DinhChinhDuocProvider} {@code M04.DIEM_DANH_CHUYEN}):
 * điểm danh chuyến chỉ Trưởng tầng của chuyến đính chính. Lượt này cài Hủy ghi nhận rời viện (b). Đổi thời điểm thực tế
 * (a) cần 001 sửa thời điểm lịch sử trạng thái, Hủy ghi nhận về (c) cần lệnh 001 đưa người về lại Hoạt động bên ngoài; hai
 * cách này bị từ chối cho tới khi 001 có (plan Phase 7).
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
            BusinessTime time) {
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
        return type == CorrectionType.VOID;
    }

    @Override
    public void validateContent(Object correctedContent) {
        throw new InvalidValueException("Điểm danh chuyến chỉ Hủy ghi nhận được trong lượt này");
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

    /** Kiểm trước khi lập bản đính chính, để cách C không lập yêu cầu không áp dụng được. */
    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        check(row(id).content());
        return true;
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
        check(a);
        var reason = correction.reason() == null || correction.reason().isBlank()
                ? Optional.<Reason>empty()
                : Optional.of(new Reason(correction.reason()));
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
