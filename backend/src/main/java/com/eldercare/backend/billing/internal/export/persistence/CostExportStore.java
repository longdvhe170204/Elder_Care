package com.eldercare.backend.billing.internal.export.persistence;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CostExportStore {
    private final JdbcTemplate jdbc;

    public CostExportStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long firstExport(long id) {
        return jdbc.queryForObject("SELECT first_export_id FROM cost_statement WHERE id=?", Long.class, id);
    }

    public void mark(long id, long export) {
        jdbc.update("UPDATE cost_statement SET first_export_id=? WHERE id=? AND first_export_id IS NULL", export, id);
    }

    public String adjustmentType(long id) {
        return jdbc.queryForObject("SELECT adjustment_type FROM cost_item WHERE id=?", String.class, id);
    }
}
