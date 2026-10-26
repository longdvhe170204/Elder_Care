package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.ResidencyEndingReadApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.EndCase;
import com.cambae.backend.m02.api.dto.ResidencyEndingView;
import com.cambae.backend.m02.api.dto.SettlementInfoView;
import com.cambae.backend.m02.internal.death.domain.DeathRecord;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m02.internal.ending.domain.EndingConditionCode;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc hồ sơ kết thúc cho 017 (tên logic {@code HoSoKetThucDocApi}; Q-379, 017 FR-022, FR-034). Chỉ đọc, không kiểm quyền: 017
 * tự quyết dùng cho điều kiện nào. Người qua đời mà danh sách việc sau qua đời còn mở thì trường hợp là {@code DEATH} kèm ngày
 * qua đời (017 FR-034) và ngoại lệ ý tiền đọc từ dòng không gắn hồ sơ kết thúc (FR-071a).
 */
@Service
class ResidencyEndingReadApiImpl implements ResidencyEndingReadApi {

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);

    private final ResidencyEndingStore store;
    private final ContractReadApi contracts;
    private final DeathStore deaths;

    ResidencyEndingReadApiImpl(ResidencyEndingStore store, ContractReadApi contracts, DeathStore deaths) {
        this.store = store;
        this.contracts = contracts;
        this.deaths = deaths;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResidencyEndingView> preparing(long residentId) {
        return store.preparingOf(residentId)
                .map(e -> new ResidencyEndingView(
                        e.id(), e.residentId(), e.endCase(), e.expectedEndDate(), e.overdueSince()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SettlementInfoView> settlementInfo(long residentId) {
        Optional<SettlementInfoView> forEnding = store.preparingOf(residentId).map(ending -> {
            Optional<ContractView> contract = signedContract(residentId);
            return new SettlementInfoView(
                    contract.map(ContractView::residenceType).orElse(null),
                    contract.map(ContractView::id).orElse(null),
                    contract.map(ContractView::startDate).orElse(null),
                    ending.expectedEndDate(),
                    ending.endCase(),
                    null);
        });
        if (forEnding.isPresent()) {
            return forEnding;
        }
        return openDeath(residentId).map(record -> {
            LocalDate deathDate = record.deathAt().atZone(BusinessTime.ZONE).toLocalDate();
            Optional<ContractView> contract = signedContract(residentId);
            return new SettlementInfoView(
                    contract.map(ContractView::residenceType).orElse(null),
                    contract.map(ContractView::id).orElse(null),
                    contract.map(ContractView::startDate).orElse(null),
                    deathDate,
                    EndCase.DEATH,
                    deathDate);
        });
    }

    private Optional<ContractView> signedContract(long residentId) {
        return contracts.ofResident(residentId).stream()
                .filter(c -> SIGNED.contains(c.status()))
                .max(Comparator.comparing((ContractView c) -> c.status() == ContractStatus.ACTIVE)
                        .thenComparing(ContractView::startDate));
    }

    /** Bản ghi qua đời mà danh sách việc còn mở (hồ sơ lưu trú chưa Đã đóng). */
    private Optional<DeathRecord> openDeath(long residentId) {
        return deaths.ofResident(residentId)
                .filter(r -> deaths.closedAt(residentId).isEmpty());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApprovedSettlementException(long residentId) {
        boolean forEnding = store.preparingOf(residentId)
                .map(ResidencyEnding::id)
                .map(store::exceptionsOf)
                .map(list -> list.stream().anyMatch(e -> e.conditionCode() == EndingConditionCode.END_H_SETTLEMENT))
                .orElse(false);
        return forEnding || (openDeath(residentId).isPresent() && store.hasPostDeathSettlementException(residentId));
    }
}
