package com.eldercare.backend.notification.internal.directcontact.domain;

/** Thông báo tới người thân không tới được qua kênh nào (FR-021). */
public record Unreached(long notificationId, long familyMemberId, Long residentId, String shortContent) {}
