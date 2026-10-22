package com.cambae.backend.support.fake.m15;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.spi.StaffProfileApi;
import com.cambae.backend.m15.account.api.spi.StaffStatus;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link StaffProfileApi} (008 cài): mặc định Đang làm việc, chức danh không dùng hệ thống. */
public class FakeStaffProfiles implements StaffProfileApi {

    private final Map<Long, StaffStatus> statuses = new HashMap<>();
    private final Map<Long, Role> positionRoles = new HashMap<>();
    private final Map<Long, String> createdMinimal = new HashMap<>();
    private final Map<Long, String> names = new HashMap<>();
    // Dải riêng, không trùng support/TestUsers (từ 5.000.000) vì hai bên cùng ghi staff_id vào bảng account
    private long nextStaffId = 6_000_000;

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

    public FakeStaffProfiles name(long staffId, String fullName) {
        names.put(staffId, fullName);
        return this;
    }

    /** Họ tên đặt bằng {@link #name} hoặc khi tạo hồ sơ tối thiểu. */
    @Override
    public Optional<String> fullName(long staffId) {
        return Optional.ofNullable(names.getOrDefault(staffId, createdMinimal.get(staffId)));
    }

    /** Hồ sơ đã tạo bằng {@link #createMinimal}: id → họ tên. */
    public Map<Long, String> createdMinimal() {
        return Map.copyOf(createdMinimal);
    }
}
