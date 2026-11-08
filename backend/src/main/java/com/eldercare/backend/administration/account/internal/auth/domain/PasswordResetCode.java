package com.eldercare.backend.administration.account.internal.auth.domain;

/** Mã đặt lại mật khẩu còn dùng được của một tài khoản (002 T105; FR-052a): băm và số lần đã nhập sai. */
public record PasswordResetCode(long id, byte[] codeHash, int failedAttempts) {}
