package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FR-050, FR-051, FR-051a (006 F-06, data-model mục 13): provider thật và MySQL thật. Chuỗi đơn thuốc → liều → kết quả
 * liều, đơn thay thế, lần giao → nhận lại phải xóa được bằng tài khoản {@code eldercare_retention} theo thứ tự ngược
 * (bản trỏ tới trước), tài khoản ứng dụng vẫn không xóa được; hồ sơ trỏ tới đủ để hồ sơ không bị xóa trước.
 */
@Import(FakeM01Config.class)
class FR_051a_MedicationRetentionIT extends MySqlIntegrationTestBase {

    private static final Instant OLD = Instant.parse("1940-01-01T00:00:00Z");
    private static final Instant CUTOFF = Instant.parse("1970-01-01T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RetentionDatabase database;

    @Autowired
    FakeResidents residents;

    @Autowired
    List<RetentionProvider> providers;

    private RetentionProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Chưa có provider " + type));
    }

    private long endedResident() {
        long id = TestUsers.nextId();
        residents.resident(id, "[Hồ sơ kiểm lưu giữ thuốc]", null, ResidentStatus.RESIDENCY_ENDED);
        residents.changeStatus(id, ResidentStatus.RESIDENCY_ENDED, OLD);
        return id;
    }

    private long medicationItem() {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id, catalog_type, module, code, name, status) VALUES (?, 'MEDICATION', 'M07', ?, ?, 'ACTIVE')",
                id,
                "RET" + id,
                "[Thuốc kiểm lưu giữ " + id + "]");
        return id;
    }

    private long prescription(long resident, long medication, Long replaces) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO prescription(id, resident_id, medication_id, active_ingredient_ids, dosage, dosage_unit, route,"
                        + " prescription_type, frequency_type, started_at, medication_source, prescriber_type, created_at, replaces_id)"
                        + " VALUES (?,?,?,'[]',1,'viên','uống','PERIODIC','EVERY_N_HOURS',?,'FACILITY','INTERNAL',?,?)",
                id,
                resident,
                medication,
                java.sql.Timestamp.from(OLD),
                java.sql.Timestamp.from(OLD),
                replaces);
        return id;
    }

    private long dose(long prescription, long resident, long medication) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO medication_dose(id, prescription_id, resident_id, medication_id, scheduled_time, window_from,"
                        + " window_to, dosage, dosage_unit, medication_source)"
                        + " VALUES (?,?,?,?,?,?,?,1,'viên','FACILITY')",
                id,
                prescription,
                resident,
                medication,
                java.sql.Timestamp.from(OLD),
                java.sql.Timestamp.from(OLD),
                java.sql.Timestamp.from(OLD.plusSeconds(3600)));
        return id;
    }

    private long result(long dose, long resident) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO dose_result(id, kind, dose_id, resident_id, result_type, authority_basis)"
                        + " VALUES (?,'SCHEDULED',?,?,'MISSED','ASSIGNED')",
                id,
                dose,
                resident);
        return id;
    }

    private long handover(long resident, String kind, Long original) {
        long id = TestUsers.nextId();
        var at = java.sql.Timestamp.from(OLD);
        if (original == null) {
            jdbc.update(
                    "INSERT INTO medication_handover(id, kind, resident_id, recipient_type, dispensed_by_id, handover_time,"
                            + " status, locked_at) VALUES (?,'HANDOVER',?,'STAFF',1,?,'RETURNED',?)",
                    id,
                    resident,
                    at,
                    at);
        } else {
            jdbc.update(
                    "INSERT INTO medication_handover(id, kind, resident_id, handover_id, recorded_by_id, return_time,"
                            + " locked_at) VALUES (?,'RETURN',?,?,1,?,?)",
                    id,
                    resident,
                    original,
                    at,
                    at);
        }
        return id;
    }

    @Test
    void FR_051a_chainIsFoundFromTheResidentAndDeletedBottomUpByRetentionAccount() {
        long resident = endedResident();
        long medication = medicationItem();
        long old = prescription(resident, medication, null);
        long replacement = prescription(resident, medication, old);
        long dose = dose(old, resident, medication);
        long result = result(dose, resident);
        long given = handover(resident, "HANDOVER", null);
        long returned = handover(resident, "RETURN", given);

        assertThat(provider("M07.PRESCRIPTION")
                        .expiredCandidates(CUTOFF, null, 1000)
                        .map(Candidate::id))
                .contains(old, replacement);
        assertThat(provider("M07.PRESCRIPTION").referencesTo(old))
                .contains(new DataRef("M07.PRESCRIPTION", replacement), new DataRef("M07.MEDICATION_DOSE", dose));
        assertThat(provider("M07.MEDICATION_DOSE").referencesTo(dose))
                .containsExactly(new DataRef("M07.DOSE_RESULT", result));
        assertThat(provider("M07.MEDICATION_HANDOVER").referencesTo(given))
                .containsExactly(new DataRef("M07.MEDICATION_HANDOVER", returned));
        assertThat(providers.stream()
                        .flatMap(p -> p.referencing(new DataRef(SharedRowRetention.RESIDENT, resident)).stream())
                        .toList())
                .contains(
                        new DataRef("M07.PRESCRIPTION", old),
                        new DataRef("M07.MEDICATION_DOSE", dose),
                        new DataRef("M07.DOSE_RESULT", result),
                        new DataRef("M07.MEDICATION_HANDOVER", given));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM dose_result WHERE id = ?", result))
                .isInstanceOf(DataAccessException.class);

        database.transaction().executeWithoutResult(tx -> {
            provider("M07.MEDICATION_HANDOVER").delete(Set.of(returned));
            provider("M07.MEDICATION_HANDOVER").delete(Set.of(given));
            provider("M07.DOSE_RESULT").delete(Set.of(result));
            provider("M07.MEDICATION_DOSE").delete(Set.of(dose));
            provider("M07.PRESCRIPTION").delete(Set.of(replacement));
            provider("M07.PRESCRIPTION").delete(Set.of(old));
        });

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM prescription WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM medication_handover WHERE resident_id = ?", Integer.class, resident))
                .isZero();
    }
}
