/**
 * Phần kỹ thuật dùng chung (đồng hồ, mô hình lỗi, kiểu cơ sở, chú thích nhóm dữ liệu), không chứa dữ liệu
 * nghiệp vụ (research R-02). Mở cho mọi module dùng trực tiếp.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.eldercare.backend.common;

import org.springframework.modulith.ApplicationModule;
