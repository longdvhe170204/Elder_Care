package com.cambae.backend.m01.internal.retention.service;

import com.cambae.backend.m15.retention.api.RetentionDatabase;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Các loại bản ghi con của hồ sơ người cao tuổi cho job lưu giữ (001 T011, T079; data-model mục 11). Mã loại trùng loại
 * đối tượng trong nhật ký và loại bản ghi đính chính của từng bảng, để nhật ký và đính chính đi theo bản ghi.
 */
@Configuration
class ResidentRetentionBeans {

    @Bean
    ResidentChildRetention statusHistoryRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.RESIDENT_STATUS_HISTORY", "resident_status_history", List.of(), jdbc, database);
    }

    @Bean
    ResidentChildRetention consentRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.CONSENT",
                "consent",
                List.of(
                        "DELETE FROM consent_family_member WHERE consent_id IN (SELECT id FROM consent WHERE resident_id IN %s)"),
                jdbc,
                database);
    }

    @Bean
    ResidentChildRetention initialHealthRecordRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.INITIAL_HEALTH_RECORD", "initial_health_record", List.of(), jdbc, database);
    }

    @Bean
    ResidentChildRetention healthItemRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ResidentChildRetention("M01.HEALTH_ITEM", "health_item", List.of(), jdbc, database);
    }

    @Bean
    ResidentChildRetention assessmentRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.ASSESSMENT",
                "assessment",
                List.of(
                        "DELETE FROM assessment_care_need WHERE assessment_id IN (SELECT id FROM assessment WHERE resident_id IN %s)"),
                jdbc,
                database);
    }

    @Bean
    ResidentChildRetention endOfLifeWishRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.END_OF_LIFE_WISH",
                "end_of_life_wish",
                List.of(
                        "DELETE FROM end_of_life_wish_opinion WHERE wish_id IN (SELECT id FROM end_of_life_wish WHERE resident_id IN %s)"),
                jdbc,
                database);
    }

    @Bean
    ResidentChildRetention treatmentWishRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.TREATMENT_WISH",
                "treatment_wish",
                List.of(
                        "DELETE FROM treatment_wish_notify WHERE treatment_wish_id IN (SELECT id FROM treatment_wish WHERE resident_id IN %s)",
                        "DELETE FROM treatment_wish_opinion WHERE treatment_wish_id IN (SELECT id FROM treatment_wish WHERE resident_id IN %s)"),
                jdbc,
                database);
    }
}
