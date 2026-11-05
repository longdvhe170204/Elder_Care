package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.staffing.internal.handover.domain.HandoverItemInput;
import com.eldercare.backend.staffing.internal.handover.persistence.HandoverStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T050: id mục cục bộ không lẫn giữa bàn giao; giữ bổ sung sau lập và khóa phần đã chốt. */
@Transactional
class HandoverEmbeddedItemsIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    HandoverStore store;

    private long staff;
    private long template;
    private long first;
    private long second;
    private static final Instant NOW = Instant.parse("2026-01-01T01:00:00.123456Z");

    @BeforeEach
    void fixtures() {
        staff = TestUsers.nextId();
        template = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position) VALUES (?,'[Nhân viên JSON]','Điều dưỡng')", staff);
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES (?,'M09.SHIFT_TEMPLATE','M09',?,'ACTIVE',JSON_OBJECT('startTime','00:00:00','endTime','08:00:00','scopeType','FLOOR','requiresHandover',true))",
                template,
                "[Mẫu " + template + "]");
        first = create();
        second = create();
    }

    private long create() {
        long schedule = TestUsers.nextId(), shift = TestUsers.nextId(), floor = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO shift_schedule(id,month,scope_type,scope_id) VALUES (?,'2026-01','FLOOR',?)",
                schedule,
                floor);
        jdbc.update(
                "INSERT INTO work_shift(id,schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover) VALUES (?,?,?,'2026-01-01','2026-01-01 08:00','FLOOR',?,1)",
                shift,
                schedule,
                template,
                floor);
        return store.create(shift, null, staff, staff, NOW, null);
    }

    private HandoverItemInput item(String description) {
        return new HandoverItemInput("FOLLOW_UP_ISSUE", null, null, staff, null, description, null, false);
    }

    @Test
    void FR_040_localItemIdsNeverOverwriteAnotherHandover_andAreNotReused() {
        long a = store.add(first, item("Mục A"), "FINALIZED", "[Điều dưỡng]", NOW);
        long b = store.add(second, item("Mục B"), "FINALIZED", "[Điều dưỡng]", NOW);
        assertThat(a).isEqualTo(1);
        assertThat(b).isEqualTo(1);
        store.update(first, a, item("Sửa A"), "[Điều dưỡng]");
        assertThat(store.item(second, b).description()).isEqualTo("Mục B");
        store.delete(first, a);
        assertThat(store.add(first, item("Mục mới"), "FINALIZED", "[Điều dưỡng]", NOW))
                .isGreaterThan(a);
        assertThat(store.item(second, b).description()).isEqualTo("Mục B");
    }

    @Test
    void FR_044_preparedJsonAllowsAppendAfterPreparationButProtectsFrozenContent() {
        long a = store.add(first, item("Đã chốt"), "FINALIZED", "[Điều dưỡng]", NOW);
        store.assess(first, "Nhận định", Map.of(staff, "Nhận định NCT"));
        store.complete(first, staff, NOW, List.of());
        long b = store.add(first, item("Phát sinh"), "AFTER_PREPARATION", "[Điều dưỡng]", NOW);
        assertThat(store.items(first)).hasSize(2);
        assertThat(store.item(first, b).section()).isEqualTo("AFTER_PREPARATION");
        assertThatThrownBy(() -> store.update(first, a, item("Sửa phần cũ"), "[Điều dưỡng]"))
                .isInstanceOf(DataAccessException.class);
        assertThat(store.general(first)).isEqualTo("Nhận định");
        store.confirm(first, null, staff, NOW, null);
        assertThatThrownBy(() -> store.add(first, item("Sau xác nhận"), "AFTER_PREPARATION", "[Điều dưỡng]", NOW))
                .isInstanceOf(DataAccessException.class);
    }
}
