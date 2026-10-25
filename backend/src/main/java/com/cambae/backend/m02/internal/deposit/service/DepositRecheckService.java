package com.cambae.backend.m02.internal.deposit.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.DepositStatus;
import com.cambae.backend.m02.api.events.DepositNotSatisfiedAfterAdmission;
import com.cambae.backend.m02.api.spi.DepositStatusPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đặt cọc về Chưa đáp ứng sau khi người cao tuổi đã Đang lưu trú (FR-032): báo Quản lý viện, không đổi trạng thái lưu
 * trú. Gọi khi 017 báo trạng thái đặt cọc của hợp đồng đổi; trạng thái luôn đọc lại qua {@link DepositStatusPort}.
 * Listener của sự kiện 017 chờ {@code m11.api} (team-plan mục 8).
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
