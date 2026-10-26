package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.ResidencyEndingReadApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.ResidencyEndingView;
import com.cambae.backend.m02.api.dto.SettlementInfoView;
import com.cambae.backend.m02.internal.ending.domain.EndingConditionCode;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc hồ sơ kết thúc cho 017 (tên logic {@code HoSoKetThucDocApi}; Q-379, 017 FR-022, FR-034). Chỉ đọc, không kiểm quyền: 017
 * tự quyết dùng cho điều kiện nào. Trường hợp qua đời (danh sách việc sau qua đời) thuộc user story 8.
 */
@Service
class ResidencyEndingReadApiImpl implements ResidencyEndingReadApi {

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);

    private final ResidencyEndingStore store;
    private final ContractReadApi contracts;

    ResidencyEndingReadApiImpl(ResidencyEndingStore store, ContractReadApi contracts) {
        this.store = store;
        this.contracts = contracts;
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
        return store.preparingOf(residentId).map(ending -> {
            Optional<ContractView> contract = contracts.ofResident(residentId).stream()
                    .filter(c -> SIGNED.contains(c.status()))
                    .max(Comparator.comparing((ContractView c) -> c.status() == ContractStatus.ACTIVE)
                            .thenComparing(ContractView::startDate));
            ResidenceType type = contract.map(ContractView::residenceType).orElse(null);
            return new SettlementInfoView(
                    type,
                    contract.map(ContractView::id).orElse(null),
                    contract.map(ContractView::startDate).orElse(null),
                    ending.expectedEndDate(),
                    ending.endCase(),
                    null);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApprovedSettlementException(long residentId) {
        return store.preparingOf(residentId)
                .map(ResidencyEnding::id)
                .map(store::exceptionsOf)
                .map(list -> list.stream().anyMatch(e -> e.conditionCode() == EndingConditionCode.END_H_SETTLEMENT))
                .orElse(false);
    }
}
