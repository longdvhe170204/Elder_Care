package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.roombedallocation.internal.allocation.config.AllocationCurrentReadPool;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

/**
 * Chỉ dùng sau khóa, trước lần ghi phân bổ đầu tiên của COMPLETE_ADMISSION. Đọc toàn bộ view và đính chính đã commit
 * bằng connection RC chỉ đọc; parent chưa có thay đổi phân bổ nào cần thấy ngoài FUTURE đã được loại bằng id.
 */
@Repository
public class CurrentAllocationOverlapStore {
    private final AllocationCurrentReadPool reads;

    public CurrentAllocationOverlapStore(AllocationCurrentReadPool reads) {
        this.reads = reads;
    }

    public boolean overlaps(long bedId, Instant start, Instant end, Long excludedId) {
        String sql =
                "SELECT COUNT(*) FROM v_current_allocation WHERE bed_id=? AND status<>'CANCELLED' AND (? IS NULL OR id<>?) AND start_at<? AND effective_end>?";
        try (var connection = reads.open();
                var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bedId);
            statement.setObject(2, excludedId);
            statement.setObject(3, excludedId);
            statement.setObject(
                    4,
                    end == null ? LocalDateTime.of(9999, 12, 31, 0, 0) : LocalDateTime.ofInstant(end, ZoneOffset.UTC));
            statement.setObject(5, LocalDateTime.ofInstant(start, ZoneOffset.UTC));
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) > 0;
            }
        } catch (SQLException error) {
            throw new DataAccessResourceFailureException("Không thể kiểm khoảng phân bổ hiện hành", error);
        }
    }
}
