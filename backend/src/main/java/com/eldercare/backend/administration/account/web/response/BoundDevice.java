package com.eldercare.backend.administration.account.web.response;

import java.time.Instant;

/** Thiết bị gắn của tài khoản người cao tuổi (FR-070, US8). */
public record BoundDevice(Instant boundAt, String deviceId) {}
