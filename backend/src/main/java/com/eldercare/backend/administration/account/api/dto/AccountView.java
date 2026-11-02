package com.eldercare.backend.administration.account.api.dto;

import com.eldercare.backend.common.Role;
import java.util.Set;

/**
 * Tài khoản cho module khác đọc: 008 lấy vai trò để xếp ca, 009 lấy trạng thái (002 contracts/module-interfaces.md mục
 * 1.6). {@code staffId} là hồ sơ nhân viên liên kết, {@code null} với tài khoản người thân, người cao tuổi.
 */
public record AccountView(long accountId, AccountType type, AccountStatus status, Set<Role> roles, Long staffId) {}
