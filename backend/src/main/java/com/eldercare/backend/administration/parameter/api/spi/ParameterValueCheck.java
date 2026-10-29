package com.eldercare.backend.administration.parameter.api.spi;

import com.eldercare.backend.administration.parameter.api.ParameterCode;
import tools.jackson.databind.JsonNode;

/**
 * Kiểm giá trị riêng của một tham số (tên logic {@code KiemTraGiaTriThamSo}; 017 research E-04), do module sở hữu dữ
 * liệu liên quan cài, ví dụ 017 cho CFG-M11-10 (mã BIN có trong danh mục ngân hàng). Lệnh đổi giá trị của 000 gọi mọi
 * bản cài có {@link #code()} trùng mã đang đổi, sau phần kiểm chung theo kiểu và ràng buộc, trước khi lưu; khôi phục về
 * "Theo cơ sở" (giá trị {@code null}) thì không gọi.
 */
public interface ParameterValueCheck {

    /** Mã tham số bản cài này kiểm. */
    ParameterCode code();

    /**
     * Giá trị mới dạng JSON như sẽ lưu. Không nhận thì ném {@code InvalidValueException} (422 {@code INVALID_VALUE})
     * với lý do hiện cho Quản lý viện; lệnh đổi không lưu gì.
     */
    void check(JsonNode value);
}
