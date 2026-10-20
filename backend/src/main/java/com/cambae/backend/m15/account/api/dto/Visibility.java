package com.cambae.backend.m15.account.api.dto;

/** Mức được xem một nhóm thông tin: đầy đủ, chỉ biết có hay không, không xem. 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public enum Visibility {
    FULL,
    PRESENCE_ONLY,
    NONE
}
