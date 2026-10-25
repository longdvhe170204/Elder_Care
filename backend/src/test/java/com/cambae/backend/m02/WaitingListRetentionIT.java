package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider.DataRef;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T011, T023 (FR-050, FR-051, FR-051a; DBR-23; data-model mục 4, 14): hồ sơ chờ lưu giữ theo hồ sơ người cao tuổi; lượt
 * đăng ký khai hồ sơ chờ trỏ vào nó để job loại bỏ xóa hồ sơ chờ trước lượt; lịch sử điểm và lần liên hệ chỉ thêm, chỉ job
 * loại bỏ xóa được.
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
    void FR_051a_retentionRemovesEntryWithItsHistoryBeforeTheApplication() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, LocalDate.now(), "BASIC");
        JsonNode entry = createEntry(applicationId);
        long entryId = entry.get("id").asLong();
        jdbc.update(
                "INSERT INTO waiting_list_circumstance (entry_id, circumstance_code, status) VALUES (?, 'X',"
                        + " 'CONFIRMED')",
                entryId);
        jdbc.update(
                "INSERT INTO bed_contact (entry_id, bed_id, result, at, performed_by) VALUES (?, 1, 'EXPIRED', NOW(6),"
                        + " ?)",
                entryId,
                office.accountId());
        assertThat(count("score_history", "entry_id", entryId)).isPositive();

        provider(ENTRY).delete(List.of(entryId));

        assertThat(count("waiting_list_entry", "id", entryId)).isZero();
        assertThat(count("score_history", "entry_id", entryId)).isZero();
        assertThat(count("bed_contact", "entry_id", entryId)).isZero();
        assertThat(count("waiting_list_circumstance", "entry_id", entryId)).isZero();
        assertThat(provider(APPLICATION).referencesTo(applicationId)).isEmpty();
        provider(APPLICATION).delete(List.of(applicationId));
        assertThat(count("admission_application", "id", applicationId)).isZero();
    }

    @Test
    void DBR_23_historyAndContactsAreAppendOnly_andOnlyRetentionDeletes() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long entryId = createEntry(application(residentId, LocalDate.now(), "BASIC"))
                .get("id")
                .asLong();
        jdbc.update(
                "INSERT INTO bed_contact (entry_id, bed_id, result, at, performed_by) VALUES (?, 2, 'CONTACTED',"
                        + " NOW(6), ?)",
                entryId,
                office.accountId());

        assertThatThrownBy(() -> jdbc.update("UPDATE score_history SET new_score = 99 WHERE entry_id = ?", entryId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM score_history WHERE entry_id = ?", entryId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE bed_contact SET result = 'EXPIRED' WHERE entry_id = ?", entryId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM bed_contact WHERE entry_id = ?", entryId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM waiting_list_entry WHERE id = ?", entryId))
                .isInstanceOf(DataAccessException.class);
    }
}
