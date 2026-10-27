package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.events.AddendumApplied;
import com.cambae.backend.m02.api.events.AdmissionCompleted;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import com.cambae.backend.m02.internal.registration.domain.ObligationTrigger;
import com.cambae.backend.m02.internal.registration.domain.RegistrationStatus;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.persistence.ResidenceRegistrationStore;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Mốc nghĩa vụ khai báo từ sự kiện của 004 (018 T007, T012; FR-001, FR-001a, FR-007, BR-M02-14, Q-286, Q-336): nghe sau commit, transaction riêng,
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
    private final ResidenceRegistrationStore registrations;
    private final ContractReadApi contracts;
    private final ResidentApi residents;
    private final BusinessTime time;

    RegistrationObligationListeners(
            RegistrationObligations obligations,
            ResidenceRegistrationStore registrations,
            ContractReadApi contracts,
            ResidentApi residents,
            BusinessTime time) {
        this.obligations = obligations;
        this.registrations = registrations;
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

    /**
     * Phụ lục đã áp dụng (T012), nguồn mốc là mã phụ lục:
     *
     * <ul>
     *   <li>đổi loại lưu trú từ bán trú sang nội trú (FR-001a, Q-336): mốc {@code RESIDENTIAL_EFFECTIVE} là ngày hiệu lực
     *       của phụ lục, kể cả ở tương lai; loại trước, sau đọc theo phiên bản nội dung hợp đồng tại ngày;
     *   <li>gia hạn hợp đồng nội trú khi người đó chỉ có Thông báo lưu trú (FR-007, BR-M02-14): mốc {@code
     *       RENEWAL_ADDENDUM} là lúc phụ lục được áp dụng (sự kiện không mang thời điểm, lấy lúc nghe như {@link
     *       AdmissionCompleted}). Người có Đăng ký tạm trú Đã xác nhận được nhắc gia hạn theo ngày hết hạn (FR-006); người
     *       chưa có khai báo nào đã có nghĩa vụ đang mở, loại tính theo hợp đồng lúc lập.
     * </ul>
     */
    @ApplicationModuleListener
    void on(AddendumApplied event) {
        if (NOT_RESIDING.contains(residents.status(event.residentId()))) {
            return;
        }
        if (event.changeTypes().contains("RESIDENCE_TYPE")
                && typeAt(event.residentId(), event.effectiveDate().minusDays(1)) == ResidenceType.DAY_CARE
                && residential(typeAt(event.residentId(), event.effectiveDate()))) {
            obligations.record(
                    event.residentId(),
                    ObligationTrigger.RESIDENTIAL_EFFECTIVE,
                    event.effectiveDate().atStartOfDay(BusinessTime.ZONE).toInstant(),
                    event.addendumId());
            return;
        }
        if (event.changeTypes().contains("DURATION")
                && residential(typeAt(event.residentId(), event.effectiveDate()))
                && onlyStayNotification(event.residentId())) {
            obligations.record(event.residentId(), ObligationTrigger.RENEWAL_ADDENDUM, time.now(), event.addendumId());
        }
    }

    /** Loại lưu trú theo nội dung hợp đồng hiệu lực tại ngày; không có thì theo hợp đồng của sự kiện đã ghi. */
    private ResidenceType typeAt(long residentId, LocalDate date) {
        return contracts
                .contentAtDate(residentId, date)
                .map(content -> content.content().get("residenceType"))
                .filter(node -> node != null && !node.isNull())
                .map(node -> ResidenceType.valueOf(node.asString()))
                .orElseGet(() -> contracts.ofResident(residentId).stream()
                        .filter(c -> SIGNED.contains(c.status()))
                        .filter(c -> !c.startDate().isAfter(date))
                        .max(Comparator.comparing(ContractView::startDate))
                        .map(ContractView::residenceType)
                        .orElse(null));
    }

    private static boolean residential(ResidenceType type) {
        return type != null && type != ResidenceType.DAY_CARE;
    }

    /** Có khai báo Thông báo lưu trú (Đã xác nhận hoặc đang xử lý) và không có Đăng ký tạm trú Đã xác nhận. */
    private boolean onlyStayNotification(long residentId) {
        List<ResidenceRegistration> list = registrations.ofResident(residentId);
        boolean confirmedTemporary = list.stream()
                .anyMatch(r -> r.status() == RegistrationStatus.CONFIRMED
                        && r.type() == RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        boolean stayNotification = list.stream()
                .anyMatch(r -> r.type() == RegistrationType.STAY_NOTIFICATION
                        && Set.of(
                                        RegistrationStatus.CONFIRMED,
                                        RegistrationStatus.TO_SUBMIT,
                                        RegistrationStatus.SUBMITTED)
                                .contains(r.status()));
        return stayNotification && !confirmedTemporary;
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
