package com.cambae.backend.common.data;

import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.UnmetCondition;
import java.time.LocalDate;
import java.util.List;
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

    public EffectiveVersioning(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
