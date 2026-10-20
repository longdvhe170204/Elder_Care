package com.cambae.backend.m15.account.auth.internal;

import com.cambae.backend.m15.account.api.AccessChannel;

/** Phiên đang mở của yêu cầu hiện tại (002 research A-02). */
public record ActiveSession(long accountId, String deviceId, AccessChannel channel) {}
