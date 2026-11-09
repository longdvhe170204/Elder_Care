package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FR-050, FR-051, FR-051a (data-model 007 mục 14; 000 T112): provider lưu giữ thật của M05, M06 trên MySQL thật. Bản
 * ghi gắn hồ sơ hết hạn theo lúc người cao tuổi sang trạng thái cuối, bản ghi không gắn hồ sơ theo lúc kết thúc; chuỗi
 * cảnh báo → sự cố → danh sách tiếp xúc, khoanh vùng và đo lại → bản gốc nằm trong {@code referencesTo}; hồ sơ (kể cả
 * người tiếp xúc) có {@code referencing} để không bị xóa trước; tài khoản ứng dụng không xóa được, tài khoản
 * {@code eldercare_retention} xóa được.
 */
@Import(FakeM01Config.class)
class FR_050_IncidentHealthRetentionIT extends MySqlIntegrationTestBase {
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

    private List<Long> candidates(String type) {
        return provider(type)
                .expiredCandidates(CUTOFF, null, 100_000)
                .map(Candidate::id)
                .toList();
    }

    private long resident(ResidentStatus status) {
        long id = TestUsers.nextId();
        residents.resident(id, "[Hồ sơ kiểm lưu giữ 007]", null, status);
        if (status == ResidentStatus.RESIDENCY_ENDED) residents.changeStatus(id, status, OLD);
        return id;
    }

    private long catalogId(String type, String code) {
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type = ? AND code = ?", Long.class, type, code);
    }

