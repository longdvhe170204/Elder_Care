package com.cambae.backend.m09;

import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** Lấy khóa trên chính connection thực hiện INSERT; không đọc LAST_INSERT_ID trên connection khác trong pool. */
final class AssignmentFixtures {
    private AssignmentFixtures() {}

    static long insert(JdbcTemplate jdbc, String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }
}
