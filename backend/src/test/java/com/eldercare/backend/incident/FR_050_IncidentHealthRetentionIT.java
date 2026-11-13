package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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
