package com.eldercare.backend.billing.internal.item.persistence;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.common.error.NotFoundException;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** FR-019: không đọc tên thuốc từ nguồn vào hình chiếu chi phí. */
@Repository
public class CostViewStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CostViewStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    private static final String SELECT = """
            SELECT i.*,p.period_month AS original_month FROM cost_item i
            LEFT JOIN cost_statement p ON p.id=i.original_period_id
            """;

    public CostItemView item(long id) {
        return jdbc.query(SELECT + " WHERE i.id=?", this::map, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<CostItemView> ofStatement(long id) {
        return jdbc.query(SELECT + " WHERE i.statement_id=? ORDER BY i.id", this::map, id);
    }

    public List<CostTypeTotal> savedTotals(long statement) {
        String raw =
                jdbc.queryForObject("SELECT totals_by_type FROM cost_statement WHERE id=?", String.class, statement);
        return raw == null ? List.of() : json.readValue(raw, new TypeReference<List<CostTypeTotal>>() {});
    }

    private CostItemView map(ResultSet r, int n) throws SQLException {
        var type = CostType.valueOf(r.getString("cost_type"));
        String sourceType = r.getString("source_type");
        String description = type == CostType.MEDICATION ? "Thuốc" : r.getString("description");
        if (r.getString("origin").equals("PURCHASE_ASSISTANCE")) description = "Mua hộ";
        var date = r.getObject("incurred_on", LocalDate.class);
        var timestamp = r.getTimestamp("occurred_at");
        var flags = r.getString("flags").isBlank()
                ? List.<String>of()
                : List.of(r.getString("flags").split(","));
        var factor = r.getBigDecimal("factor_percent");
        var source = sourceType == null
                ? null
                : new BillingSourceView(
                        sourceType,
                        r.getString("source_id"),
                        r.getString("unit"),
                        date,
                        timestamp == null ? null : timestamp.toInstant(),
                        "Bản ghi tính phí",
                        r.getBigDecimal("quantity"),
                        factor,
                        flags,
                        r.getLong("id"),
                        r.getString("billing_unit"));
        String basis = r.getString("unit_price_basis");
        Map<String, Object> basisValue =
                basis == null ? Map.of() : json.readValue(basis, new TypeReference<Map<String, Object>>() {});
        long root =
                r.getObject("original_item_id", Long.class) == null ? r.getLong("id") : r.getLong("original_item_id");
        Long total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM cost_item WHERE (id=? OR original_item_id=?)"
                        + " AND status<>'CANCELLED'",
                Long.class,
                root,
                root);
        var adjustments = jdbc.query(
                """
                SELECT id,amount,status,original_period_id FROM cost_item WHERE original_item_id=? ORDER BY id
                """,
                (row, number) -> {
                    var adjustment = new LinkedHashMap<String, Object>();
                    adjustment.put("id", row.getLong("id"));
                    adjustment.put("amount", row.getObject("amount", Long.class));
                    adjustment.put("status", row.getString("status"));
                    return adjustment;
                },
                r.getLong("id"));
        return new CostItemView(
                r.getLong("id"),
                type,
                r.getString("origin"),
                source,
                description,
                r.getString("item_code"),
                null,
                date,
                r.getBigDecimal("quantity"),
                r.getString("unit"),
                r.getObject("unit_price", Long.class),
                basisValue,
                factor,
                r.getObject("amount", Long.class),
                flags,
                r.getString("status"),
                r.getObject("original_item_id", Long.class),
                r.getString("original_month"),
                List.of(),
                List.of(),
                new ArrayList<>(adjustments),
                total,
                List.of("medicationName"));
    }
}
