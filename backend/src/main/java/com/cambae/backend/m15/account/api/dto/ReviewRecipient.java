package com.cambae.backend.m15.account.api.dto;

import java.util.Set;

/**
 * Người quyết định bản ghi chờ xem lại và người nhận báo đồng bộ sau mất quyền: Trưởng tầng của tầng; Người phụ trách ca
 * khi tầng chưa có Trưởng tầng; Quản lý viện khi bản ghi không gắn tầng (FR-041c, CHK056; 002 research A-11).
 */
public record ReviewRecipient(Set<Long> accountIds) {}
