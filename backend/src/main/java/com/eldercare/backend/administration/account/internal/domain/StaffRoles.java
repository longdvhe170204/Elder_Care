package com.eldercare.backend.administration.account.internal.domain;

import com.eldercare.backend.common.Role;
import java.util.Set;

/** Tài khoản nhân viên cùng vai trò đang gán tại một thời điểm (ứng viên người duyệt, FR-031a, BR-M15-09). */
public record StaffRoles(long accountId, long staffId, Set<Role> roles) {}
