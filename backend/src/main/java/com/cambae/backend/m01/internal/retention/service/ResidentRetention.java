package com.cambae.backend.m01.internal.retention.service;

import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ hồ sơ người cao tuổi và các bản ghi con của M01 (001 T011, T079; data-model mục 11; 000 R-11; FR-050, FR-051,
 * FR-051a). Gốc của chuỗi là {@code M01.RESIDENT}; mọi loại tính hạn theo CFG-M15-03 ({@link RetentionKind.ResidentRecord})
 * từ lúc hồ sơ vào trạng thái cuối. Hồ sơ chưa ở trạng thái cuối không bao giờ là ứng viên.
 *
 * <p>Bản ghi trỏ tới hồ sơ ({@link #referencesTo}) là các bản ghi con có loại riêng ({@link ResidentChildRetention}, để
 * nhật ký và đính chính của chúng đi theo chuỗi) và hồ sơ sau liên kết tới (FR-004, FR-051a): hồ sơ cũ chỉ bị xóa khi
 * hồ sơ sau cũng hết hạn. Các bảng không có nhật ký riêng (lịch sử mức chăm sóc, lần báo đánh giá lại, nhắc định kỳ)
 * xóa cùng hồ sơ. Không xóa tệp đính kèm: nội dung ở kho tệp, việc loại bỏ thuộc {@code common.file} (chưa có), nên
 * tệp được giữ lại lâu hơn hạn thay vì bị xóa nhầm.
 */
@Component
class ResidentRetention implements RetentionProvider {

    static final String TYPE = "M01.RESIDENT";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final List<ResidentChildRetention> children;

    ResidentRetention(JdbcTemplate jdbc, RetentionDatabase database, List<ResidentChildRetention> children) {
        this.jdbc = jdbc;
        this.database = database;
        this.children = children;
    }

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return jdbc
                .query(
                        "SELECT f.resident_id, f.final_at FROM (" + ResidentChildRetention.FINAL_STATUS_AT
                                + ") f WHERE f.final_at < ? ORDER BY f.final_at, f.resident_id LIMIT ?",
                        (rs, i) -> new Candidate(
                                rs.getLong(1),
                                rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                        batch)
                .stream();
    }

    /**
     * Bản ghi con (mỗi loại một nhóm) và hồ sơ sau trỏ tới hồ sơ này (FR-051a): hồ sơ cũ chỉ bị xóa khi hồ sơ sau cũng hết hạn.
     */
    @Override
    public Set<DataRef> referencesTo(long id) {
        Set<DataRef> references = new LinkedHashSet<>();
        for (ResidentChildRetention child : children) {
            child.idsOfResident(id).forEach(childId -> references.add(new DataRef(child.dataType(), childId)));
        }
        jdbc.queryForList("SELECT id FROM resident WHERE previous_profile_id = ? ORDER BY id DESC", Long.class, id)
                .forEach(next -> references.add(new DataRef(TYPE, next)));
        return references;
    }

    /**
     * Xóa trọn dữ liệu M01 của các hồ sơ trong một transaction: bảng không có nhật ký riêng, rồi từng loại bản ghi con,
     * sau cùng dòng hồ sơ. Hồ sơ sau (nếu có) đã bị xóa trước vì job xóa theo thứ tự ngược của chuỗi.
     */
    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        JdbcTemplate retention = database.jdbc();
        for (String table : List.of("care_level_history", "reassessment_notice", "periodic_reminder")) {
            retention.update("DELETE FROM " + table + " WHERE resident_id IN " + in);
        }
        children.forEach(child -> child.deleteOfResidents(in));
        retention.update("DELETE FROM resident WHERE id IN " + in);
    }
}
