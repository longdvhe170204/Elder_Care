package com.eldercare.backend.admission.internal.transition.service;

import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.admission.api.dto.UnitPriceView;
import com.eldercare.backend.admission.internal.application.domain.AdmissionApplication;
import com.eldercare.backend.admission.internal.application.persistence.AdmissionApplicationStore;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.domain.CareLevelCodes;
import com.eldercare.backend.admission.internal.waitinglist.service.WaitingListTransition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.api.spi.HeldBed;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn lưu trú cho phân bổ giường (tên logic {@code NguonLuuTruChoPhanBo}; 003 contracts mục 2.1, FR-018, FR-024, FR-030; 004 T010):
 * loại lưu trú lấy từ hợp đồng Hiệu lực, rồi hợp đồng đang lập, rồi lượt đăng ký (CHK007); mức theo hợp đồng Nháp, Chờ ký, Hiệu lực
 * (Q-301). Vắng luôn giữ giường, không hạn; giải phóng chỉ qua lệnh của 004 (FR-050, FR-055, Q-395).
 */
@Component
class M02AllocationResidencySource implements AllocationResidencySource {

    private static final Set<ContractStatus> IN_PREPARATION =
            Set.of(ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE);

    private final ContractStore contracts;
    private final AdmissionApplicationStore applications;
    private final UnitPriceApi prices;
    private final WaitingListTransition waitingList;

    M02AllocationResidencySource(
            ContractStore contracts,
            AdmissionApplicationStore applications,
            UnitPriceApi prices,
            WaitingListTransition waitingList) {
        this.contracts = contracts;
        this.applications = applications;
        this.prices = prices;
        this.waitingList = waitingList;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResidencyType> residencyType(long residentId) {
        List<Contract> all = contracts.ofResident(residentId);
        Optional<Contract> active = latest(all, Set.of(ContractStatus.ACTIVE));
        if (active.isPresent()) {
            return Optional.of(CareLevelCodes.of(currentType(active.get())));
        }
        Optional<Contract> preparing = latest(all, IN_PREPARATION);
        if (preparing.isPresent()) {
            return Optional.of(CareLevelCodes.of(preparing.get().content().residenceType()));
        }
        return registration(residentId).map(a -> CareLevelCodes.of(a.desiredResidenceType()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareLevelCode> careLevelFromContract(long residentId) {
        List<Contract> all = contracts.ofResident(residentId);
        return latest(all, Set.of(ContractStatus.ACTIVE))
                .or(() -> latest(all, IN_PREPARATION))
                .map(contract -> contracts
                        .latestVersion(contract.id())
                        .map(v -> com.eldercare.backend.resident.api.dto.CareLevel.valueOf(
                                v.content().get("careLevel").asString()))
                        .orElse(contract.content().careLevel()))
                .map(CareLevelCodes::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareLevelCode> careLevelFromRegistration(long residentId) {
        return registration(residentId).map(a -> CareLevelCodes.of(a.expectedCareLevel()));
    }

    /** Nội trú ngắn ngày: hết ngày kết thúc hiện hành của hợp đồng (FR-018; đầu ngày liền sau, giờ Việt Nam). */
    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> expectedEnd(long residentId) {
        List<Contract> all = contracts.ofResident(residentId);
        return latest(all, Set.of(ContractStatus.ACTIVE))
                .or(() -> latest(all, IN_PREPARATION))
                .filter(contract -> currentType(contract) == ResidenceType.SHORT_TERM_RESIDENTIAL)
                .map(contract -> contract.currentEndDate() != null
                        ? contract.currentEndDate()
                        : contract.content().endDate())
                .map(end -> end.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant());
    }

    /** Q-395: vắng luôn giữ giường, không hạn. */
    @Override
    public HeldBed holdBedWhenAbsent(long residentId) {
        return new HeldBed(true, Optional.empty());
    }

    /** So đơn giá phí lưu trú của hai loại phòng tại ngày (FR-030); thiếu đơn giá của một bên thì coi là khác, phải có yêu cầu. */
    @Override
    @Transactional(readOnly = true)
    public boolean differentUnitPrice(long residentId, long previousRoomTypeId, long nextRoomTypeId, LocalDate date) {
        Optional<BigDecimal> previous = feeAt(previousRoomTypeId, date);
        Optional<BigDecimal> next = feeAt(nextRoomTypeId, date);
        return previous.isEmpty() || next.isEmpty() || previous.get().compareTo(next.get()) != 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void waitingRecordHoldExpired(long waitingRecordId) {
        waitingList.holdExpired(waitingRecordId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void allocated(long residentId, long allocationId) {
        waitingList.allocated(residentId, allocationId);
    }

    private Optional<BigDecimal> feeAt(long roomTypeId, LocalDate date) {
        return prices.residencyFeeServiceOf(roomTypeId)
                .map(ServiceView::id)
                .flatMap(serviceId -> prices.unitPriceAt(serviceId, date, false))
                .map(UnitPriceView::unitPrice);
    }

    private ResidenceType currentType(Contract contract) {
        return contracts
                .latestVersion(contract.id())
                .map(v -> ResidenceType.valueOf(v.content().get("residenceType").asString()))
                .orElse(contract.content().residenceType());
    }

    private Optional<AdmissionApplication> registration(long residentId) {
        return applications.findOpen(residentId).or(() -> applications.findLatest(residentId));
    }

    private static Optional<Contract> latest(List<Contract> all, Set<ContractStatus> statuses) {
        return all.stream()
                .filter(contract -> statuses.contains(contract.status()))
                .max(Comparator.comparing(Contract::id));
    }
}
