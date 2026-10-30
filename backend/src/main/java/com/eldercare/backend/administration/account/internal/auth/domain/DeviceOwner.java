package com.eldercare.backend.administration.account.internal.auth.domain;

/** Kết quả tra token thiết bị: người cao tuổi, tài khoản, thiết bị và trạng thái tài khoản lúc tra (FR-071). */
public record DeviceOwner(long residentId, long accountId, String deviceId, String accountStatus) {}
