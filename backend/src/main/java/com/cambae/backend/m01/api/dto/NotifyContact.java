package com.cambae.backend.m01.api.dto;

/** Người thân cần báo khi chuyển viện; họ tên và số điện thoại lấy từ hồ sơ người thân của 012 (FR-023j). */
public record NotifyContact(long familyMemberId, String fullName, String phoneNumber) {}
