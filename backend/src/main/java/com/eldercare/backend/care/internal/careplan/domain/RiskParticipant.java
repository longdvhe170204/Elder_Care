package com.eldercare.backend.care.internal.careplan.domain;

/** Nội dung record_event; active chỉ quản lý danh sách của bản Nháp. */
public record RiskParticipant(String type, long id, boolean active) {}
