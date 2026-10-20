package com.cambae.backend.m15.account.internal.permission;

import java.util.Set;

/**
 * Phân loại chú thích Phụ lục 27 ở lớp vai trò (chủ dự án duyệt 2026-10-20; plan 002 "Ghi chú triển khai" T022):
 *
 * <ul>
 *   <li>{@link #CONDITIONS}: điều kiện cần dữ liệu lúc chạy; ô chỉ thỏa khi module chủ cài {@code FootnoteCondition} cho
 *       mã đó và điều kiện đúng, chưa cài thì từ chối (¹ bản đồng ý, 001; ², ⁷ người đại diện, 012);
 *   <li>{@link #DUTIES}: điều kiện là nhiệm vụ (¹⁵ Người nhận tại tầng; ⁴⁰ Người phụ trách ca xác nhận giường); lớp vai
 *       trò từ chối, quyền đến từ lớp nhiệm vụ (FR-019g, T053). ⁹ (ô T⁹ của Trưởng tầng, dòng "Xử lý việc quá hạn") không
 *       phải điều kiện của ô: chú thích mở cùng quyền cho Người phụ trách ca (FR-019a "như Trưởng tầng"), nên ô có hiệu
 *       lực với Trưởng tầng và Người phụ trách ca có quyền ở lớp nhiệm vụ (sửa 2026-10-20, T053);
 *   <li>mã khác: chỉ mô tả, lớp vai trò cho qua; ràng buộc kiểm ở giới hạn trường (⁵, ¹⁴, ¹⁶, ³⁰), ngoại lệ sự cố
 *       FR-044a (¹⁰) hoặc ở module sở hữu lệnh (⁸, ¹³, ³⁷, ⁴¹ → ⁴⁴…).
 * </ul>
 *
 * Module thêm điều kiện mới thì thêm mã vào {@link #CONDITIONS} cùng lúc với bản cài.
 */
public final class FootnoteRules {

    public static final Set<String> CONDITIONS = Set.of("1", "2", "7");
    public static final Set<String> DUTIES = Set.of("15", "40");

    private FootnoteRules() {}

    /**
     * Phương án có chú thích chặn ở lớp vai trò bất kể đối tượng: chú thích nhiệm vụ, hoặc chú thích điều kiện chưa có
     * bản cài ({@code registered}: mã đã có {@code FootnoteCondition}).
     */
    public static boolean blockedAtRoleLayer(Set<String> footnotes, Set<String> registered) {
        return footnotes.stream()
                .anyMatch(c -> DUTIES.contains(c) || (CONDITIONS.contains(c) && !registered.contains(c)));
    }
}
