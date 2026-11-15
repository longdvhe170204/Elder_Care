package com.eldercare.backend.administration.correction.internal.persistence;

import com.eldercare.backend.administration.correction.internal.domain.ChainState;
import com.eldercare.backend.common.data.TechnicalLocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Chuỗi đính chính của từng bản gốc, đọc từ {@code correction} (số thứ tự, Hủy ghi nhận), khóa tuần tự bằng {@code
 * technical_lock}, và giá trị hiện hành ({@code v_latest_correction}) (000 FR-027 → FR-030). Loại bản ghi là tập loại
 * hiện hành cùng loại cũ ánh xạ về nó (research R-15 mục 5).
 */
@Component
public class CorrectionChainStore {

    private final JdbcTemplate jdbc;
    private final TechnicalLocks locks;

    CorrectionChainStore(JdbcTemplate jdbc, TechnicalLocks locks) {
        this.jdbc = jdbc;
        this.locks = locks;
    }

    /** Bản gốc đã Hủy ghi nhận ở một trong các loại. */
    public boolean isVoided(Collection<String> recordTypes, long originalRecordId) {
        return stateOf(recordTypes, originalRecordId).voided();
    }

    /** Số thứ tự lớn nhất và Hủy ghi nhận, đọc từ các bản đính chính ghi với các loại đã cho. */
    public ChainState stateOf(Collection<String> recordTypes, long originalRecordId) {
        Map<String, Object> chain = jdbc.queryForMap(
                "SELECT IFNULL(MAX(sequence_no), 0) AS n, IFNULL(MAX(type = 'VOID'), 0) AS v FROM correction"
                        + " WHERE record_type IN (" + placeholders(recordTypes.size()) + ") AND original_record_id = ?",
                args(recordTypes, List.of(originalRecordId)));
        return new ChainState(((Number) chain.get("n")).intValue(), ((Number) chain.get("v")).intValue() == 1);
    }

    /** Khóa chuỗi của bản gốc tới hết transaction ({@code technical_lock}) rồi đọc. */
    public ChainState lock(String recordType, Collection<String> recordTypes, long originalRecordId) {
        locks.lock("M15.CORRECTION:" + recordType + ":" + originalRecordId);
        return stateOf(recordTypes, originalRecordId);
    }

    /** Nội dung đúng ({@code JSON}) của bản sửa nội dung mới nhất theo từng bản gốc (FR-030). */
    public Map<Long, String> latestContent(Collection<String> recordTypes, Collection<Long> originalRecordIds) {
        Map<Long, String> latest = new HashMap<>();
        Map<Long, Long> latestId = new HashMap<>();
        jdbc.query(
                "SELECT original_record_id, corrected_content, correction_id FROM v_latest_correction"
                        + where(recordTypes, originalRecordIds),
                rs -> {
                    long id = rs.getLong(1);
                    if (rs.getLong(3) > latestId.getOrDefault(id, 0L)) {
                        latestId.put(id, rs.getLong(3));
                        latest.put(id, rs.getString(2));
                    }
                },
                args(recordTypes, originalRecordIds));
        return latest;
    }

    /** Bản gốc đã có đính chính, kèm cờ Hủy ghi nhận. */
    public Map<Long, Boolean> voided(Collection<String> recordTypes, Collection<Long> originalRecordIds) {
        Map<Long, Boolean> chains = new HashMap<>();
        jdbc.query(
                "SELECT original_record_id, MAX(type = 'VOID') FROM correction" + where(recordTypes, originalRecordIds)
                        + " GROUP BY original_record_id",
                rs -> {
                    chains.put(rs.getLong(1), rs.getBoolean(2));
                },
                args(recordTypes, originalRecordIds));
        return chains;
    }

    private static String where(Collection<String> recordTypes, Collection<Long> originalRecordIds) {
        return " WHERE record_type IN (" + placeholders(recordTypes.size()) + ") AND original_record_id IN ("
                + placeholders(originalRecordIds.size()) + ")";
    }

    private static String placeholders(int n) {
        return String.join(", ", Collections.nCopies(n, "?"));
    }

    private static Object[] args(Collection<?> first, Collection<?> second) {
        List<Object> all = new ArrayList<>(first);
        all.addAll(second);
        return all.toArray();
    }
}
