package com.eldercare.backend.billing.internal.finalperiod.service;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.admission.api.*;
import com.eldercare.backend.billing.internal.period.domain.BillingResidentState;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.*;
import org.springframework.stereotype.Service;

/** BR-M11-08: phân biệt ngày dự kiến của hồ sơ đang chuẩn bị với mốc dừng thực tế. */
@Service
public class FinalBillingWindow {
    private final ResidentModuleStateApi states;
    private final ResidentApi residents;
    private final ResidencyEndingReadApi endings;
    private final ResidencyDayApi days;
    private final BusinessTime time;
    private final ContractReadApi contracts;

    public FinalBillingWindow(
            ResidentModuleStateApi states,
            ResidentApi residents,
            ResidencyEndingReadApi endings,
            ResidencyDayApi days,
            BusinessTime time,
            ContractReadApi contracts) {
        this.states = states;
        this.residents = residents;
        this.endings = endings;
        this.days = days;
        this.time = time;
        this.contracts = contracts;
    }

    public boolean overdueBillable(long resident, LocalDate date) {
        if (residents.isFinalStatus(resident) || !date.isBefore(time.today())) return false;
        return endings.preparing(resident)
                .filter(e -> e.expectedEndDate().isBefore(time.today()) && date.isAfter(e.expectedEndDate()))
                .filter(e -> days.billingStopDate(resident)
                        .filter(e.expectedEndDate()::equals)
                        .isPresent())
                .isPresent();
    }

    /**
     * FR-011: ngày phát sinh của nguồn sự kiện nằm ngoài thời gian tính phí khi chưa có nội dung hợp đồng hiệu lực tại ngày
     * (trước ngày bắt đầu hợp đồng) hoặc sau mốc dừng tính phí của 004 (ngày kết thúc dự kiến, Hủy tiếp nhận, ngày qua
     * đời); ngày mốc dừng vẫn tính. Hồ sơ kết thúc quá ngày dự kiến mà người chưa ở trạng thái cuối giữ như FR-035.
     */
    public boolean outsideEventPeriod(long resident, LocalDate date) {
        if (contracts.contentAtDate(resident, date).isEmpty()) return true;
        return days.billingStopDate(resident).filter(date::isAfter).isPresent() && !overdueBillable(resident, date);
    }

    public boolean afterDeath(long resident, LocalDate date, Instant occurred, boolean dayOnly) {
        var state = states.get(BillingResidentState.KIND, resident).orElseGet(BillingResidentState::empty);
        if (state.deathAt() == null) return false;
        return dayOnly
                ? date.isAfter(LocalDate.ofInstant(state.deathAt(), BusinessTime.ZONE))
                : occurred.isAfter(state.deathAt());
    }

    public boolean advanceAllowed(long resident, LocalDate date) {
        var state = states.get(BillingResidentState.KIND, resident).orElseGet(BillingResidentState::empty);
        return state.finalStatementId() != null
                && state.throughDate() != null
                && !date.isAfter(state.throughDate())
                && !afterDeath(resident, date, null, true);
    }
}
