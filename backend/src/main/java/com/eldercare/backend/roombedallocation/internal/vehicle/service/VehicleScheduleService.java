package com.eldercare.backend.roombedallocation.internal.vehicle.service;

import static com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.VehicleScheduleApi;
import com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import com.eldercare.backend.roombedallocation.api.dto.VehicleScheduleChangeCheck;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetRow;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleBookingCheck;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleScheduleInput;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleScheduleNotice;
import com.eldercare.backend.roombedallocation.internal.vehicle.persistence.VehicleScheduleStore;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T022 (FR-017 → FR-019, FR-020, FR-021; BR-M03-16, DBR-32; Q-220, Q-236, Q-239, Q-240, Q-295, Q-298; research
 * W-07): lịch xe ở {@code simple_record} loại {@code M03.VEHICLE_SCHEDULE}. Đặt lịch, dời giờ kiểm dưới khóa dòng tài sản
 * xe nên hai lệnh cùng xe chạy tuần tự (SC-005). Trưởng tầng, Hành chính, Quản lý viện đặt lịch; người đặt, Quản lý viện
 * làm lệnh trên lịch. Trưởng tầng có phạm vi ASSIGNED mà lịch xe toàn viện không gắn tầng, nên lệnh tự kiểm quyền theo
 * vai trò và khóa quyền ({@code RequiresPermissionArchTest.SELF_CHECKED}). Bản cài {@link VehicleScheduleApi},
 * {@link VehicleScheduleCommandApi} cho chuyến đi của 014.
 */
@Service
public class VehicleScheduleService implements VehicleScheduleApi, VehicleScheduleCommandApi {
    static final Set<String> ACTIVE = Set.of(BOOKED, IN_USE);
    private static final Set<String> PURPOSES = Set.of("TRIP", "MEDICAL_VISIT", "NON_EMERGENCY_TRANSFER", "OTHER");
    private static final Set<Role> BOOKERS = Set.of(Role.FLOOR_MANAGER, Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER);

    private final SimpleRecordApi records;
    private final AssetStore assets;
    private final CatalogApi catalog;
    private final PermissionApi permissions;
    private final CurrentUser user;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final VehicleScheduleStore store;

    public VehicleScheduleService(
            SimpleRecordApi records,
            AssetStore assets,
            CatalogApi catalog,
            PermissionApi permissions,
            CurrentUser user,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            VehicleScheduleStore store) {
        this.records = records;
        this.assets = assets;
        this.catalog = catalog;
        this.permissions = permissions;
        this.user = user;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.store = store;
    }

    // ---- Lệnh của người --------------------------------------------------------------------------------------------

