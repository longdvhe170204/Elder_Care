package com.cambae.backend.m01.internal.retention.service;

import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Một loại bản ghi con của hồ sơ người cao tuổi trong chuỗi lưu giữ (001 T011, T079; data-model mục 11). {@code dataType}
 * trùng loại đối tượng trong nhật ký và loại bản ghi của đính chính, để 000 gắn nhật ký và đính chính của bản ghi vào chuỗi.
 *
 * <p>Bản ghi con KHÔNG tự bị xóa: mọi chuỗi bắt đầu từ bản ghi con đều kéo theo hồ sơ ({@link #referencesTo}) nên chỉ
 * xóa được khi cả hồ sơ, mọi bản ghi con và hồ sơ sau liên kết tới đều hết hạn (FR-051a); việc xóa dòng dữ liệu do
 * {@link ResidentRetention#delete} làm trọn một lượt ({@link #deleteOfResidents}), nên {@link #delete} không làm gì. Nếu
 * bản ghi con tự xóa thì hồ sơ còn được giữ sẽ mất lịch sử.
 */
class ResidentChildRetention implements RetentionProvider {

    /** Hồ sơ ở trạng thái cuối và lúc vào trạng thái hiện tại (dòng lịch sử mới nhất đến đúng trạng thái đó). */
    static final String FINAL_STATUS_AT = "SELECT r.id AS resident_id, (SELECT MAX(h.occurred_at) FROM"
            + " resident_status_history h WHERE h.resident_id = r.id AND h.to_status = r.status) AS final_at"
            + " FROM resident r WHERE r.status IN ('RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED')";

    private final String dataType;
    private final String table;
    private final List<String> subTableDeletes;
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;

    /**
     * @param subTableDeletes câu {@code DELETE} bảng con của bản ghi; {@code %s} là danh sách mã hồ sơ, chạy trước khi
     *     xóa dòng của bản ghi
     */
    ResidentChildRetention(
            String dataType,
            String table,
            List<String> subTableDeletes,
            JdbcTemplate jdbc,
            RetentionDatabase database) {
        this.dataType = dataType;
        this.table = table;
        this.subTableDeletes = subTableDeletes;
        this.jdbc = jdbc;
        this.database = database;
    }

    @Override
    public String dataType() {
        return dataType;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return jdbc
                .query(
                        "SELECT c.id, f.final_at FROM " + table + " c JOIN (" + FINAL_STATUS_AT
                                + ") f ON f.resident_id = c.resident_id WHERE f.final_at < ? ORDER BY f.final_at,"
                                + " c.id LIMIT ?",
                        (rs, i) -> new Candidate(
                                rs.getLong(1),
                                rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                        batch)
                .stream();
    }

    /** Hồ sơ chứa bản ghi: kéo cả hồ sơ và các bản ghi con khác vào chuỗi. */
    @Override
    public Set<DataRef> referencesTo(long id) {
        return jdbc.queryForList("SELECT resident_id FROM " + table + " WHERE id = ?", Long.class, id).stream()
                .map(residentId -> new DataRef(ResidentRetention.TYPE, residentId))
                .collect(Collectors.toSet());
    }

    /** Dòng dữ liệu đã bị xóa cùng hồ sơ ({@link #deleteOfResidents}); không làm gì. */
    @Override
    public void delete(Collection<Long> ids) {}

    /** Mã các bản ghi của một hồ sơ. */
    List<Long> idsOfResident(long residentId) {
        return jdbc.queryForList("SELECT id FROM " + table + " WHERE resident_id = ?", Long.class, residentId);
    }

    /**
     * Xóa mọi bản ghi của các hồ sơ (kết nối {@code eldercare_retention}): bảng con trước, rồi các dòng theo mã tăng dần
     * để bản cũ (trỏ tới bản thay thế mới hơn qua {@code superseded_by_id}) bị xóa trước bản mới.
     */
    void deleteOfResidents(String residentIdsIn) {
        JdbcTemplate retention = database.jdbc();
        for (String statement : subTableDeletes) {
            retention.update(statement.formatted(residentIdsIn));
        }
        retention.update("DELETE FROM " + table + " WHERE resident_id IN " + residentIdsIn + " ORDER BY id");
    }
}
