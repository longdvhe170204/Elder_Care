package com.eldercare.backend.admission.internal.transition.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lưu giữ hợp đồng, lượt vắng, hồ sơ kết thúc, bản ghi qua đời (T011; data-model mục 14). Con của mỗi loại xóa cùng cha. */
final class M02RetentionProviders {

    private M02RetentionProviders() {}

    @Component
    static class ContractRetention extends M02RecordRetention {

        ContractRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
            super(jdbc, database, residents);
        }

        @Override
        public String dataType() {
            return "M02.CONTRACT";
        }

        @Override
        String selectByResidents() {
            return "SELECT id, resident_id FROM contract WHERE resident_id IN (%s) ORDER BY id";
        }

        @Override
        List<String> deleteStatements() {
            // Phiên bản nội dung trỏ phụ lục: xóa phiên bản trước
            return List.of(
                    "DELETE FROM contract_version WHERE kind = 'DRAFT' AND contract_id IN %s",
                    "DELETE FROM contract_version WHERE kind = 'CONTENT' AND contract_id IN %s",
                    "DELETE FROM contract_version WHERE kind = 'ADDENDUM' AND contract_id IN %s",
                    "DELETE FROM contract WHERE id IN %s");
        }
    }

    @Component
    static class AbsenceRetention extends M02RecordRetention {

        AbsenceRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
            super(jdbc, database, residents);
        }

        @Override
        public String dataType() {
            return "M02.ABSENCE";
        }

        @Override
        String selectByResidents() {
            return "SELECT id, resident_id FROM absence WHERE kind = 'ABSENCE' AND resident_id IN (%s) ORDER BY id";
        }

        @Override
        List<String> deleteStatements() {
            // Lượt Bệnh viện trỏ lượt Tạm vắng trước (FR-057): xóa từ mới tới cũ
            return List.of("DELETE FROM absence WHERE kind = 'ABSENCE' AND id IN %s ORDER BY id DESC");
        }
    }

    @Component
    static class ResidencyEndingRetention extends M02RecordRetention {

        ResidencyEndingRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
            super(jdbc, database, residents);
        }

        @Override
        public String dataType() {
            return "M02.RESIDENCY_ENDING";
        }

        @Override
        String selectByResidents() {
            return "SELECT id, resident_id FROM residency_ending WHERE kind = 'ENDING' AND resident_id IN (%s) ORDER BY id";
        }

        @Override
        List<String> deleteStatements() {
            return List.of("DELETE FROM residency_ending WHERE kind = 'ENDING' AND id IN %s");
        }
    }

    @Component
    static class DeathRecordRetention extends M02RecordRetention {

        DeathRecordRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
            super(jdbc, database, residents);
        }

        @Override
        public String dataType() {
            return "M02.DEATH_RECORD";
        }

        @Override
        String selectByResidents() {
            return "SELECT id, resident_id FROM residency_ending WHERE kind = 'DEATH' AND resident_id IN (%s) ORDER BY id";
        }

        @Override
        List<String> deleteStatements() {
            return List.of("DELETE FROM residency_ending WHERE kind = 'DEATH' AND id IN %s");
        }
    }
}
