package com.eldercare.backend.administration.retention.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Kết nối bằng tài khoản {@code eldercare_retention}, tài khoản duy nhất được xóa bảng nhóm 3 và nhật ký (research R-05,
 * R-11; FR-051). Chỉ job loại bỏ dữ liệu hết hạn và {@link com.eldercare.backend.administration.retention.api.spi.RetentionProvider#delete} dùng. Không đăng ký thành bean
 * {@code DataSource} để không thay DataSource chính của ứng dụng; pool chỉ mở kết nối khi job chạy.
 */
public interface RetentionDatabase {

    JdbcTemplate jdbc();

    /** Transaction trên kết nối {@code eldercare_retention}; mỗi chuỗi bản ghi xóa trong một transaction (BR-M15-13). */
    TransactionTemplate transaction();
}
