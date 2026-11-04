package com.eldercare.backend.resident.internal.retention.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.resident.internal.persistence.ResidentRecordStore;
import java.time.Instant;
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
public class ResidentChildRetention implements RetentionProvider {

    private final String dataType;
    private final String table;
    private final String residentColumn;
    private final String condition;
    private final List<String> subTableDeletes;
    private final ResidentRecordStore records;
    private final RetentionDatabase database;

    /**
     * @param subTableDeletes câu {@code DELETE} bảng con của bản ghi; {@code %s} là danh sách mã hồ sơ, chạy trước khi
     *     xóa dòng của bản ghi
     */
    public ResidentChildRetention(
            String dataType,
            String table,
            List<String> subTableDeletes,
            ResidentRecordStore records,
            RetentionDatabase database) {
        this(dataType, table, "", subTableDeletes, records, database);
    }

    /** {@code condition}: điều kiện thêm cho bảng gộp nhiều loại (ví dụ {@code " AND kind = 'TREATMENT'"}). */
    public ResidentChildRetention(
            String dataType,
            String table,
            String condition,
            List<String> subTableDeletes,
            ResidentRecordStore records,
            RetentionDatabase database) {
        this(dataType, table, "resident_id", condition, subTableDeletes, records, database);
    }

    /** {@code residentColumn}: cột mã hồ sơ khi không phải {@code resident_id} (bảng dùng chung {@code status_history}). */
    public ResidentChildRetention(
            String dataType,
            String table,
            String residentColumn,
            String condition,
            List<String> subTableDeletes,
            ResidentRecordStore records,
            RetentionDatabase database) {
        this.dataType = dataType;
        this.table = table;
        this.residentColumn = residentColumn;
        this.condition = condition;
        this.subTableDeletes = subTableDeletes;
        this.records = records;
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
        return records.expiredCandidates(table, residentColumn, condition, cutoff, batch).stream();
    }

    /** Hồ sơ chứa bản ghi: kéo cả hồ sơ và các bản ghi con khác vào chuỗi. */
    @Override
    public Set<DataRef> referencesTo(long id) {
        return records.residentIdOf(table, residentColumn, condition, id).stream()
                .map(residentId -> new DataRef(ResidentRetention.TYPE, residentId))
                .collect(Collectors.toSet());
    }

    /** Dòng dữ liệu đã bị xóa cùng hồ sơ ({@link #deleteOfResidents}); không làm gì. */
    @Override
    public void delete(Collection<Long> ids) {}

    /** Mã các bản ghi của một hồ sơ. */
    List<Long> idsOfResident(long residentId) {
        return records.idsOfResident(table, residentColumn, condition, residentId);
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
        retention.update("DELETE FROM " + table + " WHERE " + residentColumn + " IN " + residentIdsIn + condition
                + " ORDER BY id");
    }
}