    /** FR-017: đặt lịch; chặn chồng giờ, xe không sẵn sàng, quá hạn đăng kiểm, bảo hiểm tại ngày đi. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long book(VehicleScheduleInput.Booking in) {
        if (BOOKERS.stream().noneMatch(user.roles()::contains) || !has(Operation.CREATE))
            throw new PermissionDeniedException("Chỉ Trưởng tầng, Hành chính, Quản lý viện đặt lịch xe");
        long staff = staff();
        if (in.vehicleId() == null
                || in.plannedDepartureAt() == null
                || in.plannedReturnAt() == null
                || in.driverStaffId() == null
                || in.purpose() == null
                || !PURPOSES.contains(in.purpose()))
            throw new InvalidValueException("Cần xe, giờ đi, giờ về, mục đích, người lái");
        if (!in.plannedReturnAt().isAfter(in.plannedDepartureAt()))
            throw new InvalidValueException("Giờ về phải sau giờ đi");
        if ("TRIP".equals(in.purpose()) != (in.tripId() != null))
            throw new InvalidValueException("Lịch của chuyến đi cần tripId; mục đích khác không gắn chuyến");
        var vehicle = lockVehicle(in.vehicleId());
        if (in.tripId() != null
                && records.findByUniqueKey(KIND, tripKey(in.tripId())).isPresent())
            throw conflict("STATE_CHANGED", "Chuyến đi đã có lịch xe chưa hủy");
        requireBookable(vehicle, lockedOthers(in.vehicleId(), null), in.plannedDepartureAt(), in.plannedReturnAt());
        var content = new VehicleSchedule(
                in.vehicleId(),
                in.plannedDepartureAt(),
                in.plannedReturnAt(),
                in.purpose(),
                in.tripId(),
                in.residentId(),
                in.driverStaffId(),
                staff,
                blank(in.note()) ? null : in.note().strip(),
                null,
                null,
                null);
        var row = records.create(
                KIND,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef(AssetKinds.ENTITY, in.vehicleId()),
                        BOOKED,
                        content,
                        null,
                        null,
                        in.tripId() == null ? null : tripKey(in.tripId()),
                        user.accountId(),
                        false));
        record(row.id(), "BOOK", null, BOOKED, null);
        return row.id();
    }

    /** FR-018: dời giờ lịch Đã đặt, kiểm FR-017 với giờ mới (không tính chính lịch này). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void reschedule(long id, VehicleScheduleInput.Command in) {
        if (in.plannedDepartureAt() == null || in.plannedReturnAt() == null)
            throw new InvalidValueException("Cần giờ đi, giờ về mới");
        var row = actorRow(id);
        moveTimes(row, in.plannedDepartureAt(), in.plannedReturnAt());
    }

    /** FR-018: hủy lịch Đã đặt, bắt buộc lý do; báo người đặt, Phụ trách bộ phận quản lý xe, người lái (Q-298). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void cancel(long id, VehicleScheduleInput.Command in) {
        var reason = new Reason(in.reason());
        var row = actorRow(id);
        cancelRow(row, reason.value());
    }

    /** FR-018: ghi xuất phát (Đã đặt → Đang dùng) với thời điểm thực tế. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void recordDeparture(long id, VehicleScheduleInput.Command in) {
        var row = actorRow(id);
        depart(row, in.occurredAt() == null ? time.now() : in.occurredAt());
    }

    /** FR-018: ghi trả xe (Đang dùng → Đã hoàn thành) với thời điểm thực tế. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void recordReturn(long id, VehicleScheduleInput.Command in) {
        var row = actorRow(id);
        finish(row, in.occurredAt() == null ? time.now() : in.occurredAt());
    }

    /** Lịch theo xe, khoảng [from, to) (mỗi đầu có thể rỗng); người có quyền xem tài sản, lịch xe. */
    @Transactional(readOnly = true)
    public List<SimpleRecord<VehicleSchedule>> list(Long vehicleId, Instant from, Instant to) {
        if (!has(Operation.VIEW)) throw new PermissionDeniedException("Không có quyền xem lịch xe");
        var rows = vehicleId == null
                ? records.listByStatus(KIND, Set.of(BOOKED, IN_USE, COMPLETED, CANCELLED))
                : records.listByParent(KIND, new EntityRef(AssetKinds.ENTITY, vehicleId));
        return rows.stream()
                .filter(r -> to == null || r.content().plannedDepartureAt().isBefore(to))
                .filter(r -> from == null || r.content().plannedReturnAt().isAfter(from))
                .sorted(Comparator.comparing(
                                (SimpleRecord<VehicleSchedule> r) -> r.content().plannedDepartureAt())
                        .thenComparing(SimpleRecord::id))
                .toList();
    }

    // ---- Cho 014 ---------------------------------------------------------------------------------------------------

    /** FR-019: lịch Đã đặt hoặc Đang dùng của chuyến bao trùm giờ rời dự kiến (chủ dự án chốt 2026-11-06). */
    @Override
    @Transactional(readOnly = true)
    public boolean hasBookedSchedule(long tripId, Instant plannedDepartureAt) {
        return records.findByUniqueKey(KIND, tripKey(tripId))
                .filter(r -> ACTIVE.contains(r.status()))
                .filter(r -> !plannedDepartureAt.isBefore(r.content().plannedDepartureAt())
                        && plannedDepartureAt.isBefore(r.content().plannedReturnAt()))
                .isPresent();
    }

    /** Q-239: giờ mới của chuyến có dời được lịch xe không; chuyến chưa có lịch thì được. */
    @Override
    @Transactional(readOnly = true)
    public VehicleScheduleChangeCheck checkChange(long tripId, Instant newDepartureAt, Instant newReturnAt) {
        var row = records.findByUniqueKey(KIND, tripKey(tripId)).filter(r -> ACTIVE.contains(r.status()));
        if (row.isEmpty()) return new VehicleScheduleChangeCheck(true, List.of());
        var vehicle = assets.find(row.get().content().vehicleId()).orElseThrow(NotFoundException::new);
        var unmet = VehicleBookingCheck.check(
                facts(vehicle), others(vehicle.id(), row.get().id()), newDepartureAt, newReturnAt);
        return new VehicleScheduleChangeCheck(
                unmet.isEmpty(), unmet.stream().map(UnmetCondition::description).toList());
    }

