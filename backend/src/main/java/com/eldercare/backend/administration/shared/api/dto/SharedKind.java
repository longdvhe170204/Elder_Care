package com.eldercare.backend.administration.shared.api.dto;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Loại dòng của bảng dùng chung (research R-15, module-interfaces 000 mục 1.8): mã {@code <module>.<KIND>}, ví dụ
 * {@code M03.FLOOR}; cột {@code module} của dòng lấy từ tiền tố. Nội dung {@code JSON} của dòng là record {@code content}
 * của module sở hữu ({@code Void} khi loại không có nội dung). Hằng khai ở gói của module sở hữu ({@code SharedKindArchTest}).
 *
 * @param sharedRead module khác được đọc dòng của loại này (hằng khi đó đặt ở gói {@code api} của module)
 * @param deletable mục danh mục được xóa khi chưa bị tham chiếu; {@code false} với loại có tham chiếu không qua khóa ngoại
 *     (FR-002)
 */
public record SharedKind<T>(String code, Class<T> content, boolean sharedRead, boolean deletable) {

    private static final Pattern CODE = Pattern.compile("M(0[1-9]|1[0-5])\\.[A-Z][A-Z0-9_]*");
    public static final int MAX_CODE_LENGTH = 40;

    public SharedKind {
        Objects.requireNonNull(content, "content");
        if (code == null
                || code.length() > MAX_CODE_LENGTH
                || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Mã loại phải dạng M01.KIND … M15.KIND, tối đa 40 ký tự: " + code);
        }
    }

    public static <T> SharedKind<T> of(String code, Class<T> content) {
        return new SharedKind<>(code, content, false, true);
    }

    public SharedKind<T> readableByOtherModules() {
        return new SharedKind<>(code, content, true, deletable);
    }

    public SharedKind<T> notDeletable() {
        return new SharedKind<>(code, content, sharedRead, false);
    }

    /** Mã module sở hữu, ví dụ {@code M03}. */
    public String module() {
        return code.substring(0, code.indexOf('.'));
    }
}
