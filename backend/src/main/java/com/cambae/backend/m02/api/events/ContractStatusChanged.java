package com.cambae.backend.m02.api.events;

import com.cambae.backend.m02.api.dto.ContractStatus;

/** Hợp đồng đổi trạng thái; 017, 018, 010, 012 nghe (FR-033a). {@code reason} rỗng với chuyển không cần lý do. */
public record ContractStatusChanged(
        long contractId, long residentId, ContractStatus from, ContractStatus to, String reason) {}
