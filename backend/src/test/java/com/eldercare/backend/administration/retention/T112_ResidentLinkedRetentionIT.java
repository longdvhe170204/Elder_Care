package com.eldercare.backend.administration.retention;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 000 T112 (module-interfaces 000 mục 1.7; FR-051, FR-051a): bản ghi của module khác gắn hồ sơ người cao tuổi được trả
 * qua {@code referencing} để hồ sơ không bị xóa trước. Yêu cầu phê duyệt (000), yêu cầu thông báo (009) gắn người cao
 * tuổi hết hạn theo hồ sơ (CFG-M15-03 từ lúc sang trạng thái cuối), nếu không chuỗi của hồ sơ luôn bị giữ. Đồ gửi (013)
 * đọc theo trang. Dùng chung context với {@code FR_051a_MedicationRetentionIT}.
 */
@Import(FakeM01Config.class)
class T112_ResidentLinkedRetentionIT extends MySqlIntegrationTestBase {
    private static final Instant OLD = Instant.parse("1940-01-01T00:00:00Z");
    /** Hạn của loại không gắn hồ sơ: trước mọi bản ghi của test, để chỉ hạn theo hồ sơ làm bản ghi hết hạn. */
    private static final Instant CUTOFF = Instant.parse("1930-01-01T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeResidents residents;

    @Autowired
    List<RetentionProvider> providers;

    @Test
    void T112_approvalRequestsOfAnEndedResidentExpireWithTheProfileAndAreReferenced() {
        long resident = endedResident();
        long applied = approvalRequest(resident, "APPLIED");
        long pending = approvalRequest(resident, "PENDING_APPROVAL");
        var provider = provider("M15.APPROVAL_REQUEST");

        assertThat(provider.expiredCandidates(CUTOFF, null, 1000).map(Candidate::id))
                .contains(applied)
                .doesNotContain(pending);
        // chỉ yêu cầu xóa được (trạng thái cuối) đi cùng chuỗi hồ sơ; yêu cầu còn mở không giữ hồ sơ mãi
        assertThat(provider.referencing(new DataRef(SharedRowRetention.RESIDENT, resident)))
                .containsExactly(new DataRef("M15.APPROVAL_REQUEST", applied));
    }

    @Test
    void T112_notificationRequestsOfAnEndedResidentExpireWithTheProfileAndAreReferenced() {
        long resident = endedResident();
        long request = notificationRequest(resident);
        var provider = provider("M13.NOTIFICATION_REQUEST");

        assertThat(provider.expiredCandidates(CUTOFF, null, 1000).map(Candidate::id))
                .contains(request);
        assertThat(provider.referencing(new DataRef(SharedRowRetention.RESIDENT, resident)))
                .containsExactly(new DataRef("M13.NOTIFICATION_REQUEST", request));
    }

    @Test
    void T112_belongingsAreReferencedAndPaged() {
        long resident = endedResident();
        long type = catalogItem("BELONGING_TYPE");
        long condition = catalogItem("BELONGING_CONDITION");
        long first = belonging(resident, type, condition);
        long second = belonging(resident, type, condition);
        var provider = provider("M12.BELONGING");

        assertThat(provider.referencing(new DataRef(SharedRowRetention.RESIDENT, resident)))
                .containsExactlyInAnyOrder(new DataRef("M12.BELONGING", first), new DataRef("M12.BELONGING", second));
        var all = provider.expiredCandidates(Instant.now(), null, 100_000).toList();
        int index = all.stream().map(Candidate::id).toList().indexOf(first);
        assertThat(index).isNotNegative();
        assertThat(provider.expiredCandidates(Instant.now(), all.get(index), 1).map(Candidate::id))
                .containsExactly(all.get(index + 1).id());
    }

    private RetentionProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private long endedResident() {
        long id = TestUsers.nextId();
        residents.resident(id, "[Hồ sơ kiểm lưu giữ T112]", null, ResidentStatus.RESIDENCY_ENDED);
        residents.changeStatus(id, ResidentStatus.RESIDENCY_ENDED, OLD);
        // ResidencyEndApi (004 cài) đọc lịch sử trạng thái thật của 001, không qua FakeResidents
        jdbc.update(
                "INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at,"
                        + " actor_id, by_system) VALUES ('M01.RESIDENT', ?, 'STATUS', 'M01', 'POINT', 'RESIDENCY_ENDED',"
                        + " ?, 1, TRUE)",
                id,
                Timestamp.from(OLD));
        return id;
    }

    private long approvalRequest(long resident, String status) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO approval_request (id, type, status, content, resident_id, requester_id, created_at, reason,"
                        + " requested_effective_date) VALUES (?, 'T112_TEST', ?, JSON_OBJECT(), ?, 1, ?, 'Kiểm lưu giữ',"
                        + " '1940-01-01')",
                id,
                status,
                resident,
                Timestamp.from(OLD));
        return id;
    }

    private long notificationRequest(long resident) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO notification_request (id, level, resident_id, received_at, intake_result, contents,"
                        + " recipient_groups) VALUES (?, 'LOW', ?, ?, 'VALID', JSON_ARRAY(), JSON_ARRAY())",
                id,
                resident,
                Timestamp.from(OLD));
        return id;
    }

    private long catalogItem(String type) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id, catalog_type, module, code, name, status)"
                        + " VALUES (?, ?, 'M12', ?, ?, 'ACTIVE')",
                id,
                type,
                "T112-" + id,
                "[Danh mục kiểm lưu giữ " + id + "]");
        return id;
    }

    private long belonging(long resident, long type, long condition) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO belonging (id, resident_id, type_id, description, quantity, status, condition_level_id,"
                        + " created_by, created_at) VALUES (?, ?, ?, 'Đồ kiểm lưu giữ', 1, 'HELD', ?, 1, ?)",
                id,
                resident,
                type,
                condition,
                Timestamp.from(OLD));
        return id;
    }
}
