package com.eldercare.backend.incident.internal.governance.persistence;

import static com.eldercare.backend.incident.internal.governance.persistence.IncidentRetentionTypes.ALERT;
import static com.eldercare.backend.incident.internal.governance.persistence.IncidentRetentionTypes.FACILITY_INCIDENT;
import static com.eldercare.backend.incident.internal.governance.persistence.IncidentRetentionTypes.INCIDENT;
import static com.eldercare.backend.incident.internal.governance.persistence.IncidentRetentionTypes.UNLINKED_ALERT;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lưu giữ dữ liệu cho M05 theo R-11 (data-model 007 mục 14; FR-050, FR-051a): sự cố, cảnh báo gắn hồ sơ theo hồ sơ;
 * sự cố cơ sở vật chất, cảnh báo nút hỗ trợ không gắn người và khoanh vùng không gắn hồ sơ, tính từ lúc kết thúc. Chuỗi:
 * cảnh báo → sự cố chuyển từ nó; sự cố → danh sách tiếp xúc, khoanh vùng gắn nó. Dữ liệu con trên bảng chung (nguồn,
 * diễn biến, lịch sử, vị trí…) xóa cùng bản ghi cha.
 */
@Configuration
public class IncidentRetentionProviders {
    private static final IncidentRetentionLink CONTACT_LIST = new IncidentRetentionLink(
            ContactListRetention.TYPE,
            "SELECT id FROM simple_record WHERE kind = 'M05.CONTACT_LIST' AND entity_type = 'M05.INCIDENT'"
                    + " AND entity_id = ?");
    private static final IncidentRetentionLink CONTAINMENT = new IncidentRetentionLink(
            ContainmentRetention.TYPE,
            "SELECT id FROM simple_record WHERE kind = 'M05.CONTAINMENT'"
                    + " AND JSON_CONTAINS(JSON_EXTRACT(content, '$.incidentIds'), CAST(? AS JSON))");
    private static final IncidentRetentionLink LINKED_INCIDENT = new IncidentRetentionLink(
            INCIDENT, "SELECT id FROM incident WHERE source_alert_id = ? AND resident_id IS NOT NULL");
    private static final IncidentRetentionLink UNLINKED_INCIDENT = new IncidentRetentionLink(
            FACILITY_INCIDENT, "SELECT id FROM incident WHERE source_alert_id = ? AND resident_id IS NULL");

    @Bean
    public RetentionProvider incidentRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new IncidentTableRetention(
                new IncidentRetentionSpec(
                        INCIDENT,
                        INCIDENT,
                        "incident",
                        "resident_id IS NOT NULL",
                        null,
                        List.of(CONTACT_LIST, CONTAINMENT)),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider facilityIncidentRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new IncidentTableRetention(
                new IncidentRetentionSpec(
                        FACILITY_INCIDENT,
                        INCIDENT,
                        "incident",
                        "resident_id IS NULL AND status IN ('CLOSED','CANCELLED')",
                        "COALESCE(closed_at, created_at)",
                        List.of(CONTAINMENT)),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider alertRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new IncidentTableRetention(
                new IncidentRetentionSpec(
                        ALERT, ALERT, "alert", "resident_id IS NOT NULL", null, List.of(LINKED_INCIDENT)),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider unlinkedAlertRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new IncidentTableRetention(
                new IncidentRetentionSpec(
                        UNLINKED_ALERT,
                        ALERT,
                        "alert",
                        "resident_id IS NULL AND status IN ('CLOSED','CONVERTED_TO_INCIDENT')",
                        "COALESCE(closed_at, latest_at)",
                        List.of(LINKED_INCIDENT, UNLINKED_INCIDENT)),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider containmentRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ContainmentRetention(jdbc, database);
    }

    @Bean
    public RetentionProvider contactListRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new ContactListRetention(jdbc, database, residents);
    }
}
