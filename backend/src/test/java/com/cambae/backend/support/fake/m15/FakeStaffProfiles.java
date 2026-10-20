package com.cambae.backend.support.fake.m15;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.StaffProfileApi;
import com.cambae.backend.m15.account.api.StaffStatus;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link StaffProfileApi} (008 cài): mặc định Đang làm việc, chức danh không dùng hệ thống. */
public class FakeStaffProfiles implements StaffProfileApi {

    private final Map<Long, StaffStatus> statuses = new HashMap<>();
    private final Map<Long, Role> positionRoles = new HashMap<>();
    private final Map<Long, String> createdMinimal = new HashMap<>();
    private long nextStaffId = 5_000_000;

    public FakeStaffProfiles staff(long staffId, Role positionRole) {
        positionRoles.put(staffId, positionRole);
        return this;
    }

    public FakeStaffProfiles status(long staffId, StaffStatus status) {
        statuses.put(staffId, status);
        return this;
    }

    @Override
    public StaffStatus status(long staffId, LocalDate date) {
        return statuses.getOrDefault(staffId, StaffStatus.WORKING);
    }

    @Override
    public Optional<Role> positionRole(long staffId) {
        return Optional.ofNullable(positionRoles.get(staffId));
    }

    /** Hồ sơ tối thiểu: id mới tăng dần, ghi lại họ tên để test kiểm. */
    @Override
    public synchronized long createMinimal(String fullName, Role positionRole) {
        long staffId = ++nextStaffId;
        createdMinimal.put(staffId, fullName);
        positionRoles.put(staffId, positionRole);
        return staffId;
    }

    /** Hồ sơ đã tạo bằng {@link #createMinimal}: id → họ tên. */
    public Map<Long, String> createdMinimal() {
        return Map.copyOf(createdMinimal);
    }
}
