package com.eldercare.backend.resident.internal.assessment.persistence;

import com.eldercare.backend.resident.internal.assessment.domain.FamilyDeclaredEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phần "Gia đình khai" của lần đánh giá đầu vào (001 T112; Q-412, FR-025a): cột JSON {@code assessment.family_declared}
 * không thuộc entity {@code Assessment}, ghi bằng câu lệnh riêng để không đổi {@code version} của bản Nháp (lệnh sửa nháp
 * của Bác sĩ không nhận 412 vì Hành chính ghi). Chỉ ghi được khi bản ghi còn Nháp và là đánh giá đầu vào.
 */
@Repository
public class FamilyDeclaredStore {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final JsonMapper json;

    FamilyDeclaredStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.json = json;
    }

    /** Mục "Gia đình khai" theo id lần đánh giá; lần không có mục nào không có trong kết quả. */
    public Map<Long, List<FamilyDeclaredEntry>> read(Collection<Long> assessmentIds) {
        Map<Long, List<FamilyDeclaredEntry>> result = new HashMap<>();
        if (assessmentIds.isEmpty()) {
            return result;
        }
        named.query(
                "SELECT id, family_declared FROM assessment WHERE id IN (:ids) AND family_declared IS NOT NULL",
                new MapSqlParameterSource("ids", assessmentIds),
                rs -> {
                    result.put(
                            rs.getLong("id"),
                            Arrays.asList(
                                    json.readValue(rs.getString("family_declared"), FamilyDeclaredEntry[].class)));
                });
        return result;
    }

    /** Thay toàn bộ phần "Gia đình khai" của bản Nháp đầu vào; trả số dòng đổi. */
    public int write(long assessmentId, List<FamilyDeclaredEntry> entries) {
        return jdbc.update(
                "UPDATE assessment SET family_declared = CAST(? AS JSON)"
                        + " WHERE id = ? AND status = 'DRAFT' AND type = 'INITIAL'",
                json.writeValueAsString(entries),
                assessmentId);
    }
}
