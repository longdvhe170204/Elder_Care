package com.eldercare.backend.staffing.internal.credential.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.*;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentSources;
import com.eldercare.backend.staffing.internal.credential.domain.*;
import com.eldercare.backend.staffing.internal.credential.persistence.CredentialStore;
import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import com.eldercare.backend.staffing.internal.staff.service.StaffNotifications;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** T034: cảnh báo đào tạo theo CFG, nhắc bộ phận theo ngày; không tự thêm hồ sơ/chứng chỉ. */
@Service
@org.springframework.core.annotation.Order(50)
public class CredentialReminders implements M09TimedTask {
    private final CredentialStore credentials;
    private final StaffStore staff;
    private final AssignmentSources sources;
    private final AccountApi accounts;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore assignments;
    private final com.eldercare.backend.staffing.internal.duty.persistence.DepartmentDutyStore departments;
    private final ApplicationEventPublisher events;
    private final StaffNotifications notifications;
    private final ShiftStore shifts;

    public CredentialReminders(
            CredentialStore credentials,
            StaffStore staff,
            AssignmentSources sources,
            AccountApi accounts,
            ParameterApi parameters,
            BusinessTime time,
            com.eldercare.backend.staffing.internal.assignment.persistence.AssignmentStore assignments,
            com.eldercare.backend.staffing.internal.duty.persistence.DepartmentDutyStore departments,
            ApplicationEventPublisher events,
            StaffNotifications notifications,
            ShiftStore shifts) {
        this.credentials = credentials;
        this.staff = staff;
        this.sources = sources;
        this.accounts = accounts;
        this.parameters = parameters;
        this.time = time;
        this.assignments = assignments;
        this.departments = departments;
        this.events = events;
        this.notifications = notifications;
        this.shifts = shifts;
    }

    public Cadence cadence() {
        return Cadence.DAILY;
    }

    public int run(Instant scheduled, Instant actual) {
        int processed = 0;
        var horizon =
                time.today().plus(parameters.read(ParameterCode.CFG_M09_03).asPeriod());
        for (var c : credentials.all()) {
            if (!"TRAINING".equals(c.group())
                    || c.replaced()
                    || c.expiryDate() == null
                    || c.expiryDate().isBefore(time.today())
                    || c.expiryDate().isAfter(horizon)
                    || CredentialValidity.status(
                                    c.issueDate(), c.expiryDate(), c.revokedFrom(), c.replaced(), actual, time)
                            != CredentialValidity.Status.VALID
                    || !staff.find(c.staffId())
                            .map(s -> "WORKING".equals(s.status()))
                            .orElse(false)) continue;
            var roles = accounts.byStaff(c.staffId())
                    .map(a -> a.roles().stream().map(Enum::name).toList())
                    .orElse(List.of());
            boolean required = roles.stream()
                    .anyMatch(
                            credentials.type(c.credentialTypeId()).orElseThrow().requiredForRoles()::contains);
            if (!required)
                required = assignments.activeTaskTypesEndingAfter(c.staffId(), actual).stream()
                        .anyMatch(task -> sources.requirements(task).certificates().stream()
                                .anyMatch(code ->
                                        code.equals(c.typeCode()) || code.equals(Long.toString(c.credentialTypeId()))));
            if (!required) continue;
            if (credentials.claimExpiryWarning(c.id(), actual) == 0) continue;
            var shiftIds = shifts.remainingStaffShifts(
                    c.staffId(), time.startOfDay(c.expiryDate().plusDays(1)));
            var floors = new TreeSet<Long>();
            for (long shiftId : shiftIds) {
                var shift = shifts.shift(shiftId, false);
                if ("FLOOR".equals(shift.scopeType())) floors.add(shift.scopeId());
                else if ("AREA".equals(shift.scopeType()))
                    floors.addAll(sources.structure().floorsInArea(shift.scopeId()));
            }
            processed++;
            events.publishEvent(new CredentialExpiring(
                    c.id(),
                    c.staffId(),
                    c.typeName(),
                    c.expiryDate(),
                    actual,
                    notifications.training(
                            c.id(),
                            c.staffId(),
                            staff.find(c.staffId()).orElseThrow().fullName(),
                            c.typeName(),
                            c.expiryDate(),
                            floors,
                            shiftIds,
                            actual)));
        }
        for (long id : departments.activeWithoutLead()) {
            processed++;
            events.publishEvent(new DepartmentLeadMissing(id, actual, notifications.missingDepartment(id, actual)));
        }
        return processed;
    }
}
