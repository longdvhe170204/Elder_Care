package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T011 (FR-050, FR-051; data-model mục 14; R-15): hợp đồng, lượt vắng, hồ sơ kết thúc, bản ghi qua đời lưu giữ theo hồ sơ người cao
 * tuổi; job loại bỏ xóa được dòng đã khóa, kể cả phiên bản hợp đồng và phụ lục trỏ nhau; dòng chưa hết hạn không là ứng viên.
 */
@AutoConfigureMockMvc
@WithMockUser
class M02RetentionIT extends AbsenceTestBase {

    @Autowired
    List<RetentionProvider> providers;

    private RetentionProvider provider(String type) {
        return providers.stream()
                .filter(p -> type.equals(p.dataType()))
                .findFirst()
                .orElseThrow();
    }

    private int count(String sql, long id) {
        return jdbc.queryForObject(sql, Integer.class, id);
    }

    @Test
    void FR_050_recordsAreKeptPerResidentRecord() {
        for (String type : List.of("M02.CONTRACT", "M02.ABSENCE", "M02.RESIDENCY_ENDING", "M02.DEATH_RECORD")) {
            assertThat(provider(type).retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        }
    }

    @Test
    void FR_051_contractRetention_removesVersionsAndDraftRow_evenWhenLocked() throws Exception {
        long residentId = inResidence();
        long contractId = jdbc.queryForObject(
                "SELECT id FROM contract WHERE resident_id = ? ORDER BY id DESC LIMIT 1", Long.class, residentId);
        assertThat(count("SELECT COUNT(*) FROM contract_version WHERE contract_id = ?", contractId))
                .isPositive();

        provider("M02.CONTRACT").delete(List.of(contractId));

        assertThat(count("SELECT COUNT(*) FROM contract_version WHERE contract_id = ?", contractId))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM contract WHERE id = ?", contractId))
                .isZero();
    }

    @Test
    void FR_051_absenceRetention_removesAChainOfAbsences() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long home = grant(residentId, "HOME_VISIT", at(d0.plusDays(9), 10, 0));
        clockAt(d0.plusDays(1), 10, 0);
        long hospital = transferOut(residentId);

        provider("M02.ABSENCE").delete(List.of(home, hospital));

        assertThat(count("SELECT COUNT(*) FROM absence WHERE resident_id = ?", residentId))
                .isZero();
    }

    @Test
    void FR_050_recordsOfAResidentStillStaying_areNotCandidates() throws Exception {
        long residentId = inResidence();
        long contractId = jdbc.queryForObject(
                "SELECT id FROM contract WHERE resident_id = ? ORDER BY id DESC LIMIT 1", Long.class, residentId);

        assertThat(provider("M02.CONTRACT").expiredCandidates(Instant.now().plusSeconds(86_400), 1000))
                .extracting(RetentionProvider.Candidate::id)
                .doesNotContain(contractId);
    }
}
