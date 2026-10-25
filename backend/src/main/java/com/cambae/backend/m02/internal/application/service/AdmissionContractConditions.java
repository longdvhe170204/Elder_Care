package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.DepositStatus;
import com.cambae.backend.m02.api.spi.DepositStatusPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Ba điều kiện tiếp nhận do 004 kiểm (FR-004 (b), (c), (e); FR-075; BR-M02-22): hợp đồng Hiệu lực và thời điểm tiếp
 * nhận không sớm hơn ngày bắt đầu; đặt cọc Đã đáp ứng hoặc Không yêu cầu (theo 017); mức chăm sóc của hợp đồng trùng
 * mức hiện hành của 001. Một nguồn cho bean tham gia lệnh Hoàn tất tiếp nhận và danh sách điều kiện (research T-09).
 * Hợp đồng đọc qua {@link ContractReadApi} (bản cài ở user story hợp đồng) nên chưa có hợp đồng thì (b), (c), (e) Chưa
 * đạt; chưa có bản cài đặt cọc của 017 thì (c) Chưa đạt.
 */
@Component
public class AdmissionContractConditions {

    public static final String CONTRACT = "ADM_B_CONTRACT";
    public static final String DEPOSIT = "ADM_C_DEPOSIT";
    public static final String CONTRACT_LEVEL = "ADM_E_CONTRACT_LEVEL";

    private final ObjectProvider<ContractReadApi> contracts;
    private final ObjectProvider<DepositStatusPort> deposits;
    private final CareLevelApi careLevels;

    AdmissionContractConditions(
            ObjectProvider<ContractReadApi> contracts,
            ObjectProvider<DepositStatusPort> deposits,
            CareLevelApi careLevels) {
        this.contracts = contracts;
        this.deposits = deposits;
        this.careLevels = careLevels;
    }

    /** Hợp đồng Hiệu lực của người cao tuổi, nếu có. */
    public Optional<ContractView> activeContract(long residentId) {
        ContractReadApi api = contracts.getIfAvailable();
        if (api == null) {
            return Optional.empty();
        }
        return api.ofResident(residentId).stream()
                .filter(contract -> contract.status() == ContractStatus.ACTIVE)
                .findFirst();
    }

    /** Hợp đồng chưa hủy của người cao tuổi (tiến trình FR-003 tính "đã xác định" từ đây). */
    public Optional<ContractView> latestContract(long residentId) {
        ContractReadApi api = contracts.getIfAvailable();
        if (api == null) {
            return Optional.empty();
        }
        return api.ofResident(residentId).stream()
                .filter(contract -> contract.status() != ContractStatus.CANCELLED)
                .reduce((first, second) -> second);
    }

    /** Điều kiện (b), (c), (e) chưa thỏa tại thời điểm {@code at}; rỗng là thỏa. */
    public List<Condition> unmet(long residentId, Instant at) {
        List<Condition> unmet = new ArrayList<>();
        Optional<ContractView> contract = activeContract(residentId);
        if (contract.isEmpty()) {
            unmet.add(new Condition(CONTRACT, "Chưa có hợp đồng Hiệu lực", "FR-004 (b)", false));
            unmet.add(new Condition(DEPOSIT, "Chưa có hợp đồng nên chưa có đặt cọc", "FR-004 (c)", false));
        } else {
            LocalDate today = LocalDate.ofInstant(at, BusinessTime.ZONE);
            if (today.isBefore(contract.get().startDate())) {
                unmet.add(new Condition(
                        CONTRACT,
                        "Thời điểm tiếp nhận sớm hơn ngày bắt đầu hợp đồng "
                                + contract.get().startDate(),
                        "FR-075",
                        false));
            }
            if (!depositSatisfied(contract.get().id())) {
                unmet.add(new Condition(DEPOSIT, "Đặt cọc chưa đáp ứng", "FR-004 (c)", false));
            }
        }
        Optional<CareLevel> current = careLevels.current(residentId);
        if (current.isEmpty()) {
            unmet.add(new Condition(CONTRACT_LEVEL, "Chưa có mức chăm sóc hiện hành", "BR-M02-22", false));
        } else if (contract.isEmpty()) {
            unmet.add(new Condition(CONTRACT_LEVEL, "Chưa có hợp đồng để đối chiếu mức chăm sóc", "BR-M02-22", false));
        } else if (contract.get().careLevel() != current.get()) {
            unmet.add(new Condition(
                    CONTRACT_LEVEL,
                    "Mức chăm sóc của hợp đồng (" + contract.get().careLevel() + ") khác mức hiện hành ("
                            + current.get() + ")",
                    "BR-M02-22",
                    false));
        }
        return unmet;
    }

    private boolean depositSatisfied(long contractId) {
        DepositStatusPort port = deposits.getIfAvailable();
        if (port == null) {
            return false;
        }
        DepositStatus status = port.status(contractId).status();
        return status == DepositStatus.SATISFIED || status == DepositStatus.NOT_REQUIRED;
    }
}
