package com.cambae.backend.m15.account.api;

/** Tra cứu tài khoản cho module khác; 000 dùng để lọc người duyệt theo vai trò (bổ sung theo plan 004, Q-338). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public interface AccountApi {

    boolean hasRole(long accountId, Role... roles);
}
