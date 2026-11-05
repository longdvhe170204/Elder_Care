package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** T008: provider thật và MySQL thật; chỉ trạng thái hồ sơ dùng fake chung của B. */
@Import(FakeM01Config.class)
class CostRetentionIT extends MySqlIntegrationTestBase {
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

    private final List<Long> periods = new ArrayList<>(), statements = new ArrayList<>(), items = new ArrayList<>();

    private RetentionProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    @BeforeEach
    void idsDoNotCollideWithPriorRuns() {
        long used = jdbc.queryForObject(
                "SELECT GREATEST((SELECT COALESCE(MAX(id),0) FROM cost_statement),(SELECT COALESCE(MAX(id),0) FROM cost_item))",
                Long.class);
        while (TestUsers.nextId() <= used) {
            /* Cô lập khỏi id của lượt trước. */
        }
    }

    @AfterEach
    void removeOnlyThisFixturesRows() {
        database.transaction().executeWithoutResult(tx -> {
            for (long id : items.reversed()) database.jdbc().update("DELETE FROM cost_item WHERE id=?", id);
            for (long id : statements.reversed())
                database.jdbc().update("DELETE FROM cost_statement WHERE id=? AND kind='STATEMENT'", id);
            for (long id : periods.reversed())
                database.jdbc().update("DELETE FROM cost_statement WHERE id=? AND kind='PERIOD'", id);
        });
    }

    private long person(ResidentStatus status, Instant ended) {
        long id = TestUsers.nextId();
        residents.resident(id, "[Hồ sơ kiểm lưu giữ chi phí]", null, status);
        if (ended != null) residents.changeStatus(id, status, ended);
        return id;
    }

    private long period(Instant created) {
        long id = TestUsers.nextId();
        long seed = Math.floorMod(id, 72000);
        var month = YearMonth.of(2000 + (int) seed / 12, 1 + (int) seed % 12);
        jdbc.update(
                "INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at) VALUES (?,'PERIOD',?,?,?,?,?)",
                id,
                month.toString(),
                month.atDay(1),
                month.atEndOfMonth(),
                "PERIOD:" + month,
                Timestamp.from(created));
        periods.add(id);
        return id;
    }

