package com.eldercare.backend.resident.internal.retention.config;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.resident.internal.persistence.ResidentRecordStore;
import com.eldercare.backend.resident.internal.retention.service.ResidentChildRetention;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Các loại bản ghi con của hồ sơ người cao tuổi cho job lưu giữ (001 T011, T079; data-model mục 11). Mã loại trùng loại
 * đối tượng trong nhật ký và loại bản ghi đính chính của từng bảng, để nhật ký và đính chính đi theo bản ghi.
 */
@Configuration
class ResidentRetentionBeans {

    @Bean
    ResidentChildRetention statusHistoryRetention(ResidentRecordStore records, RetentionDatabase database) {
        // Dòng status_history (001 T084): SharedRowRetention xóa cùng hồ sơ (M01.RESIDENT); loại riêng giữ để đính
        // chính
        // của dòng lịch sử đi theo chuỗi (SharedRowRetention.rowsFor chỉ có record_event, simple_record)
        return new ResidentChildRetention(
                "M01.RESIDENT_STATUS_HISTORY",
                "status_history",
                "entity_id",
                " AND entity_type = 'M01.RESIDENT' AND attribute = 'STATUS'",
                List.of(),
                records,
                database);
    }

    @Bean
    ResidentChildRetention consentRetention(ResidentRecordStore records, RetentionDatabase database) {
        return new ResidentChildRetention("M01.CONSENT", "consent", List.of(), records, database);
    }

    @Bean
    ResidentChildRetention initialHealthRecordRetention(ResidentRecordStore records, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.INITIAL_HEALTH_RECORD", "initial_health_record", List.of(), records, database);
    }

    @Bean
    ResidentChildRetention healthItemRetention(ResidentRecordStore records, RetentionDatabase database) {
        return new ResidentChildRetention("M01.HEALTH_ITEM", "health_item", List.of(), records, database);
    }

    @Bean
    ResidentChildRetention assessmentRetention(ResidentRecordStore records, RetentionDatabase database) {
        return new ResidentChildRetention("M01.ASSESSMENT", "assessment", List.of(), records, database);
    }

    @Bean
    ResidentChildRetention endOfLifeWishRetention(ResidentRecordStore records, RetentionDatabase database) {
        // ý kiến khác (record_event) do SharedRowRetention xóa cùng phiếu; người cần báo ở cột JSON
        return new ResidentChildRetention(
                "M01.END_OF_LIFE_WISH", "wish", " AND kind = 'END_OF_LIFE'", List.of(), records, database);
    }

    @Bean
    ResidentChildRetention treatmentWishRetention(ResidentRecordStore records, RetentionDatabase database) {
        return new ResidentChildRetention(
                "M01.TREATMENT_WISH", "wish", " AND kind = 'TREATMENT'", List.of(), records, database);
    }
}
