package com.eldercare.backend.administration.account.internal.auth.domain;

import java.time.Instant;

/** Thiết bị đang gắn của một tài khoản người cao tuổi (FR-070, BR-M15-24). */
public record Binding(long id, long accountId, String deviceId, Instant boundAt) {}
