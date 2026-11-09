package com.eldercare.backend.billing.internal.period.domain;

import java.time.*;

public record CostPeriodRecord(long id, YearMonth month, String status, LocalDate finalizationDueOn) {}
