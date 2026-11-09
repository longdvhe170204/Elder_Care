package com.eldercare.backend.billing.internal.provision.domain;

import com.eldercare.backend.billing.api.dto.CostType;
import java.time.LocalDate;

public record ProvisionalItem(CostType type, String origin, String status, Long amount, LocalDate date) {}
