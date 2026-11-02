package com.eldercare.backend.common.data;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thêm phiên bản có hiệu lực không chồng khoảng (data-model mục 1.2; FR-004, FR-008, BR-M15-07). MySQL không có ràng
 * buộc loại trừ khoảng, nên khóa dòng cha bằng {@code SELECT … FOR UPDATE}, kiểm không có phiên bản giao khoảng, rồi
 * mới thêm. Khoảng là ngày lịch, gồm cả hai đầu; {@code to = null} là còn mở. Khóa dòng cha cần quyền UPDATE (hoặc
 * DELETE) của tài khoản ứng dụng trên bảng cha.
 *
 * <p>Đổi thuộc tính theo phiên bản (FR-004; chủ dự án chốt 2026-10-19): {@link #addVersionFrom} đóng phiên bản đang mở
 * ở ngày trước ngày hiệu lực mới rồi thêm phiên bản mới còn mở. Chỉ đóng khi ngày hiệu lực mới sau hôm nay, nên phần đã
 * áp dụng không đổi; tài khoản ứng dụng chỉ có {@code UPDATE} trên cột {@code effective_to} của bảng phiên bản.
 */
@Component
public class EffectiveVersioning {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    /** Bảng đối tượng, bảng phiên bản (có {@code effective_from}, {@code effective_to}) và cột trỏ về đối tượng. */
    public record VersionedTable(String parentTable, String versionTable, String parentColumn) {

        public VersionedTable {
            for (String name : List.of(parentTable, versionTable, parentColumn)) {
                if (!IDENTIFIER.matcher(name).matches()) {
                    throw new IllegalArgumentException("Tên bảng, cột không hợp lệ: " + name);
                }
            }
        }
    }

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    public EffectiveVersioning(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    /**
     * Phiên bản mới còn mở từ {@code from}: đóng phiên bản đang mở (nếu có, và bắt đầu trước {@code from}) ở ngày
     * {@code from - 1}, rồi thêm như {@link #addVersion}. {@code from} không sau hôm nay thì 409 (FR-004).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void addVersionFrom(VersionedTable table, long parentId, LocalDate from, Runnable insert) {
        if (!from.isAfter(time.today())) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "FR-004", "Phiên bản mới phải có hiệu lực sau hôm nay; phiên bản đã áp dụng không sửa")));
        }
        jdbc.queryForList("SELECT id FROM " + table.parentTable() + " WHERE id = ? FOR UPDATE", parentId);
        List<Map<String, Object>> open = jdbc.queryForList(
                "SELECT id FROM " + table.versionTable() + " WHERE " + table.parentColumn() + " = ?"
                        + " AND effective_to IS NULL AND effective_from < ?",
                parentId,
                from);
        for (Map<String, Object> version : open) {
            jdbc.update(
                    "UPDATE " + table.versionTable() + " SET effective_to = ? WHERE id = ?",
                    from.minusDays(1),
                    version.get("id"));
        }
        addVersion(table, parentId, from, null, insert);
    }

    /** Chạy {@code insert} khi khoảng [from, to] không giao phiên bản nào của đối tượng; giao thì 409 (FR-008). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void addVersion(VersionedTable table, long parentId, LocalDate from, LocalDate to, Runnable insert) {
        if (to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Ngày kết thúc trước ngày bắt đầu");
        }
        jdbc.queryForList("SELECT id FROM " + table.parentTable() + " WHERE id = ? FOR UPDATE", parentId);
        Integer overlapping = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table.versionTable() + " WHERE " + table.parentColumn() + " = ?"
                        + " AND effective_from <= COALESCE(?, '9999-12-31')"
                        + " AND (effective_to IS NULL OR effective_to >= ?)",
                Integer.class,
                parentId,
                to,
                from);
        if (overlapping != null && overlapping > 0) {
            throw new ConditionNotMetException(List.of(
                    new UnmetCondition("BR-M15-07", "Khoảng hiệu lực chồng lên phiên bản khác của cùng đối tượng")));
        }
        insert.run();
    }
}