    /** FR-018: lịch của chuyến đi theo lệnh của 014, cùng transaction; chuyến không có lịch thì bỏ qua. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void followTrip(
            long tripId,
            TripVehicleChangeType type,
            Instant plannedDepartureAt,
            Instant plannedReturnAt,
            Optional<Instant> actualAt) {
        var found = records.findByUniqueKey(KIND, tripKey(tripId));
        if (found.isEmpty()) return;
        var row = records.lockForUpdate(KIND, found.get().id());
        switch (type) {
            case CHANGED -> moveTimes(row, plannedDepartureAt, plannedReturnAt);
            case CANCELLED -> cancelRow(row, "Chuyến đi bị hủy");
            case FIRST_DEPARTURE -> depart(row, actualAt.orElse(time.now()));
            case EXTENDED -> extend(row, plannedReturnAt);
            case LAST_DEPARTURE_UNDONE -> undoDeparture(row);
            case RETURN_FINISHED -> finish(row, actualAt.orElse(time.now()));
        }
    }

    // ---- Chuyển trạng thái -----------------------------------------------------------------------------------------

    private void moveTimes(SimpleRecord<VehicleSchedule> row, Instant from, Instant to) {
        requireStatus(row, BOOKED);
        if (!to.isAfter(from)) throw new InvalidValueException("Giờ về phải sau giờ đi");
        var vehicle = lockVehicle(row.content().vehicleId());
        requireBookable(vehicle, lockedOthers(vehicle.id(), row.id()), from, to);
        var current = records.lockForUpdate(KIND, row.id());
        change(current, BOOKED, current.content().withTimes(from, to), current.uniqueKey(), false);
        record(row.id(), "RESCHEDULE", BOOKED, BOOKED, null);
        notify(row.id(), current.content(), vehicle, "VEHICLE_SCHEDULE_CHANGED", "Lịch xe đã dời giờ");
    }

    private void cancelRow(SimpleRecord<VehicleSchedule> row, String reason) {
        requireStatus(row, BOOKED);
        change(row, CANCELLED, row.content().cancelled(reason), null, true);
        record(row.id(), "CANCEL", BOOKED, CANCELLED, new Reason(reason));
        var vehicle = assets.find(row.content().vehicleId()).orElseThrow(NotFoundException::new);
        notify(row.id(), row.content(), vehicle, "VEHICLE_SCHEDULE_CANCELLED", "Lịch xe đã hủy: " + reason);
    }

    private void depart(SimpleRecord<VehicleSchedule> row, Instant at) {
        requireStatus(row, BOOKED);
        change(row, IN_USE, row.content().departed(at), row.uniqueKey(), false);
        record(row.id(), "RECORD_DEPARTURE", BOOKED, IN_USE, null);
    }

    /** Q-240: mọi bản ghi rời viện của chuyến bị Hủy ghi nhận: về Đã đặt, bỏ thời điểm xuất phát. */
    private void undoDeparture(SimpleRecord<VehicleSchedule> row) {
        requireStatus(row, IN_USE);
        change(row, BOOKED, row.content().departed(null), row.uniqueKey(), false);
        record(row.id(), "UNDO_DEPARTURE", IN_USE, BOOKED, null);
    }

    private void finish(SimpleRecord<VehicleSchedule> row, Instant at) {
        requireStatus(row, IN_USE);
        change(row, COMPLETED, row.content().returned(at), row.uniqueKey(), true);
        record(row.id(), "RECORD_RETURN", IN_USE, COMPLETED, null);
    }

