package com.eldercare.backend.common.data;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa tuần tự hóa lệnh trên {@code technical_lock} (research R-15, module-interfaces 000 mục 1.8): các transaction cùng
 * {@code lockKey} chạy lần lượt; khóa giữ tới hết transaction của lệnh gọi.
 */
@Component
public class TechnicalLocks {

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    public TechnicalLocks(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(String lockKey) {
        // Trùng khóa chính: ON DUPLICATE KEY UPDATE lấy ngay khóa X của dòng. INSERT IGNORE lấy khóa S, hai lệnh cùng
        // giữ S rồi cùng nâng lên X ở FOR UPDATE thì bế tắc. Dòng mới: khóa X của dòng vừa chèn, lệnh sau chờ commit.
        jdbc.update(
                "INSERT INTO technical_lock (lock_key, version, updated_at) VALUES (?, 0, ?)"
                        + " ON DUPLICATE KEY UPDATE lock_key = lock_key",
                lockKey,
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC));
    }
}
