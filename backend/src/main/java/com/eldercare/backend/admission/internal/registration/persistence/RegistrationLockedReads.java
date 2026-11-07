package com.eldercare.backend.admission.internal.registration.persistence;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc khóa mã dòng {@code simple_record} của 018 ({@code SELECT … FOR UPDATE}): đọc bản mới nhất đã commit, không theo ảnh
 * chụp của transaction, vì bên gọi vừa chờ khóa theo người của lệnh khác. Sau đó gọi {@code SimpleRecordApi.lockForUpdate}
 * để lấy nội dung mới nhất.
 */
@Component
class RegistrationLockedReads {

    private final JdbcTemplate jdbc;

    RegistrationLockedReads(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mã dòng loại {@code kind} thỏa {@code condition} (đã có tham số {@code ?}), tăng dần theo mã. */
    @Transactional(propagation = Propagation.MANDATORY)
    List<Long> ids(String kind, String condition, Object... args) {
        List<Object> all = new ArrayList<>();
        all.add(kind);
        all.addAll(List.of(args));
        return jdbc.queryForList(
                "SELECT id FROM simple_record WHERE kind = ? AND " + condition + " ORDER BY id FOR UPDATE",
                Long.class,
                all.toArray());
    }
}
