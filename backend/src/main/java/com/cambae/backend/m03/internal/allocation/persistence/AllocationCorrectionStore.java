package com.cambae.backend.m03.internal.allocation.persistence;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m03.internal.allocation.config.AllocationCurrentReadPool;
import com.cambae.backend.m03.internal.allocation.domain.AllocationCorrectionOriginal;
import com.cambae.backend.m03.internal.allocation.domain.AllocationCorrectionSnapshot;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Chỉ đọc bản gốc của M03; kiểm khoảng khác qua view hiện hành dưới khóa ghi phân bổ. */
@Repository
public class AllocationCorrectionStore {
    private final JdbcTemplate jdbc;
    private final AllocationCurrentReadPool reads;

    public AllocationCorrectionStore(JdbcTemplate jdbc, AllocationCurrentReadPool reads) {
        this.jdbc = jdbc;
        this.reads = reads;
    }

    public Optional<AllocationCorrectionOriginal> find(long id) {
        return jdbc
                .query(
                        "SELECT a.*,r.floor_id FROM bed_allocation a JOIN bed b ON b.id=a.bed_id JOIN room r ON r.id=b.room_id WHERE a.id=?",
                        (rs, i) -> new AllocationCorrectionOriginal(
                                rs.getLong("id"),
                                rs.getLong("resident_id"),
                                rs.getLong("bed_id"),
                                rs.getLong("floor_id"),
                                rs.getString("status"),
                                rs.getObject("opened_by_id", Long.class),
                                rs.getTimestamp("start_at").toInstant(),
                                instant(rs, "end_at"),
                                instant(rs, "expected_end")),
                        id)
                .stream()
                .findFirst();
    }

    public AllocationCorrectionOriginal required(long id) {
        return find(id).orElseThrow(NotFoundException::new);
    }

    public boolean overlaps(long id, long resident, long bed, Instant from, Instant to) {
        String sql =
                "SELECT COUNT(*) FROM v_current_allocation WHERE id<>? AND (bed_id=? OR resident_id=?) AND status<>'CANCELLED' AND start_at<? AND effective_end>?";
        var end = to == null ? LocalDateTime.of(9999, 12, 31, 0, 0) : LocalDateTime.ofInstant(to, ZoneOffset.UTC);
        var start = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        if (Integer.valueOf(Connection.TRANSACTION_READ_COMMITTED)
                .equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()))
            return jdbc.queryForObject(sql, Integer.class, id, bed, resident, end, start) > 0;
        // Luồng duyệt RR chưa ghi phân bổ khác; chỉ correction của id này chưa commit và được loại bằng id.
        // Không đọc snapshot RR cũ; bản hiện hành của chính id được tính từ payload vừa ghi ở provider.
        try (var connection = reads.open();
                var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, bed);
            statement.setLong(3, resident);
            statement.setObject(4, end);
            statement.setObject(5, start);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) > 0;
            }
        } catch (SQLException error) {
            throw new DataAccessResourceFailureException("Không thể kiểm khoảng đính chính", error);
        }
    }

    public Optional<AllocationCorrectionSnapshot> latestCommitted(long id) {
        // Chỉ đọc public view; không chép MAX(sequence_no) hoặc đọc bảng riêng của 000.
        try (var connection = reads.open();
                var statement = connection.prepareStatement(
                        "SELECT correction_id,corrected_content FROM v_latest_correction WHERE record_type='M03.BED_ALLOCATION' AND original_record_id=?")) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new AllocationCorrectionSnapshot(result.getLong(1), result.getString(2)))
                        : Optional.empty();
            }
        } catch (SQLException error) {
            throw new DataAccessResourceFailureException("Không thể đọc giá trị trước đính chính", error);
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
