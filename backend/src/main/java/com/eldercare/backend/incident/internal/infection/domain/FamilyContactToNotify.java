package com.eldercare.backend.incident.internal.infection.domain;

/** Người thân cần Hành chính liên hệ trực tiếp (FR-062, BR-M05-24): họ tên, quan hệ, số điện thoại, nội dung cần báo. */
public record FamilyContactToNotify(String fullName, String relationship, String phoneNumber, String message) {}
