package com.eldercare.backend.support.fake.admission;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.admission.api.dto.DepositAmountView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bản giả chung duy nhất của hàm lịch đến 004; chỉ nạp trong test. Các hàm đọc hợp đồng khác trả rỗng. */
public class FakeContractSchedules implements ContractReadApi {
    private final Map<Long, List<AttendanceScheduleView>> schedules = new HashMap<>();
    private boolean available = true;

    public FakeContractSchedules schedule(long resident, AttendanceScheduleView value) {
        schedules.computeIfAbsent(resident, id -> new ArrayList<>()).add(value);
        return this;
    }

    public FakeContractSchedules available(boolean value) {
        available = value;
        return this;
    }

    public FakeContractSchedules clear() {
        schedules.clear();
        available = true;
        return this;
    }

    @Override
    public List<AttendanceScheduleView> attendanceOn(long residentId, LocalDate date) {
        if (!available) throw new IllegalStateException("Chưa có nguồn lịch đến 004");
        return schedules.getOrDefault(residentId, List.of()).stream()
                .filter(s -> s.date().equals(date))
                .toList();
    }

    @Override
    public Optional<ContractContentView> contentAtDate(long residentId, LocalDate date) {
        return Optional.empty();
    }

    @Override
    public Optional<ContractView> contract(long contractId) {
        return Optional.empty();
    }

    @Override
    public List<ContractView> ofResident(long residentId) {
        return List.of();
    }

    @Override
    public Optional<DepositAmountView> depositAmount(long contractId) {
        return Optional.empty();
    }
}
