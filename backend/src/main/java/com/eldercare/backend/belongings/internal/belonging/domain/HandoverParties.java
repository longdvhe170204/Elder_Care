package com.eldercare.backend.belongings.internal.belonging.domain;

/**
 * Mã người giao, người nhận của một bản ghi bàn giao, để tra tên khi bản ghi chỉ lưu mã (FR-011, FR-020; test luồng
 * BF-14 2026-11-23).
 */
public record HandoverParties(String giverType, Long giverId, String recipientType, Long recipientId) {}
