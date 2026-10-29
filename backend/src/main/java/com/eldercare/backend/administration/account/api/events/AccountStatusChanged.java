package com.eldercare.backend.administration.account.api.events;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;

/** Tài khoản đổi trạng thái; không gửi thông báo, app nhận qua phiên kết thúc (FR-015). */
public record AccountStatusChanged(long accountId, AccountStatus from, AccountStatus to, String cause) {}
