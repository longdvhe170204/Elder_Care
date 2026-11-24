package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/**
 * Việc báo người nhà của một sự cố (BF-06): {@code INFORMED} đã báo (ai, lúc nào, cách nào), {@code NOT_YET} chưa báo
 * (hệ thống gửi thông báo thông tin cho người liên hệ chính và nhắc người ghi gọi điện). {@code method}: PHONE, IN_PERSON,
 * MESSAGE.
 */
public record FamilyNotice(String state, Instant at, String method, String note) {
    public static final String INFORMED = "INFORMED";
    public static final String NOT_YET = "NOT_YET";
}
