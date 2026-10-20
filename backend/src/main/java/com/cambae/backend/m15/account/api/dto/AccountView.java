package com.cambae.backend.m15.account.api.dto;

import com.cambae.backend.common.Role;
import java.util.Set;

/** Tài khoản cho module khác đọc: 008 lấy vai trò để xếp ca, 009 lấy trạng thái (002 contracts/module-interfaces.md mục 1.6). */
public record AccountView(long accountId, AccountType type, AccountStatus status, Set<Role> roles) {}
