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

/**
 * Bản giả chung duy nhất của hàm lịch đến 004; chỉ nạp trong test. Hợp đồng, khoản cọc đặt bằng {@link #contract},
 * {@link #deposit} (017, A thêm 2026-11-08); hàm đọc khác trả rỗng.
 */
public class FakeContractSchedules implements ContractReadApi {
    private final Map<Long, List<AttendanceScheduleView>> schedules = new HashMap<>();
    private final Map<Long, ContractView> contracts = new HashMap<>();
    private final Map<Long, DepositAmountView> deposits = new HashMap<>();
    private boolean available = true;

    public FakeContractSchedules contract(ContractView value) {
        contracts.put(value.id(), value);
        return this;
    }

    public FakeContractSchedules deposit(DepositAmountView value) {
        deposits.put(value.contractId(), value);
        return this;
    }

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
        contracts.clear();
        deposits.clear();
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
        return Optional.ofNullable(contracts.get(contractId));
    }

    @Override
    public List<ContractView> ofResident(long residentId) {
        return contracts.values().stream()
                .filter(c -> c.residentId() == residentId)
                .toList();
    }

    @Override
    public Optional<DepositAmountView> depositAmount(long contractId) {
        return Optional.ofNullable(deposits.get(contractId));
    }
}
