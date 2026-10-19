package com.cambae.backend.m15.account.api;

import java.time.LocalDate;

/** Giấy phép làm lớp pháp lý không thỏa, trả trong lỗi 403 (002 FR-043; glossary mục 7.2 {@code license}). */
public record LicenseView(String name, LocalDate expiryDate) {}