    /** Q-236: gia hạn giờ về không chặn khi chồng; chồng thì báo Quản lý viện và người đặt lịch kế tiếp. */
    private void extend(SimpleRecord<VehicleSchedule> row, Instant newReturnAt) {
        requireStatus(row, IN_USE);
        var c = row.content();
        lockVehicle(c.vehicleId());
        change(row, IN_USE, c.withTimes(c.plannedDepartureAt(), newReturnAt), row.uniqueKey(), false);
        record(row.id(), "EXTEND", IN_USE, IN_USE, null);
        var overlapping = VehicleBookingCheck.overlaps(
                lockedOthers(c.vehicleId(), row.id()), c.plannedDepartureAt(), newReturnAt);
        if (overlapping.isEmpty()) return;
        var recipients =
                new ArrayList<RecipientGroup>(List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));
        for (var slot : overlapping)
            recipients.add(new RecipientGroup.Staff(
                    records.require(KIND, slot.id()).content().bookedByStaffId(), "VEHICLE_SCHEDULE_BOOKER"));
        events.publishEvent(new VehicleScheduleNotice(
                row.id(),
                List.of(request(
                        "VEHICLE_SCHEDULE_EXTENDED_OVERLAP",
                        row.id(),
                        "Lịch xe gia hạn giờ về chồng lịch kế tiếp của cùng xe",
                        recipients))));
    }

    // ---- Dùng chung ------------------------------------------------------------------------------------------------

    /** Người đặt hoặc Quản lý viện, có khóa quyền cập nhật tài sản, lịch xe. */
    private SimpleRecord<VehicleSchedule> actorRow(long id) {
        var row = records.lockForUpdate(KIND, id);
        boolean booker =
                user.staffId().filter(s -> s == row.content().bookedByStaffId()).isPresent();
        if ((!booker && !user.roles().contains(Role.NURSING_HOME_MANAGER)) || !has(Operation.UPDATE))
            throw new PermissionDeniedException("Chỉ người đặt lịch hoặc Quản lý viện");
        return row;
    }

    private AssetRow lockVehicle(long vehicleId) {
        var vehicle = assets.lock(vehicleId).orElseThrow(() -> new InvalidValueException("vehicleId không phải xe"));
        if (!AssetKinds.VEHICLE_CODE.equals(
                catalog.require(AssetKinds.ASSET_TYPE, vehicle.typeId()).code()))
            throw new InvalidValueException("vehicleId không phải tài sản loại Xe");
        return vehicle;
    }

    private void requireBookable(AssetRow vehicle, List<VehicleBookingCheck.Slot> others, Instant from, Instant to) {
        var unmet = VehicleBookingCheck.check(facts(vehicle), others, from, to);
        if (!unmet.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    unmet.getFirst().code(),
                    unmet.getFirst().description(),
                    Map.of("unmetConditions", unmet));
    }

    private static VehicleBookingCheck.Vehicle facts(AssetRow vehicle) {
        return new VehicleBookingCheck.Vehicle(
                vehicle.status(),
                vehicle.vehicleInspectionDueOn(),
                vehicle.insuranceDueOn(),
                vehicle.vehicleInspectionOverdue(),
                vehicle.insuranceOverdue());
    }

    /** Như {@link #others}, đọc có khóa sau khóa dòng xe (SC-005), cho lệnh ghi. */
    private List<VehicleBookingCheck.Slot> lockedOthers(long vehicleId, Long excluded) {
        return store.activeIdsForUpdate(vehicleId).stream()
                .filter(id -> !Objects.equals(id, excluded))
                .map(id -> records.lockForUpdate(KIND, id))
                .map(r -> new VehicleBookingCheck.Slot(
                        r.id(), r.content().plannedDepartureAt(), r.content().plannedReturnAt()))
                .toList();
    }

    /** Lịch chưa hủy, chưa xong của xe, trừ {@code excluded}. */
    List<VehicleBookingCheck.Slot> others(long vehicleId, Long excluded) {
        return records.listByParent(KIND, new EntityRef(AssetKinds.ENTITY, vehicleId)).stream()
                .filter(r -> ACTIVE.contains(r.status()) && !Objects.equals(r.id(), excluded))
                .map(r -> new VehicleBookingCheck.Slot(
                        r.id(), r.content().plannedDepartureAt(), r.content().plannedReturnAt()))
                .toList();
    }

    private void change(
            SimpleRecord<VehicleSchedule> row, String status, VehicleSchedule content, String key, boolean lock) {
        records.change(KIND, row.id(), row.version(), new RecordChange<>(status, content, null, key, lock));
    }

    private static void requireStatus(SimpleRecord<VehicleSchedule> row, String expected) {
        if (!expected.equals(row.status()))
            throw conflict("STATE_CHANGED", "Lịch xe đang ở trạng thái " + row.status());
    }

    /** Q-298: dời, hủy báo người đặt lịch, Phụ trách bộ phận quản lý xe, người lái (009 chỉ gửi khi có tài khoản). */
    private void notify(long id, VehicleSchedule c, AssetRow vehicle, String event, String title) {
        events.publishEvent(new VehicleScheduleNotice(
                id,
                List.of(request(
                        event,
                        id,
                        title,
                        List.of(
                                new RecipientGroup.Staff(c.bookedByStaffId(), "VEHICLE_SCHEDULE_BOOKER"),
                                new RecipientGroup.DepartmentLead(vehicle.departmentId(), new Substitution.Default()),
                                new RecipientGroup.Staff(c.driverStaffId(), "VEHICLE_DRIVER"))))));
    }

    NotificationRequest request(String event, long id, String title, List<RecipientGroup> to) {
        var now = time.now();
        return new NotificationRequest(
                "M03",
                event,
                SourceGroup.OTHER,
                new SourceRecord(KIND.code(), Long.toString(id)),
                event + ":" + id + ":" + now,
                Optional.of(NotificationLevel.LOW),
                Optional.empty(),
                Optional.empty(),
                to,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                now,
                Map.of("vehicleScheduleId", id),
                false,
                Optional.empty(),
                Optional.empty());
    }

    private boolean has(Operation operation) {
        return permissions.permissions(user).contains(new PermissionKey(Function.ASSET_VEHICLE, operation));
    }

    private long staff() {
        return user.staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ tài khoản nhân viên đặt lịch xe"));
    }

    private void record(long id, String action, String from, String to, Reason reason) {
        var entry = AuditLogEntry.builder()
                .action(new AuditAction("M03_VEHICLE_SCHEDULE_" + action, reason != null))
                .entity(KIND.code(), id)
                .before(from == null ? Map.of() : Map.of("status", from))
                .after(Map.of("status", to));
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
