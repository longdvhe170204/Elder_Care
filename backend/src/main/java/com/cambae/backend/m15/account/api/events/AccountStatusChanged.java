package com.cambae.backend.m15.account.api.events;

import com.cambae.backend.m15.account.api.AccountStatus;

/** Tài khoản đổi trạng thái; không gửi thông báo, app nhận qua phiên kết thúc (FR-015). */
public record AccountStatusChanged(long accountId, AccountStatus from, AccountStatus to, String cause) {}