    private long statement(long period, long resident) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at) VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)",
                id,
                period,
                resident,
                "STATEMENT:" + period + ":" + resident + ":REGULAR",
                Timestamp.from(OLD));
        statements.add(id);
        return id;
    }

    private long item(long period, long statement, long resident) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,description,incurred_on,quantity,unit,amount,reason,created_at,created_by) VALUES (?,?,?,?,'OTHER','MANUAL','[Khoản kiểm lưu giữ]','1940-01-01',1,'UNIT',300,'Kiểm lưu giữ',?,1)",
                id,
                resident,
                statement,
                period,
                Timestamp.from(OLD));
        items.add(id);
        return id;
    }

    private long adjustment(long period, long statement, long resident, long original, long originalPeriod) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,description,incurred_on,quantity,unit,amount,reason,original_item_id,original_period_id,created_at,created_by) VALUES (?,?,?,?,'OTHER','ADJUSTMENT','[Điều chỉnh kiểm lưu giữ]','1940-01-01',1,'UNIT',-100,'Kiểm chuỗi',?,?,?,1)",
                id,
                resident,
                statement,
                period,
                original,
                originalPeriod,
                Timestamp.from(OLD));
        items.add(id);
        return id;
    }

    @Test
    void FR_050_threeActualTypesHaveTheRequiredRetentionKinds() {
        assertThat(provider("M11.COST_PERIOD").retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        assertThat(provider("M11.COST_STATEMENT").retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        assertThat(provider("M11.COST_ITEM").retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
    }

    @Test
    void FR_050_periodUsesItsCreationMilestoneAndStrictCutoff() {
        long old = period(OLD), boundary = period(CUTOFF);
        var candidates =
                provider("M11.COST_PERIOD").expiredCandidates(CUTOFF, 1000).toList();
        assertThat(candidates).anyMatch(c -> c.id() == old && c.at().equals(OLD));
        assertThat(candidates).noneMatch(c -> c.id() == boundary);
        assertThat(provider("M11.COST_PERIOD").expiredCandidates(CUTOFF, 1).toList())
                .hasSize(1);
        assertThat(provider("M11.COST_PERIOD").expiredCandidates(CUTOFF, 0).toList())
                .isEmpty();
    }

    @Test
    void FR_050_oldCreationDoesNotExpireAnActiveResidentOrMissingFinalMilestone() {
        long active = person(ResidentStatus.IN_RESIDENCE, null);
        long missing = person(ResidentStatus.RESIDENCY_ENDED, null);
        long p = period(OLD), a = statement(p, active), b = statement(p, missing);
        long x = item(p, a, active), y = item(p, b, missing);
        assertThat(provider("M11.COST_STATEMENT").expiredCandidates(CUTOFF, 1000))
                .noneMatch(c -> c.id() == a || c.id() == b);
        assertThat(provider("M11.COST_ITEM").expiredCandidates(CUTOFF, 1000))
                .noneMatch(c -> c.id() == x || c.id() == y);
    }

    @Test
    void FR_050_residentFinalMilestoneControlsStatementsAndItems() {
        var ended = OLD.plusSeconds(3600);
        long resident = person(ResidentStatus.RESIDENCY_ENDED, ended), p = period(OLD);
        long s = statement(p, resident), i = item(p, s, resident);
        assertThat(provider("M11.COST_STATEMENT").expiredCandidates(CUTOFF, 1000))
                .anyMatch(c -> c.id() == s && c.at().equals(ended));
        assertThat(provider("M11.COST_ITEM").expiredCandidates(CUTOFF, 1000))
                .anyMatch(c -> c.id() == i && c.at().equals(ended));
    }

    @Test
    void FR_050_exactFinalMilestoneIsNotYetBeforeTheCutoff() {
        long resident = person(ResidentStatus.DECEASED, CUTOFF), p = period(OLD);
        long s = statement(p, resident), i = item(p, s, resident);
        assertThat(provider("M11.COST_STATEMENT").expiredCandidates(CUTOFF, 1000))
                .noneMatch(c -> c.id() == s);
        assertThat(provider("M11.COST_ITEM").expiredCandidates(CUTOFF, 1000)).noneMatch(c -> c.id() == i);
    }

    @Test
    void FR_051a_referencesIncludeUnexpiredChildrenAndCrossPeriodAdjustments() {
        long ended = person(ResidentStatus.RESIDENCY_ENDED, OLD), active = person(ResidentStatus.IN_RESIDENCE, null);
        long p = period(OLD), other = period(OLD), s = statement(p, ended), t = statement(other, active);
        long original = item(p, s, ended), correction = adjustment(other, t, active, original, p);
        assertThat(provider("M11.COST_PERIOD").referencesTo(p))
                .contains(
                        new DataRef("M11.COST_STATEMENT", s),
                        new DataRef("M11.COST_ITEM", original),
                        new DataRef("M11.COST_ITEM", correction));
        assertThat(provider("M11.COST_STATEMENT").referencesTo(s))
                .containsExactly(new DataRef("M11.COST_ITEM", original));
        assertThat(provider("M11.COST_ITEM").referencesTo(original))
                .containsExactly(new DataRef("M11.COST_ITEM", correction));
        assertThat(provider("M11.COST_ITEM").expiredCandidates(CUTOFF, 1000)).noneMatch(c -> c.id() == correction);
    }

    @Test
    void FR_051_deletionUsesRetentionAccountEvenForLockedRows() {
        long resident = person(ResidentStatus.RESIDENCY_ENDED, OLD), p = period(OLD), s = statement(p, resident);
        long i = item(p, s, resident);
        jdbc.update("UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", Timestamp.from(OLD), i);
        jdbc.update(
                "UPDATE cost_statement SET status='FINALIZED',total=300,totals_by_type=JSON_ARRAY(JSON_OBJECT('type','OTHER','total',300)),finalized_at=?,locked_at=? WHERE id=?",
                Timestamp.from(OLD),
                Timestamp.from(OLD),
                s);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM cost_item WHERE id=?", i))
                .isInstanceOf(DataAccessException.class);
        database.transaction().executeWithoutResult(tx -> {
            provider("M11.COST_ITEM").delete(List.of(i));
            provider("M11.COST_STATEMENT").delete(List.of(s));
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE id=?", Integer.class, i))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE id=?", Integer.class, s))
                .isZero();
    }

    @Test
    void FR_051_deletionRollsBackTheWholeRequestedTransaction() {
        long resident = person(ResidentStatus.RESIDENCY_ENDED, OLD), p = period(OLD), s = statement(p, resident);
        long i = item(p, s, resident);
        assertThatThrownBy(() -> database.transaction().executeWithoutResult(tx -> {
                    provider("M11.COST_ITEM").delete(List.of(i));
                    provider("M11.COST_STATEMENT").delete(List.of(s));
                    throw new IllegalStateException("Kiểm rollback");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE id=?", Integer.class, i))
                .isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE id=?", Integer.class, s))
                .isOne();
    }

    @Test
    void FR_051_periodAndStatementKindsAreNeverInterchangeable() {
        long resident = person(ResidentStatus.IN_RESIDENCE, null), p = period(OLD), s = statement(p, resident);
        database.transaction().executeWithoutResult(tx -> {
            provider("M11.COST_PERIOD").delete(List.of(s));
            provider("M11.COST_STATEMENT").delete(List.of(p));
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE id IN (?,?)", Integer.class, p, s))
                .isEqualTo(2);
        assertThat(provider("M11.COST_PERIOD").referencesTo(s)).isEmpty();
        assertThat(provider("M11.COST_STATEMENT").referencesTo(p)).isEmpty();
    }
}
