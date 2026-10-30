package com.eldercare.backend.administration.shared.internal.persistence;

import static com.eldercare.backend.administration.shared.internal.persistence.Rows.instant;
import static com.eldercare.backend.administration.shared.internal.persistence.Rows.utc;

import com.eldercare.backend.administration.shared.internal.domain.CatalogRow;
import com.eldercare.backend.common.data.CatalogStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Đọc, ghi {@code catalog_item} (data-model 000 mục 8). */
@Component
public class CatalogItemStore {

    private static final String COLUMNS = "SELECT id, code, name, parent_id, sort_order, status, attributes,"
            + " confirmed_by, confirmed_at, version FROM catalog_item";

    private final JdbcTemplate jdbc;

    public CatalogItemStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(
            String type, String module, String code, String name, Long parentId, int sortOrder, String attributes) {
        jdbc.update(
                "INSERT INTO catalog_item (catalog_type, module, code, name, parent_id, sort_order, status, attributes,"
                        + " version) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, 0)",
                type,
                module,
                code,
                name,
                parentId,
                sortOrder,
                attributes);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public Optional<CatalogRow> find(String type, long id) {
        return jdbc.query(COLUMNS + " WHERE id = ? AND catalog_type = ?", (rs, i) -> row(rs), id, type).stream()
                .findFirst();
    }

    public Optional<CatalogRow> findByCode(String type, Long parentId, String code) {
        return jdbc
                .query(
                        COLUMNS + " WHERE catalog_type = ? AND parent_id <=> ? AND code = ?",
                        (rs, i) -> row(rs),
                        type,
                        parentId,
                        code)
                .stream()
                .findFirst();
    }

    /** Mục đang Hiệu lực trùng tên trong cùng loại, cùng cha, khác {@code exceptId} (BR-M15-06). */
    public Optional<CatalogRow> findActiveByName(String type, Long parentId, String name, long exceptId) {
        return jdbc
                .query(
                        COLUMNS + " WHERE catalog_type = ? AND parent_id <=> ? AND name = ? AND status = 'ACTIVE'"
                                + " AND id <> ?",
                        (rs, i) -> row(rs),
                        type,
                        parentId,
                        name,
                        exceptId)
                .stream()
                .findFirst();
    }

    public List<CatalogRow> list(String type, Long parentId, boolean activeOnly) {
        return jdbc.query(
                COLUMNS + " WHERE catalog_type = ? AND (? IS NULL OR parent_id = ?)"
                        + (activeOnly ? " AND status = 'ACTIVE'" : "") + " ORDER BY sort_order, name, id",
                (rs, i) -> row(rs),
                type,
                parentId,
                parentId);
    }

    /** Ghi các cột có thể đổi khi {@code version} còn đúng; trả {@code false} khi phiên bản đã khác. */
    public boolean update(
            String type,
            long id,
            int version,
            String name,
            int sortOrder,
            CatalogStatus status,
            String attributes,
            Long confirmedBy,
            Instant confirmedAt) {
        return jdbc.update(
                        "UPDATE catalog_item SET name = ?, sort_order = ?, status = ?, attributes = ?, confirmed_by = ?,"
                                + " confirmed_at = ?, version = version + 1 WHERE id = ? AND catalog_type = ?"
                                + " AND version = ?",
                        name,
                        sortOrder,
                        status.name(),
                        attributes,
                        confirmedBy,
                        utc(confirmedAt),
                        id,
                        type,
                        version)
                == 1;
    }

    public void delete(String type, long id) {
        jdbc.update("DELETE FROM catalog_item WHERE id = ? AND catalog_type = ?", id, type);
    }

    private static CatalogRow row(ResultSet rs) throws SQLException {
        return new CatalogRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getObject("parent_id", Long.class),
                rs.getInt("sort_order"),
                CatalogStatus.valueOf(rs.getString("status")),
                rs.getString("attributes"),
                rs.getObject("confirmed_by", Long.class),
                instant(rs, "confirmed_at"),
                rs.getInt("version"));
    }
}
