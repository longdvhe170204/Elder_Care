package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.events.AdmissionCompleted;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import com.cambae.backend.m02.internal.registration.domain.ObligationTrigger;
import java.util.Comparator;
import java.util.Set;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Mốc nghĩa vụ khai báo từ sự kiện của 004 (018 T007; FR-001, FR-001a, Q-286, Q-336): nghe sau commit, transaction riêng,
 * giao lại khi khởi động lại (NFR-05); khóa {@code (người, loại mốc, nguồn)} làm lần giao lại không ghi trùng. Lỗi ở đây
 * không làm hỏng lệnh nguồn (Q-215: khai báo không chặn Hoàn tất tiếp nhận).
 */
@Component
class RegistrationObligationListeners {

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);
    private static final Set<ResidentStatus> NOT_RESIDING = Set.of(
            ResidentStatus.ADMISSION_IN_PROGRESS,
            ResidentStatus.RESIDENCY_ENDED,
            ResidentStatus.DECEASED,
            ResidentStatus.ADMISSION_CANCELLED);

    private final RegistrationObligations obligations;
    private final ContractReadApi contracts;
    private final ResidentApi residents;
    private final BusinessTime time;

    RegistrationObligationListeners(
            RegistrationObligations obligations, ContractReadApi contracts, ResidentApi residents, BusinessTime time) {
        this.obligations = obligations;
        this.contracts = contracts;
        this.residents = residents;
        this.time = time;
    }

    /**
     * Hoàn tất tiếp nhận người nội trú: mốc là thời điểm Hoàn tất tiếp nhận (FR-002). Sự kiện không mang thời điểm nên lấy
     * lúc nghe, ngay sau commit; chỉ lệch khi sự kiện được giao lại sau khởi động lại (plan "Phase 3 hoàn thành").
     */
    @ApplicationModuleListener
    void on(AdmissionCompleted event) {
        if (event.residenceType() == ResidenceType.DAY_CARE) {
            return;
        }
        obligations.record(event.residentId(), ObligationTrigger.ADMISSION_COMPLETED, time.now(), event.contractId());
    }

    /**
     * Hợp đồng nội trú mới của người đang lưu trú bán trú vào Hiệu lực (FR-001a): mốc là ngày bắt đầu hợp đồng, kể cả ở
     * tương lai (Q-336). Người chưa Hoàn tất tiếp nhận nhận mốc từ {@link AdmissionCompleted}; người đã nội trú (hợp đồng
     * liền trước không phải bán trú) không có mốc mới ở đây.
     */
    @ApplicationModuleListener
    void on(ContractStatusChanged event) {
        if (event.to() != ContractStatus.ACTIVE) {
            return;
        }
        ContractView contract = contracts.contract(event.contractId()).orElse(null);
        if (contract == null
                || contract.residenceType() == ResidenceType.DAY_CARE
                || NOT_RESIDING.contains(residents.status(event.residentId()))
                || !dayCareBefore(contract)) {
            return;
        }
        obligations.record(
                event.residentId(),
                ObligationTrigger.RESIDENTIAL_EFFECTIVE,
                contract.startDate().atStartOfDay(BusinessTime.ZONE).toInstant(),
                contract.id());
    }

    /** Hợp đồng đã ký liền trước (bắt đầu muộn nhất trước {@code contract}) là bán trú. */
    private boolean dayCareBefore(ContractView contract) {
        return contracts.ofResident(contract.residentId()).stream()
                .filter(other -> other.id() != contract.id() && SIGNED.contains(other.status()))
                .filter(other -> other.startDate().isBefore(contract.startDate()))
                .max(Comparator.comparing(ContractView::startDate))
                .map(previous -> previous.residenceType() == ResidenceType.DAY_CARE)
                .orElse(false);
    }
}
