package com.eldercare.backend.billing.internal.purchase.persistence;

import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class PurchaseCostStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public PurchaseCostStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long create(
            long request,
            long resident,
            long statement,
            long period,
            PurchaseDetails data,
            long actor,
            Instant at,
            boolean excess) {
        var values = new LinkedHashMap<String, Object>();
        values.put("resident_id", resident);
        values.put("statement_id", statement);
        values.put("period_id", period);
        values.put("cost_type", "PURCHASE_ASSISTANCE");
        values.put("origin", "PURCHASE_ASSISTANCE");
        values.put("purchase_assistance_id", request);
        values.put("description", data.description());
        values.put("incurred_on", data.purchasedOn());
        values.put("quantity", data.quantity());
        values.put("unit", "mục");
        values.put("amount", data.actualAmount());
        values.put("reason", data.reason());
        values.put(
                "source_values",
                json.writeValueAsString(
                        Map.of("allowedAmount", data.consent().allowedAmount(), "actualAmount", data.actualAmount())));
        values.put("flags", excess ? "EXCEEDS_CONSENT" : "");
        values.put("created_by", actor);
        values.put("created_at", Timestamp.from(at));
        return new SimpleJdbcInsert(jdbc)
                .withTableName("cost_item")
                .usingGeneratedKeyColumns("id")
                .usingColumns(values.keySet().toArray(String[]::new))
                .executeAndReturnKey(values)
                .longValue();
    }
}
