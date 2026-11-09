package com.eldercare.backend.admission.internal.deposit.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.admission.api.events.DepositNotSatisfiedAfterAdmission;
import com.eldercare.backend.admission.api.spi.DepositStatusPort;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đặt cọc về Chưa đáp ứng sau khi người cao tuổi đã Đang lưu trú (FR-032): báo Quản lý viện, không đổi trạng thái lưu
 * trú. {@link DepositStatusListener} gọi khi 017 báo trạng thái đặt cọc của hợp đồng đổi; trạng thái luôn đọc lại qua
 * {@link DepositStatusPort}.
 */
@Service
public class DepositRecheckService {

    private final ContractReadApi contracts;
    private final ObjectProvider<DepositStatusPort> port;
    private final ResidentApi residents;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    DepositRecheckService(
            ContractReadApi contracts,
            ObjectProvider<DepositStatusPort> port,
            ResidentApi residents,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.contracts = contracts;
        this.port = port;
        this.residents = residents;
        this.events = events;
        this.time = time;
    }

    @Transactional
    public void recheck(long contractId) {
        DepositStatusPort source = port.getIfAvailable();
        ContractView contract = contracts.contract(contractId).orElse(null);
        if (source == null
                || contract == null
                || (contract.status() != ContractStatus.ACTIVE && contract.status() != ContractStatus.PENDING_SIGNATURE)
                || residents.status(contract.residentId()) != ResidentStatus.IN_RESIDENCE
                || source.status(contractId).status() != DepositStatus.NOT_SATISFIED) {
            return;
        }
        events.publishEvent(new DepositNotSatisfiedAfterAdmission(contractId, contract.residentId(), time.now()));
    }
}
