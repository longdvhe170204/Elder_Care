package com.eldercare.backend.admission.internal.ending.service;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Góp ý demo BF-10 (2026-11-27, chủ dự án duyệt): hết hạn hay chấm dứt hợp đồng không do người dùng chọn mà theo ngày kết
 * thúc so với ngày kết thúc của hợp đồng Hiệu lực: không sớm hơn thì hết hạn, sớm hơn (hoặc hợp đồng không có ngày kết
 * thúc) thì chấm dứt. Khớp trạng thái hợp đồng sau lệnh Kết thúc lưu trú (Kết thúc / Chấm dứt). Các trường hợp khác giữ
 * nguyên.
 */
@Component
public class ContractEndCases {

    private static final Set<EndCase> CONTRACT_CASES = Set.of(EndCase.CONTRACT_EXPIRED, EndCase.CONTRACT_TERMINATION);

    private final ContractStore contracts;

    ContractEndCases(ContractStore contracts) {
        this.contracts = contracts;
    }

    public EndCase resolve(long residentId, EndCase requested, LocalDate endDate) {
        if (requested == null || !CONTRACT_CASES.contains(requested)) return requested;
        Optional<LocalDate> contractEnd = contracts.ofResident(residentId).stream()
                .filter(c -> c.status() == ContractStatus.ACTIVE)
                .max(Comparator.comparing((Contract c) -> c.content().startDate()))
                .map(Contract::currentEndDate);
        return contractEnd.isPresent() && !endDate.isBefore(contractEnd.get())
                ? EndCase.CONTRACT_EXPIRED
                : EndCase.CONTRACT_TERMINATION;
    }
}
