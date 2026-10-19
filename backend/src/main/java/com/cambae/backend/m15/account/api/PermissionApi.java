package com.cambae.backend.m15.account.api;

/** Kiểm quyền ba lớp BR-M15-01 (vai trò, phạm vi, pháp lý). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public interface PermissionApi {

    PermissionResult hasPermission(CurrentUser user, PermissionKey key, Target target);
}
