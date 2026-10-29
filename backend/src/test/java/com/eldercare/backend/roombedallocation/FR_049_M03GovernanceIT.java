package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** T010: đăng ký phân loại trường, phạm vi và lưu giữ qua API nền tảng, không đọc module khác. */
class FR_049_M03GovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    List<FieldClassificationProvider> fields;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    RetentionDatabase database;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_049_classificationSeparatesInfectionReasonsFromOrdinaryAllocationReasons() {
        var byType =
                fields.stream().collect(Collectors.toMap(FieldClassificationProvider::entityType, Function.identity()));
        assertThat(byType).containsKeys("M03.BED", "M03.ROOM", "M03.BED_ALLOCATION", "M03.ROOM_ISOLATION_HISTORY");
        assertThat(byType.get("M03.ROOM_ISOLATION_HISTORY").fieldGroups()).containsEntry("reason", InfoGroup.HEALTH);
        assertThat(byType.get("M03.ROOM").fieldGroups()).containsEntry("isolationReason", InfoGroup.HEALTH);
        assertThat(byType.get("M03.BED_ALLOCATION").fieldGroups())
                .containsEntry("infectionControlReason", InfoGroup.HEALTH);
        assertThat(byType.get("M03.BED_ALLOCATION").fieldGroups().getOrDefault("reason", InfoGroup.INTERNAL_PUBLIC))
                .isEqualTo(InfoGroup.INTERNAL_PUBLIC);
    }

    @Test
    void FR_034_scopeReadsTheOwningFloorAndFailsClosedForMissingData() {
        Map<String, ScopeAssignmentProvider> byType =
                scopes.stream().collect(Collectors.toMap(ScopeAssignmentProvider::entityType, Function.identity()));
        assertThat(byType).containsKeys("M03.BED", "M03.ROOM", "M03.BED_ALLOCATION");
        jdbc.update("INSERT INTO area(id,code,name) VALUES(903900,'M03-GOV-A','Khu kiểm phạm vi')");
        jdbc.update("INSERT INTO floor(id,area_id,code,name) VALUES(903900,903900,'M03-GOV-T','Tầng kiểm phạm vi')");
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(903900,'Loại kiểm phạm vi',2)");
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(903900,903900,903900,'M03-GOV-P','Phòng kiểm phạm vi','UNRESTRICTED')");
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(903900,903900,'M03-GOV-G')");
        assertThat(byType.get("M03.BED").assignScope(903900).floorAreaId()).isEqualTo(903900L);
        assertThat(byType.get("M03.ROOM").assignScope(903900).floorAreaId()).isEqualTo(903900L);
        assertThatThrownBy(() -> byType.get("M03.BED").assignScope(Long.MAX_VALUE))
                .isInstanceOf(InvalidPermissionDataException.class);
        assertThatThrownBy(() -> byType.get("M03.BED_ALLOCATION").assignScope(Long.MAX_VALUE))
                .isInstanceOf(InvalidPermissionDataException.class);
    }

    @Test
    void FR_051_historyCandidatesAreRemovedOnlyThroughTheRetentionConnection() {
        var byType = retention.stream().collect(Collectors.toMap(RetentionProvider::dataType, Function.identity()));
        assertThat(byType).containsKeys("M03.BED_ALLOCATION", "M03.BED_STATE_HISTORY", "M03.ROOM_ISOLATION_HISTORY");
        assertThat(byType.get("M03.BED_ALLOCATION").retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        assertThat(byType.get("M03.BED_ALLOCATION").expiredCandidates(Instant.parse("2001-01-01T00:00:00Z"), 10))
                .isEmpty();
        assertThat(byType.get("M03.ROOM_ISOLATION_HISTORY")
                        .expiredCandidates(Instant.parse("2001-01-01T00:00:00Z"), 10))
                .isEmpty();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(903901,'M03-RET-A','Khu kiểm lưu giữ')");
        jdbc.update("INSERT INTO floor(id,area_id,code,name) VALUES(903901,903901,'M03-RET-T','Tầng kiểm lưu giữ')");
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(903901,'Loại kiểm lưu giữ',2)");
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(903901,903901,903901,'M03-RET-P','Phòng kiểm lưu giữ','UNRESTRICTED')");
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(903901,903901,'M03-RET-G')");
        jdbc.update(
                "INSERT INTO bed_state_history(id,bed_id,to_state,event,occurred_at,basis_type,basis_id,floor_area_id) VALUES(903901,903901,'EMPTY','CREATE_BED','2000-01-01 00:00:00','TEST',1,903901)");
        var provider = byType.get("M03.BED_STATE_HISTORY");
        assertThat(provider.expiredCandidates(Instant.parse("2001-01-01T00:00:00Z"), 10)
                        .map(RetentionProvider.Candidate::id))
                .contains(903901L);
        database.transaction().executeWithoutResult(status -> provider.delete(List.of(903901L)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE id=903901", Integer.class))
                .isZero();
    }
}
