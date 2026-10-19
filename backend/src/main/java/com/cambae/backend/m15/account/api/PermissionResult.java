package com.cambae.backend.m15.account.api;

/** Kết quả kiểm quyền ba lớp BR-M15-01; 002 thêm lớp không thỏa và căn cứ. 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public record PermissionResult(boolean allowed) {}
