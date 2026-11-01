package com.eldercare.backend.care.web.request;

import java.time.Instant;

public record DayCareCheckInWrite(Long residentId, Instant at) {}
