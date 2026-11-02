package com.eldercare.backend.admission.internal.deposit.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.DepositAmountView;
import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.admission.api.dto.DepositStatusView;
import com.eldercare.backend.admission.api.spi.DepositStatusPort;
import com.eldercare.backend.admission.internal.application.service.M02ActorNames;
import com.eldercare.backend.common.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khoản cần đặt cọc kèm trạng thái đặt cọc của hợp đồng (FR-030, FR-031). Khoản do 004 giữ trên hợp đồng; trạng thái do
 * 017 tính, đọc qua {@link DepositStatusPort}. Chưa có bản cài của 017 thì trạng thái là Chưa đáp ứng, giống điều kiện
 * (c) của danh sách điều kiện tiếp nhận. Quyền xem kiểm ở nơi gọi (cùng quyền xem hợp đồng).
 */
@Service
public class DepositViewService {

    /** Căn cứ trạng thái: giao dịch làm đổi gần nhất, Kế toán lập; rỗng khi chưa có giao dịch. */
    public record Basis(Instant at, Long transactionId, String accountant) {}

    public record View(boolean notRequired, BigDecimal requiredAmount, DepositStatus status, Basis basis) {}

    private final ContractReadApi contracts;
    private final ObjectProvider<DepositStatusPort> port;
    private final M02ActorNames actorNames;

    DepositViewService(ContractReadApi contracts, ObjectProvider<DepositStatusPort> port, M02ActorNames actorNames) {
        this.contracts = contracts;
        this.port = port;
        this.actorNames = actorNames;
    }

    @Transactional(readOnly = true)
    public View view(long contractId) {
        DepositAmountView amount = contracts.depositAmount(contractId).orElseThrow(NotFoundException::new);
        DepositStatusPort source = port.getIfAvailable();
        DepositStatusView state = source == null
                ? new DepositStatusView(DepositStatus.NOT_SATISFIED, null, null, null)
                : source.status(contractId);
        return new View(amount.notRequired(), amount.amount(), state.status(), basisOf(state));
    }

    private Basis basisOf(DepositStatusView state) {
        if (state.at() == null && state.transactionId() == null && state.accountantId() == null) {
            return null;
        }
        return new Basis(
                state.at(),
                state.transactionId(),
                state.accountantId() == null ? null : actorNames.of(state.accountantId()));
    }
}
