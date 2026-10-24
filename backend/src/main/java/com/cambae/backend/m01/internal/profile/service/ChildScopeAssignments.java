package com.cambae.backend.m01.internal.profile.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Các loại bản ghi con có nhật ký, đính chính hoặc tệp của hồ sơ người cao tuổi (001 T011; mã loại như trong nhật ký). */
@Configuration
class ChildScopeAssignments {

    @Bean
    ChildScopeAssignment statusHistoryScope(JdbcTemplate jdbc) {
        return new ChildScopeAssignment("M01.RESIDENT_STATUS_HISTORY", "resident_status_history", jdbc);
    }

    @Bean
    ChildScopeAssignment consentScope(JdbcTemplate jdbc) {
        return new ChildScopeAssignment("M01.CONSENT", "consent", jdbc);
    }

    @Bean
    ChildScopeAssignment initialHealthRecordScope(JdbcTemplate jdbc) {
        return new ChildScopeAssignment("M01.INITIAL_HEALTH_RECORD", "initial_health_record", jdbc);
    }

    @Bean
    ChildScopeAssignment healthItemScope(JdbcTemplate jdbc) {
        return new ChildScopeAssignment("M01.HEALTH_ITEM", "health_item", jdbc);
    }

    @Bean
    ChildScopeAssignment assessmentScope(JdbcTemplate jdbc) {
        return new ChildScopeAssignment("M01.ASSESSMENT", "assessment", jdbc);
    }

    @Bean
    ChildScopeAssignment endOfLifeWishScope(JdbcTemplate jdbc) {
        return new ChildScopeAssignment("M01.END_OF_LIFE_WISH", "end_of_life_wish", jdbc);
    }

    @Bean
    ChildScopeAssignment treatmentWishScope(JdbcTemplate jdbc) {
        return new ChildScopeAssignment("M01.TREATMENT_WISH", "treatment_wish", jdbc);
    }
}
