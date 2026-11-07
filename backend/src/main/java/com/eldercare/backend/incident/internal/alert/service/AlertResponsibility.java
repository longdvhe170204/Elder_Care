package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.roombedallocation.api.LocationHistoryApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.ResidentLocationRow;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Tầng và người phụ trách của cảnh báo (FR-031, FR-037, BR-M05-19, research S-08, S-18). Người phụ trách lưu bằng mã
 * tài khoản; {@link AlertStaffPort} trả mã nhân viên nên đổi qua {@link AccountApi#byStaff}.
 */
@Component
public class AlertResponsibility {
    private final Optional<AlertStaffPort> staff;
    private final Optional<AccountApi> accounts;
    private final Optional<ScopeApi> scopes;
    private final Optional<ResidentLocationApi> locations;
    private final Optional<LocationHistoryApi> history;

    public AlertResponsibility(
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) AccountApi accounts,
            @Autowired(required = false) ScopeApi scopes,
            @Autowired(required = false) ResidentLocationApi locations,
            @Autowired(required = false) LocationHistoryApi history) {
        this.staff = Optional.ofNullable(staff);
        this.accounts = Optional.ofNullable(accounts);
        this.scopes = Optional.ofNullable(scopes);
        this.locations = Optional.ofNullable(locations);
        this.history = Optional.ofNullable(history);
    }

    /**
     * Tầng của người cao tuổi: tầng của giường đang giữ (kể cả khi vắng mặt); không còn giường thì tầng của phân bổ gần
     * nhất (BR-M05-19, FR-031).
     */
    public Optional<Long> floorOf(long residentId, Instant at) {
        var current = locations.flatMap(api -> api.current(residentId)).map(location -> location.floorId());
        if (current.isPresent()) return current;
        return history.flatMap(api -> api.byResident(residentId, Instant.EPOCH, at).stream()
                .filter(row -> row.location() != null && row.location().floorId() != null)
                .max(Comparator.comparing(ResidentLocationRow::from))
                .map(row -> row.location().floorId()));
    }

    /** Điều dưỡng phụ trách người cao tuổi tại thời điểm, không có thì Người phụ trách ca của tầng (FR-031, FR-037). */
    public Optional<Long> initialAssignee(Long residentId, Long floorAreaId, Instant at) {
        if (residentId != null) {
            var nurse =
                    staff.flatMap(port -> port.assignedNurse(residentId, at)).flatMap(this::accountOfStaff);
            if (nurse.isPresent()) return nurse;
        }
        return shiftLead(floorAreaId, at);
    }

    /** Người tạm nhận khi bàn giao chưa xác nhận: Người phụ trách ca sau, không có thì Trưởng tầng (FR-037, Q-94). */
    public Optional<Long> provisionalAssignee(long floorAreaId, Instant at) {
        var lead = shiftLead(floorAreaId, at);
        if (lead.isPresent()) return lead;
        return staff.flatMap(port -> port.assignedFloorManager(floorAreaId, at)).flatMap(this::accountOfStaff);
    }

    /** Mã nhân viên của tài khoản phụ trách, để hỏi lịch ca ({@link AlertStaffPort} dùng mã nhân viên). */
    public Optional<Long> staffOfAccount(long accountId) {
        return accounts.flatMap(api -> api.byId(accountId)).map(AccountView::staffId);
    }

    private Optional<Long> shiftLead(Long floorAreaId, Instant at) {
        if (floorAreaId == null) return Optional.empty();
        return scopes.flatMap(api -> api.shiftLead(floorAreaId, at));
    }

    private Optional<Long> accountOfStaff(long staffId) {
        return accounts.flatMap(api -> api.byStaff(staffId)).map(AccountView::accountId);
    }
}
