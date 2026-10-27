package com.cambae.backend.m02.internal.residencyday.service;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ResidencyStatisticsApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ResidenceTypeAtDateView;
import com.cambae.backend.m02.api.dto.WaitingEntryView;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractVersion;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Số liệu cho báo cáo 016 (tên logic {@code LuuTruThongKeApi}; research O-10): loại lưu trú tại ngày theo hợp đồng và hồ sơ
 * chờ trong khoảng. Chỉ đọc. M02 không giữ tầng, phòng của người cao tuổi nên phạm vi không phải toàn viện chỉ lọc theo
 * {@code residentIds}; người gọi cần đổi tầng, phòng thành danh sách người trước.
 */
@Service
class ResidencyStatisticsService implements ResidencyStatisticsApi {

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);

    private final ContractStore contracts;
    private final WaitingListStore waitingList;

    ResidencyStatisticsService(ContractStore contracts, WaitingListStore waitingList) {
        this.contracts = contracts;
        this.waitingList = waitingList;
    }

    /**
     * Hợp đồng đã ký có khoảng chứa ngày (Hiệu lực kể cả quá hạn, hoặc Kết thúc, Chấm dứt tới ngày kết thúc); người chưa có hợp
     * đồng như vậy mà có hợp đồng Chờ ký thì loại lưu trú theo hợp đồng đó; người còn lại không có dòng.
     */
    @Override
    @Transactional(readOnly = true)
    public Stream<ResidenceTypeAtDateView> residenceTypeAtDate(ScopeFilter scope, LocalDate date) {
        Map<Long, List<Contract>> byResident = contracts
                .list(
                        null,
                        Set.of(
                                ContractStatus.ACTIVE,
                                ContractStatus.ENDED,
                                ContractStatus.TERMINATED,
                                ContractStatus.PENDING_SIGNATURE),
                        null)
                .stream()
                .filter(c -> scope.facilityWide() || scope.residentIds().contains(c.residentId()))
                .collect(Collectors.groupingBy(Contract::residentId));
        return byResident.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> view(e.getKey(), e.getValue(), date))
                .flatMap(Optional::stream);
    }

    private Optional<ResidenceTypeAtDateView> view(long residentId, List<Contract> all, LocalDate date) {
        Optional<Contract> covering = all.stream()
                .filter(c -> SIGNED.contains(c.status()))
                .filter(c -> !c.content().startDate().isAfter(date))
                .filter(c -> c.status() == ContractStatus.ACTIVE || !date.isAfter(lastDay(c)))
                .max(Comparator.comparing((Contract c) -> c.content().startDate())
                        .thenComparing(Contract::id));
        if (covering.isPresent()) {
            Contract contract = covering.get();
            ResidenceType type = contracts
                    .versionAt(contract.id(), date)
                    .map(ContractVersion::content)
                    .map(content ->
                            ResidenceType.valueOf(content.get("residenceType").asString()))
                    .orElse(contract.content().residenceType());
            return Optional.of(new ResidenceTypeAtDateView(residentId, type, contract.status()));
        }
        return all.stream()
                .filter(c -> c.status() == ContractStatus.PENDING_SIGNATURE)
                .max(Comparator.comparing(Contract::id))
                .map(c -> new ResidenceTypeAtDateView(residentId, c.content().residenceType(), c.status()));
    }

    private LocalDate lastDay(Contract contract) {
        return contracts
                .terminatedOn(contract.id())
                .or(() -> Optional.ofNullable(contract.currentEndDate()))
                .orElse(LocalDate.MAX);
    }

    /** Hồ sơ chờ có ngày đăng ký trong khoảng; M02 chưa lưu ngày rời danh sách nên {@code leftOn} để trống. */
    @Override
    @Transactional(readOnly = true)
    public Stream<WaitingEntryView> waitingList(LocalDate from, LocalDate to) {
        return waitingList.list(null).stream()
                .filter(e -> !e.applicationDate().isBefore(from)
                        && !e.applicationDate().isAfter(to))
                .map(e -> new WaitingEntryView(
                        e.id(), e.status().name(), e.residenceType(), e.careLevel(), e.applicationDate(), null));
    }
}
