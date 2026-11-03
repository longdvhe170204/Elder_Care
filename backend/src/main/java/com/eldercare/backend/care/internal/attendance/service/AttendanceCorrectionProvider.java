package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** FR-015: bản ghi gốc bất biến; chỉ cập nhật projection sau khi 000 áp dụng đính chính. */
@Component
public class AttendanceCorrectionProvider implements CorrectableRecordProvider {
    private final AttendanceStore store;
    private final CareLockStore locks;
    private final AttendanceService commands;
    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final BusinessTime time;

    public AttendanceCorrectionProvider(
            AttendanceStore store,
            CareLockStore locks,
            AttendanceService commands,
            PermissionApi permissions,
            ScopeApi scopes,
            BusinessTime time) {
        this.store = store;
        this.locks = locks;
        this.commands = commands;
        this.permissions = permissions;
        this.scopes = scopes;
        this.time = time;
    }

    public String recordType() {
        return "M04.ATTENDANCE_RECORD";
    }

    public boolean isConfirmed(long id) {
        try {
            return store.record(id).confirmedAt() != null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    public long originalRecorder(long id) {
        return Objects.requireNonNull(store.record(id).actorId());
    }

    public Optional<Long> floorArea(long id) {
        return Optional.ofNullable(store.record(id).floorAreaId());
    }

    public Optional<Long> residentId(long id) {
        return Optional.of(
                store.require(store.record(id).content().attendanceId()).residentId());
    }

    public boolean canRecord(CurrentUser user) {
        if (Collections.disjoint(user.roles(), Set.of(Role.OFFICE_STAFF, Role.CAREGIVER, Role.NURSING_STAFF)))
            return false;
        var scope = scopes.filter(user, new PermissionKey(Function.DAY_CARE_ATTENDANCE, Operation.CREATE));
        return scope.facilityWide()
                || !scope.floorAreaIds().isEmpty()
                || !scope.roomIds().isEmpty()
                || !scope.residentIds().isEmpty();
    }

    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        return method == CreationMethod.B
                || permissions
                        .hasPermission(
                                user,
                                new PermissionKey(Function.DAY_CARE_ATTENDANCE, Operation.CREATE),
                                new Target(
                                        recordType(),
                                        Long.toString(id),
                                        residentId(id).orElseThrow(),
                                        floorArea(id).orElse(null)))
                        .allowed();
    }

    public Object originalContent(long id) {
        return Map.of("at", store.record(id).content().at().toString());
    }

    private Instant at(Object value) {
        if (!(value instanceof Map<?, ?> map)
                || !map.keySet().equals(Set.of("at"))
                || !(map.get("at") instanceof String text))
            throw new InvalidValueException("Chỉ đính chính thời điểm bằng trường at");
        try {
            return Instant.parse(text);
        } catch (java.time.format.DateTimeParseException e) {
            throw new InvalidValueException("Thời điểm đúng chưa hợp lệ");
        }
    }

    public void validateContent(Object value) {
        at(value);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void afterCorrection(CorrectionView correction) {
        if (correction.type() == CorrectionType.VOID)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CORRECTION_NOT_SUPPORTED",
                    "Hủy ghi nhận điểm danh đang chờ thống nhất nghiệp vụ");
        var original = store.record(correction.originalRecordId());
        var first = store.require(original.content().attendanceId());
        locks.lock(List.of(first.residentId()));
        var before = store.lock(first.id());
        var actual = at(correction.correctedContent());
        var data = before.content();
        if (original.content().direction().equals("ARRIVE")) {
            if (!actual.atZone(BusinessTime.ZONE).toLocalDate().equals(before.effectiveFrom())
                    || (data.leftAt() != null && actual.isAfter(data.leftAt())))
                throw new InvalidValueException("Giờ đến đúng phải thuộc ngày có mặt và không sau giờ về");
            data = data.arrive(original.id(), actual, data.adHocSession());
        } else {
            if (data.arrivedAt() == null || actual.isBefore(data.arrivedAt()))
                throw new InvalidValueException("Giờ về đúng không được trước giờ đến");
            data = data.leave(original.id(), actual, data.checkoutType());
        }
        var after = store.change(before, AttendanceStatus.valueOf(before.status()), data);
        commands.log(before, after, "ATTENDANCE_TIME_CORRECTED", time.now());
    }
}
