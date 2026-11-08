package com.eldercare.backend.billing.internal.pricing.domain;

import java.math.BigDecimal;
import java.util.*;

public record CalculatedCost(
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal factor,
        BigDecimal amount,
        Object basis,
        Map<String, Object> sourceValues,
        Set<String> flags) {}
