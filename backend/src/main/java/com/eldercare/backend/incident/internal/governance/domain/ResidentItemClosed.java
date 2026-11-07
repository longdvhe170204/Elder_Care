package com.eldercare.backend.incident.internal.governance.domain;

/**
 * Sự kiện nội bộ của {@code incident}: một cảnh báo hoặc sự cố của người cao tuổi vừa đóng hoặc hủy; nghe đồng bộ trong
 * cùng transaction để tính lại danh sách việc sau qua đời (research S-17).
 */
public record ResidentItemClosed(long residentId) {}
