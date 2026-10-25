package com.cambae.backend.support.fake.m02;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ArrivalScheduleView;
import com.cambae.backend.m02.api.dto.ContractContentView;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.DepositAmountView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả {@link ContractReadApi} cho tới khi bản thật của Phase 5 (hợp đồng, V004_05) có: test của user story 1 đặt
 * hợp đồng cho điều kiện (b), (c), (e) và cảnh báo FR-076. Nạp bằng {@code @Import(FakeContractReads.Config.class)};
 * {@code @Primary} nên thắng bản thật khi sau này có.
 */
public class FakeContractReads implements ContractReadApi {

    private static final AtomicLong IDS = new AtomicLong(7_000_000);

    private final Map<Long, List<ContractView>> contracts = new HashMap<>();

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        public FakeContractReads fakeContractReads() {
            return new FakeContractReads();
        }
    }

    /** Hợp đồng mới của người cao tuổi; trả mã hợp đồng. */
    public long contract(
            long residentId,
            ContractStatus status,
            ResidenceType residenceType,
            CareLevel careLevel,
            LocalDate startDate) {
        long id = IDS.incrementAndGet();
        contracts
                .computeIfAbsent(residentId, key -> new ArrayList<>())
                .add(new ContractView(
                        id, "HD-" + id, residentId, residenceType, startDate, null, null, careLevel, status, null));
        return id;
    }

    /** Hợp đồng Hiệu lực dài hạn bắt đầu ở {@code startDate}. */
    public long active(long residentId, CareLevel careLevel, LocalDate startDate) {
        return contract(residentId, ContractStatus.ACTIVE, ResidenceType.LONG_TERM_RESIDENTIAL, careLevel, startDate);
    }

    public FakeContractReads reset() {
        contracts.clear();
        return this;
    }

    @Override
    public Optional<ContractContentView> contentAtDate(long residentId, LocalDate date) {
        return Optional.empty();
    }

    @Override
    public Optional<ContractView> contract(long contractId) {
        return contracts.values().stream()
                .flatMap(List::stream)
                .filter(contract -> contract.id() == contractId)
                .findFirst();
    }

    @Override
    public List<ContractView> ofResident(long residentId) {
        return List.copyOf(contracts.getOrDefault(residentId, List.of()));
    }

    @Override
    public Optional<DepositAmountView> depositAmount(long contractId) {
        return Optional.empty();
    }

    @Override
    public List<ArrivalScheduleView> arrivalScheduleAtDate(long residentId, LocalDate date) {
        return List.of();
    }
}
