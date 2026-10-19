package com.cambae.backend.m15.approval.api;

import java.util.Map;
import java.util.Set;

/**
 * Chuyển trạng thái riêng của một loại yêu cầu (data-model mục 3, "Một loại yêu cầu có thể khai báo thêm"), ví dụ
 * {@code DUOC_THAY_THE} của 004, các trạng thái của 015. Chỉ hệ thống chuyển, kèm lệnh của actor ở module sở hữu; vẫn
 * qua kiểm tra chung (nhật ký, khóa lạc quan).
 */
public record ExtendedTransitions(Map<String, Transition> byCode) {

    public static final ExtendedTransitions NONE = new ExtendedTransitions(Map.of());

    public record Transition(Set<String> from, String to) {}
}
