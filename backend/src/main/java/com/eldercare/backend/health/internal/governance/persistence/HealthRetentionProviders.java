package com.eldercare.backend.health.internal.governance.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lưu giữ dữ liệu cho M06 theo R-11 (data-model 007 mục 14; FR-050, FR-051a): bản ghi chỉ số, ngưỡng cá nhân, lịch đo
 * theo hồ sơ; ngưỡng mặc định không gắn hồ sơ, tính từ lúc hết hiệu lực hoặc hủy. Bản đo lại trỏ tới bản gốc (khóa ngoại)
 * nên cùng chuỗi và xóa trước. Bản ghi khám, kết quả ngủ nằm trên {@code record_event} của hồ sơ, xóa cùng hồ sơ.
 */
@Configuration
public class HealthRetentionProviders {

    public static final String VITAL_SIGN_RECORD = "M06.VITAL_SIGN_RECORD";
    public static final String THRESHOLD = "M06.THRESHOLD";
    public static final String FACILITY_THRESHOLD = "M06.FACILITY_THRESHOLD";
    public static final String MEASUREMENT_SCHEDULE = "M06.MEASUREMENT_SCHEDULE";
    public static final String FACILITY_LICENSE = "M06.FACILITY_LICENSE";

    @Bean
    public RetentionProvider vitalSignRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new HealthTableRetention(
                new HealthRetentionSpec(
                        VITAL_SIGN_RECORD,
                        VITAL_SIGN_RECORD,
                        "vital_sign_record",
                        "TRUE",
                        null,
                        List.of(new HealthRetentionLink(
                                VITAL_SIGN_RECORD, "SELECT id FROM vital_sign_record WHERE remeasurement_for = ?"))),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider thresholdRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new HealthTableRetention(
                new HealthRetentionSpec(
                        THRESHOLD,
                        THRESHOLD,
                        "monitoring_setting",
                        "kind = 'THRESHOLD' AND scope = 'PERSONAL'",
                        null,
                        List.of()),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider facilityThresholdRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new HealthTableRetention(
                new HealthRetentionSpec(
                        FACILITY_THRESHOLD,
                        THRESHOLD,
                        "monitoring_setting",
                        "kind = 'THRESHOLD' AND scope = 'FACILITY' AND status IN ('EXPIRED','CANCELLED')",
                        "COALESCE(effective_until, created_at)",
                        List.of()),
                jdbc,
                database,
                residents);
    }

    @Bean
    public RetentionProvider measurementScheduleRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        return new HealthTableRetention(
                new HealthRetentionSpec(
                        MEASUREMENT_SCHEDULE,
                        MEASUREMENT_SCHEDULE,
                        "monitoring_setting",
                        "kind = 'MEASUREMENT_SCHEDULE'",
                        null,
                        List.of()),
                jdbc,
                database,
                residents);
    }

    /**
     * Giấy phép cơ sở là dòng danh mục ({@code catalog_item} {@code M06.FACILITY_LICENSE}, nhóm 1, không xóa được), nên
     * không có ứng viên loại bỏ.
     */
    @Bean
    public RetentionProvider facilityLicenseRetention() {
        return facilityLicense();
    }

    public static RetentionProvider facilityLicense() {
        return new CatalogRetention(FACILITY_LICENSE, new RetentionKind.NotResidentLinked());
    }
}
