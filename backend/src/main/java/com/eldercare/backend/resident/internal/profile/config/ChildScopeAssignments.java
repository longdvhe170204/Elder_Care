package com.eldercare.backend.resident.internal.profile.config;

import com.eldercare.backend.resident.internal.persistence.ResidentRecordStore;
import com.eldercare.backend.resident.internal.profile.service.ChildScopeAssignment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Các loại bản ghi con có nhật ký, đính chính hoặc tệp của hồ sơ người cao tuổi (001 T011; mã loại như trong nhật ký). */
@Configuration
class ChildScopeAssignments {

    @Bean
    ChildScopeAssignment statusHistoryScope(ResidentRecordStore records) {
        // Dòng status_history của M01.RESIDENT, thuộc tính STATUS (001 T084)
        return new ChildScopeAssignment(
                "M01.RESIDENT_STATUS_HISTORY",
                "status_history",
                "entity_id",
                " AND entity_type = 'M01.RESIDENT' AND attribute = 'STATUS'",
                records);
    }

    @Bean
    ChildScopeAssignment consentScope(ResidentRecordStore records) {
        return new ChildScopeAssignment("M01.CONSENT", "consent", records);
    }

    @Bean
    ChildScopeAssignment initialHealthRecordScope(ResidentRecordStore records) {
        // Dòng simple_record M01.INITIAL_HEALTH_RECORD (001 T084)
        return new ChildScopeAssignment(
                "M01.INITIAL_HEALTH_RECORD", "simple_record", " AND kind = 'M01.INITIAL_HEALTH_RECORD'", records);
    }

    @Bean
    ChildScopeAssignment healthItemScope(ResidentRecordStore records) {
        return new ChildScopeAssignment("M01.HEALTH_ITEM", "health_item", records);
    }

    @Bean
    ChildScopeAssignment assessmentScope(ResidentRecordStore records) {
        return new ChildScopeAssignment("M01.ASSESSMENT", "assessment", records);
    }

    @Bean
    ChildScopeAssignment endOfLifeWishScope(ResidentRecordStore records) {
        return new ChildScopeAssignment("M01.END_OF_LIFE_WISH", "wish", " AND kind = 'END_OF_LIFE'", records);
    }

    @Bean
    ChildScopeAssignment treatmentWishScope(ResidentRecordStore records) {
        return new ChildScopeAssignment("M01.TREATMENT_WISH", "wish", " AND kind = 'TREATMENT'", records);
    }
}
