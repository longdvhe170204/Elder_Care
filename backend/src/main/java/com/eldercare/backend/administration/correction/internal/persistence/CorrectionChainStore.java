package com.eldercare.backend.administration.correction.internal.persistence;

import com.eldercare.backend.administration.correction.internal.domain.ChainState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Chuỗi đính chính của từng bản gốc ({@code correction_chain}, kỹ thuật) và giá trị hiện hành ({@code
 * v_latest_correction}) (000 FR-027 → FR-030). Loại bản ghi là tập loại hiện hành cùng loại cũ ánh xạ về nó (research
 * R-15 mục 5).
 */
@Component
public class CorrectionChainStore {

    private final JdbcTemplate jdbc;

    CorrectionChainStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Chuỗi của bản gốc đã Hủy ghi nhận ở một trong các loại. */
    public boolean isVoided(Collection<String> recordTypes, long originalRecordId) {
        return Boolean.TRUE.equals(jdbc.query(
                "SELECT MAX(voided) FROM correction_chain WHERE record_type IN (" + placeholders(recordTypes.size())
                        + ") AND original_record_id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : null,
                args(recordTypes, List.of(originalRecordId))));
    }

    /** Tạo chuỗi nếu chưa có; {@code true} khi vừa tạo. */
    public boolean create(String recordType, long originalRecordId) {
        return jdbc.update(
                        "INSERT IGNORE INTO correction_chain (record_type, original_record_id) VALUES (?, ?)",
                        recordType,
                        originalRecordId)
                == 1;
    }

    /** Số bản lớn nhất và Hủy ghi nhận của chuỗi ghi với các loại (cũ) đã cho. */
    public ChainState stateOf(Collection<String> recordTypes, long originalRecordId) {
        Map<String, Object> old = jdbc.queryForMap(
                "SELECT IFNULL(MAX(correction_count), 0) AS n, IFNULL(MAX(voided), FALSE) AS v FROM correction_chain"
                        + " WHERE record_type IN (" + placeholders(recordTypes.size()) + ") AND original_record_id = ?",
                args(recordTypes, List.of(originalRecordId)));
        return new ChainState(((Number) old.get("n")).intValue(), ((Number) old.get("v")).intValue() == 1);
    }

    /** Khóa chuỗi tới hết transaction rồi đọc. */
    public ChainState lock(String recordType, long originalRecordId) {
        Map<String, Object> chain = jdbc.queryForMap(
                "SELECT correction_count, voided FROM correction_chain"
                        + " WHERE record_type = ? AND original_record_id = ? FOR UPDATE",
                recordType,
                originalRecordId);
        return new ChainState(
                ((Number) chain.get("correction_count")).intValue(), Boolean.TRUE.equals(chain.get("voided")));
    }

    public void save(String recordType, long originalRecordId, ChainState state) {
        jdbc.update(
                "UPDATE correction_chain SET correction_count = ?, voided = ? WHERE record_type = ?"
                        + " AND original_record_id = ?",
                state.correctionCount(),
                state.voided(),
                recordType,
                originalRecordId);
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

    /** Bản gốc đã có chuỗi, kèm cờ Hủy ghi nhận. */
    public Map<Long, Boolean> voided(Collection<String> recordTypes, Collection<Long> originalRecordIds) {
        Map<Long, Boolean> chains = new HashMap<>();
        jdbc.query(
                "SELECT original_record_id, voided FROM correction_chain" + where(recordTypes, originalRecordIds),
                rs -> {
                    chains.merge(rs.getLong(1), rs.getBoolean(2), Boolean::logicalOr);
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
