package com.eldercare.backend.billing.internal.provision.domain;

import com.eldercare.backend.billing.api.dto.CostStatementView;
import java.time.Instant;

/** FR-042: chi tiết đã chốt cho 017; tên thuốc luôn bị che. */
public record FinalizedCostStatement(long periodId, Instant finalizedAt, CostStatementView statement) {}
