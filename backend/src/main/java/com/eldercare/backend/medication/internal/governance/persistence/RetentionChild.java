package com.eldercare.backend.medication.internal.governance.persistence;

/** Bản ghi của loại {@code dataType} đọc bằng {@code sql} (một tham số là id bản ghi bị trỏ tới). */
public record RetentionChild(String dataType, String sql) {}
