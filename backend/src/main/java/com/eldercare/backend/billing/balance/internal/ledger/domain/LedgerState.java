package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.billing.api.dto.BalanceStatus;
import java.time.LocalDate;

/**
 * Một dòng {@code resident_balance} (data-model 017 mục 2): hai sổ, mã nộp tiền, tình trạng và chu kỳ báo (FR-016 →
 * FR-020), trạng thái đặt cọc đã báo 004 (FR-006). Tiền nguyên đồng, có dấu.
 */
public record LedgerState(
        long residentId,
        long openingContractId,
        String paymentCode,
        long balance,
        long depositBalance,
        BalanceStatus status,
        Integer daysCovered,
        LocalDate cycleStartedOn,
        LocalDate lastNoticeOn,
        LocalDate inDebtSince,
        boolean remindersStopped,
        DepositStatus depositStatus,
        Long depositStatusContractId,
        int version) {}
