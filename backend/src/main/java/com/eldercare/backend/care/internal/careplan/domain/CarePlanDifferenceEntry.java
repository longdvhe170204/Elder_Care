package com.eldercare.backend.care.internal.careplan.domain;

import java.util.List;

public record CarePlanDifferenceEntry(CarePlanItem before, CarePlanItem after, List<String> changedFields) {}
