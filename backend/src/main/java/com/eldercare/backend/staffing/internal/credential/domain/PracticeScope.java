package com.eldercare.backend.staffing.internal.credential.domain;
/** Danh mục phạm vi, cùng mã logic dùng khi ghi chứng chỉ. */
public record PracticeScope(long id, String name, boolean active, String code) {}
