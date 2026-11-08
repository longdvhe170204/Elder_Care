package com.eldercare.backend.care.internal.session.domain;

/** 014 T009: buổi trong danh sách kèm số đăng ký còn hiệu lực (schema {@code ActivitySession}). */
public record ListedSession(Session session, int registrationCount) {}