    private long vitalSign(long resident, Long remeasurementFor) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO vital_sign_record (id, resident_id, indicator_id, measured_at, measured_by, level,"
                        + " confirmation_status, remeasurement_for, components, labels, confirmed_at, locked_at, created_at,"
                        + " created_by) VALUES (?,?,?,?,1,'NORMAL','CONFIRMED',?,'{\"VALUE\":{\"value\":70,\"level\":\"NORMAL\"}}','',?,?,?,1)",
                id,
                resident,
                catalogId("M06.VITAL_SIGN", "HEART_RATE"),
                Timestamp.from(OLD),
                remeasurementFor,
                Timestamp.from(OLD),
                Timestamp.from(OLD),
                Timestamp.from(OLD));
        return id;
    }

    private long setting(String kind, String scope, Long resident, String status, Instant until) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO monitoring_setting (id, kind, scope, resident_id, indicator_id, effective_from,"
                        + " effective_until, status, components, created_by, created_at)"
                        + " VALUES (?,?,?,?,?,?,?,?,'{\"VALUE\":{\"min\":1}}',1,?)",
                id,
                kind,
                scope,
                resident,
                catalogId("M06.VITAL_SIGN", "HEART_RATE"),
                Timestamp.from(OLD.minusSeconds(3600)),
                until == null ? null : Timestamp.from(until),
                status,
                Timestamp.from(OLD.minusSeconds(3600)));
        return id;
    }

    private long alert(Long resident, String type, String status) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO alert (id, resident_id, type, type_lock, level, first_at, latest_at, status, closed_at)"
                        + " VALUES (?,?,?,?,'MEDIUM',?,?,?,?)",
                id,
                resident,
                type,
                "RET:" + id,
                Timestamp.from(OLD),
                Timestamp.from(OLD),
                status,
                "CLOSED".equals(status) || "CONVERTED_TO_INCIDENT".equals(status) ? Timestamp.from(OLD) : null);
        return id;
    }

    private long incident(Long resident, String typeCode, String status, Long sourceAlert) {
        long id = TestUsers.nextId();
        long type = catalogId("M05.INCIDENT_TYPE", typeCode);
        var at = Timestamp.from(OLD);
        jdbc.update(
                "INSERT INTO incident (id, resident_id, incident_type_id, current_type_id, type_code, discovered_at,"
                        + " description, initial_level, level, recorded_by, created_at, confirmed_at, status,"
                        + " closed_at, source_alert_id, floor_area_id)"
                        + " VALUES (?,?,?,?,?,?,'Kiểm lưu giữ','MEDIUM','MEDIUM',1,?,?,?,?,?,?)",
                id,
                resident,
                type,
                type,
                typeCode,
                at,
                at,
                at,
                status,
                "CLOSED".equals(status) ? at : null,
                sourceAlert,
                TestUsers.nextId());
        return id;
    }

    private long simpleRecord(
            String kind, Long resident, String entityType, Long entityId, String status, String content) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record (id, kind, module, resident_id, entity_type, entity_id, status, content,"
                        + " created_at, created_by) VALUES (?,?,'M05',?,?,?,?,CAST(? AS JSON),?,1)",
                id,
                kind,
                resident,
                entityType,
                entityId,
                status,
                content,
                Timestamp.from(OLD));
        return id;
    }

    @Test
    void FR_051a_residentChainIsFoundAndDeletedOnlyByRetentionAccount() {
        long resident = resident(ResidentStatus.RESIDENCY_ENDED);
        long contact = resident(ResidentStatus.RESIDENCY_ENDED);
        long vital = vitalSign(resident, null);
        long remeasured = vitalSign(resident, vital);
        long threshold = setting("THRESHOLD", "PERSONAL", resident, "EXPIRED", OLD);
        long schedule = setting("MEASUREMENT_SCHEDULE", "PERSONAL", resident, "STOPPED", null);
        long alert = alert(resident, "VITAL_SIGN", "CONVERTED_TO_INCIDENT");
        long incident = incident(resident, "SUSPECTED_INFECTION", "CLOSED", alert);
        long list = simpleRecord("M05.CONTACT_LIST", null, "M05.INCIDENT", incident, "CONFIRMED", "{}");
        simpleRecord("M05.CONTACT_PERSON", contact, "M05.CONTACT_LIST", list, "CONFIRMED", "{}");
        long containment = simpleRecord(
                "M05.CONTAINMENT",
                null,
                null,
                null,
                "RELEASED",
                "{\"period\":{\"startedAt\":\"1939-12-01T00:00:00Z\",\"releasedAt\":\"1940-01-01T00:00:00Z\"},"
                        + "\"incidentIds\":[" + incident + "]}");

        assertThat(candidates("M06.VITAL_SIGN_RECORD")).contains(vital, remeasured);
        assertThat(provider("M06.VITAL_SIGN_RECORD").referencesTo(vital))
                .containsExactly(new DataRef("M06.VITAL_SIGN_RECORD", remeasured));
        assertThat(candidates("M06.THRESHOLD")).contains(threshold);
        assertThat(candidates("M06.MEASUREMENT_SCHEDULE")).contains(schedule);
        assertThat(candidates("M05.ALERT")).contains(alert);
        assertThat(provider("M05.ALERT").referencesTo(alert)).containsExactly(new DataRef("M05.INCIDENT", incident));
        assertThat(candidates("M05.INCIDENT")).contains(incident);
        assertThat(provider("M05.INCIDENT").referencesTo(incident))
                .containsExactlyInAnyOrder(
                        new DataRef("M05.CONTACT_LIST", list), new DataRef("M05.CONTAINMENT", containment));
        assertThat(candidates("M05.CONTACT_LIST")).contains(list);
        assertThat(provider("M05.CONTACT_LIST").referencesTo(list))
                .containsExactly(new DataRef("M05.INCIDENT", incident));
        assertThat(candidates("M05.CONTAINMENT")).contains(containment);
        assertThat(provider("M05.CONTAINMENT").referencesTo(containment))
                .containsExactly(new DataRef("M05.INCIDENT", incident));
        assertThat(providers.stream()
                        .flatMap(p -> p.referencing(new DataRef(SharedRowRetention.RESIDENT, resident)).stream())
                        .toList())
                .contains(
                        new DataRef("M06.VITAL_SIGN_RECORD", vital),
                        new DataRef("M06.THRESHOLD", threshold),
                        new DataRef("M06.MEASUREMENT_SCHEDULE", schedule),
                        new DataRef("M05.ALERT", alert),
                        new DataRef("M05.INCIDENT", incident));
        assertThat(providers.stream()
                        .flatMap(p -> p.referencing(new DataRef(SharedRowRetention.RESIDENT, contact)).stream())
                        .toList())
                .containsExactly(new DataRef("M05.CONTACT_LIST", list));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM incident WHERE id = ?", incident))
                .isInstanceOf(DataAccessException.class);
        database.transaction().executeWithoutResult(tx -> {
            provider("M05.CONTAINMENT").delete(Set.of(containment));
            provider("M05.CONTACT_LIST").delete(Set.of(list));
            provider("M05.INCIDENT").delete(Set.of(incident));
            provider("M05.ALERT").delete(Set.of(alert));
            provider("M06.VITAL_SIGN_RECORD").delete(Set.of(remeasured));
            provider("M06.VITAL_SIGN_RECORD").delete(Set.of(vital));
            provider("M06.THRESHOLD").delete(Set.of(threshold));
            provider("M06.MEASUREMENT_SCHEDULE").delete(Set.of(schedule));
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM incident WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alert WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM vital_sign_record WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM monitoring_setting WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE id IN (?, ?)", Integer.class, list, containment))
                .isZero();
    }

    @Test
    void FR_051_dataOfResidentsStillInResidenceIsKept() {
        long resident = resident(ResidentStatus.IN_RESIDENCE);
        long ended = resident(ResidentStatus.RESIDENCY_ENDED);
        long vital = vitalSign(resident, null);
        long alert = alert(resident, "VITAL_SIGN", "CLOSED");
        long incident = incident(ended, "SUSPECTED_INFECTION", "CLOSED", null);
        long list = simpleRecord("M05.CONTACT_LIST", null, "M05.INCIDENT", incident, "CONFIRMED", "{}");
        // Người tiếp xúc còn ở viện: danh sách giữ tới khi cả chuỗi hết hạn (data-model 007 mục 14).
        simpleRecord("M05.CONTACT_PERSON", resident, "M05.CONTACT_LIST", list, "CONFIRMED", "{}");

        assertThat(candidates("M06.VITAL_SIGN_RECORD")).doesNotContain(vital);
        assertThat(candidates("M05.ALERT")).doesNotContain(alert);
        assertThat(candidates("M05.INCIDENT")).contains(incident);
        assertThat(candidates("M05.CONTACT_LIST")).doesNotContain(list);
    }

    @Test
    void FR_050_unlinkedRecordsExpireFromTheirEndAndShareRowsWithLinkedType() {
        long closedFacility = incident(null, "FACILITY", "CLOSED", null);
        long openFacility = incident(null, "FACILITY", "IN_PROGRESS", null);
        long assist = alert(null, "STAFF_ASSIST_REQUEST", "CLOSED");
        long expiredDefault = setting("THRESHOLD", "FACILITY", null, "EXPIRED", OLD);
        long effectiveDefault = setting("THRESHOLD", "FACILITY", null, "PENDING", null);

        var facility = provider("M05.FACILITY_INCIDENT");
        assertThat(facility.retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        assertThat(facility.sharedRowEntityType()).isEqualTo("M05.INCIDENT");
        assertThat(candidates("M05.FACILITY_INCIDENT")).contains(closedFacility).doesNotContain(openFacility);
        assertThat(candidates("M05.INCIDENT")).doesNotContain(closedFacility);
        assertThat(provider("M05.UNLINKED_ALERT").sharedRowEntityType()).isEqualTo("M05.ALERT");
        assertThat(candidates("M05.UNLINKED_ALERT")).contains(assist);
        assertThat(provider("M06.FACILITY_THRESHOLD").sharedRowEntityType()).isEqualTo("M06.THRESHOLD");
        assertThat(candidates("M06.FACILITY_THRESHOLD"))
                .contains(expiredDefault)
                .doesNotContain(effectiveDefault);
        assertThat(provider("M05.CONTAINMENT").retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
    }
}
