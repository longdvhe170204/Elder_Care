package com.eldercare.backend.administration.account.api.dto;

/** Nhóm thông tin của mục 19.3 để giới hạn xem theo trường (002 research, {@code NhomThongTin}). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public enum InfoGroup {
    IDENTITY,
    CONTACT,
    CARE_LEVEL,
    HEALTH,
    MEDICATION,
    WISHES,
    COSTS,
    FINANCE,
    INTERNAL_PUBLIC,
    STAFF_CONTACT,
    STAFF_RESTRICTED,
    STAFF_REASON
}
