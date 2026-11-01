package com.eldercare.backend.care.web.request;

import java.time.Instant;

public record DayCareCheckOutWrite(Long pickupRecordId, Instant at) {}
