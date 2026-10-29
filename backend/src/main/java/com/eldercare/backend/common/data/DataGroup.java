package com.eldercare.backend.common.data;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Nhóm dữ liệu của entity, quyết định thao tác được phép (mục 1.5; FR-001, research R-05). */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataGroup {

    Group value();

    enum Group {
        /** Nhóm 1 – Danh mục: tạo, sửa, ngừng hiệu lực; xóa khi chưa được tham chiếu (FR-002). */
        CATALOG,
        /** Nhóm 2 – Nghiệp vụ có trạng thái: chỉ đổi qua lệnh nghiệp vụ hoặc phiên bản mới (FR-005). */
        BUSINESS,
        /** Nhóm 3 – Ghi nhận đã xác nhận: chỉ thêm và đính chính (FR-009). */
        RECORD
    }
}
