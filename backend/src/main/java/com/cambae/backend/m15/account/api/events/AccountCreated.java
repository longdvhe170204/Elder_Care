package com.cambae.backend.m15.account.api.events;

import com.cambae.backend.m15.account.api.dto.AccountType;

/** Tài khoản đã tạo; không gửi thông báo, dùng cho báo cáo (FR-015). */
public record AccountCreated(long accountId, AccountType type) {}
