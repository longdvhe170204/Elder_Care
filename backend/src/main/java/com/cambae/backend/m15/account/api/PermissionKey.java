package com.cambae.backend.m15.account.api;

/**
 * Khóa quyền: chức năng (một dòng Phụ lục 27) và thao tác. 002 đổi hai thành phần sang enum {@code Function},
 * {@code Operation} (002 research, "Khóa quyền"). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật.
 */
public record PermissionKey(String function, String operation) {}
