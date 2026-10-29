package com.eldercare.backend.administration.account.internal.domain;

import java.time.Instant;

/** Thiết bị đang gắn của tài khoản người cao tuổi, để hiển thị (FR-070; contracts {@code Account.boundDevice}). */
public record DeviceBinding(Instant boundAt, String deviceId) {}
