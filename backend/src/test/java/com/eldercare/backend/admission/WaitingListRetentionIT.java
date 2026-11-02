package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.resident.api.dto.Gender;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T011, T023 (FR-050, FR-051, FR-051a; DBR-23; data-model mục 4, 14; R-15): hồ sơ chờ lưu giữ theo hồ sơ người cao tuổi; lượt
 * đăng ký khai hồ sơ chờ trỏ vào nó để job loại bỏ xóa hồ sơ chờ trước lượt; chỉ job loại bỏ xóa được.
 */
@AutoConfigureMockMvc
@WithMockUser
class WaitingListRetentionIT extends WaitingListTestBase {

    private static final String APPLICATION = "M02.ADMISSION_APPLICATION";
    private static final String ENTRY = "M02.WAITING_LIST_ENTRY";

    @Autowired
    List<RetentionProvider> providers;

    private RetentionProvider provider(String type) {
        return providers.stream()
                .filter(p -> type.equals(p.dataType()))
                .findFirst()
                .orElseThrow();
    }

    private long count(String table, String column, long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
    }

    @Test
    void FR_051_entryIsKeptPerResidentRecord_andApplicationReferencesIt() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, LocalDate.now(), "BASIC");
        long entryId = createEntry(applicationId).get("id").asLong();

        assertThat(provider(ENTRY).retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        assertThat(provider(APPLICATION).referencesTo(applicationId)).containsExactly(new DataRef(ENTRY, entryId));
        assertThat(provider(ENTRY).referencesTo(entryId)).isEmpty();
    }

    @Test
    void FR_051a_retentionRemovesEntryBeforeTheApplication() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, LocalDate.now(), "BASIC");
        JsonNode entry = createEntry(applicationId);
        long entryId = entry.get("id").asLong();
        provider(ENTRY).delete(List.of(entryId));

        assertThat(count("admission_record", "id", entryId)).isZero();
        assertThat(provider(APPLICATION).referencesTo(applicationId)).isEmpty();
        provider(APPLICATION).delete(List.of(applicationId));
        assertThat(count("admission_record", "id", applicationId)).isZero();
    }

    @Test
    void DBR_23_onlyRetentionDeletes() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long entryId = createEntry(application(residentId, LocalDate.now(), "BASIC"))
                .get("id")
                .asLong();

        assertThatThrownBy(() ->
                        jdbc.update("DELETE FROM admission_record WHERE kind = 'WAITING_ENTRY' AND id = ?", entryId))
                .isInstanceOf(DataAccessException.class);
    }
}
