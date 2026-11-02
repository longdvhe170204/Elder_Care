package com.eldercare.backend.admission.internal.contract.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.events.ContractOverdueEscalated;
import com.eldercare.backend.admission.internal.contract.domain.ExpiringContract;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code CONTRACTS} của job hằng ngày (FR-029, BR-M02-05, CFG-M02-10; research T-11): hợp đồng Hiệu lực qua ngày kết
 * thúc hiện hành mà người cao tuổi vẫn lưu trú thì giữ Hiệu lực và gắn dấu "quá hạn hợp đồng" từ ngày liền sau ngày kết
 * thúc (đúng ngày lẽ ra phải đặt, kể cả khi job chạy bù); quá CFG-M02-10 ngày thì báo Quản lý viện một lần cho mỗi hợp
 * đồng; ngày kết thúc hiện hành dời ra sau (phụ lục gia hạn) thì gỡ dấu. Không tự gia hạn, không tự kết thúc hợp đồng.
 */
@Component
@Order(30)
class ContractOverdueTask implements M02DailyTask {

    static final String NAME = "CONTRACTS";

    private static final Set<ResidentStatus> STAYING = Set.of(
            ResidentStatus.IN_RESIDENCE,
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.OFF_SITE_ACTIVITY,
            ResidentStatus.HOSPITALIZED);

    private final ContractStore store;
    private final ResidencyLock residencyLock;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    ContractOverdueTask(
            ContractStore store,
            ResidencyLock residencyLock,
            ResidentApi residents,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.residents = residents;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int run(LocalDate day) {
        long threshold = parameters.read(ParameterCode.CFG_M02_10).asDuration().toDays();
        int changed = 0;
        for (ExpiringContract contract : store.activeWithEndDate()) {
            boolean overdue =
                    day.isAfter(contract.currentEndDate()) && STAYING.contains(residents.status(contract.residentId()));
            if (overdue) {
                residencyLock.lock(contract.residentId());
                LocalDate since = contract.currentEndDate().plusDays(1);
                if (!since.equals(contract.overdueSince())) {
                    store.setOverdueSince(contract.id(), since);
                    changed++;
                }
                long days = ChronoUnit.DAYS.between(contract.currentEndDate(), day);
                if (days > threshold && contract.managerNotifiedOverdueAt() == null) {
                    store.markManagerNotified(contract.id(), time.now());
                    events.publishEvent(
                            new ContractOverdueEscalated(contract.id(), contract.residentId(), (int) days, time.now()));
                    changed++;
                }
            } else if (contract.overdueSince() != null && !day.isAfter(contract.currentEndDate())) {
                residencyLock.lock(contract.residentId());
                store.setOverdueSince(contract.id(), null);
                changed++;
            }
        }
        return changed;
    }
}
