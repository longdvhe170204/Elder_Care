package com.eldercare.backend.staffing.internal.persistence;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.common.time.BusinessTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** T050: tạo các provider lưu giữ từ cùng kết nối; cấu hình không phụ thuộc JDBC. */
@Component
public class M09RetentionFactory {
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final BusinessTime time;
    private final AccountApi accounts;
    private final ProfessionalActivityApi activities;

    private final CatalogApi catalogs;
    private final StatusHistoryApi histories;
    private final RecordEventApi events;
    private final SimpleRecordApi records;

    public M09RetentionFactory(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities,
            CatalogApi catalogs,
            StatusHistoryApi histories,
            RecordEventApi events,
            SimpleRecordApi records) {
        this.jdbc = jdbc;
        this.database = database;
        this.time = time;
        this.accounts = accounts;
        this.activities = activities;
        this.catalogs = catalogs;
        this.histories = histories;
        this.events = events;
        this.records = records;
    }

    public M09Retention create(M09Retention.Kind kind) {
        return new M09Retention(kind, jdbc, database, time, accounts, activities, catalogs, histories, events, records);
    }
}
