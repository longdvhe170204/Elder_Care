package com.eldercare.backend.care.web.request;

import java.time.Instant;

public record CareTaskReworkWrite(Long qualityCheckResultId, Instant plannedAt, Long performerId, Boolean shared) {}
