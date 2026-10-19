package com.cambae.backend.m15.account.api;

import java.util.Set;

/** Người dùng đang thực hiện lệnh; Bộ lập lịch có bản riêng với {@code isScheduler() = true} (FR-045). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public interface CurrentUser {

    long accountId();

    Set<Role> roles();

    boolean isScheduler();
}
