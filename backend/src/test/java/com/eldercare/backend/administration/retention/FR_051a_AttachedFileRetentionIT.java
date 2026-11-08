package com.eldercare.backend.administration.retention;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.retention.scheduling.ExpiredDataPurgeJob;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import com.eldercare.backend.support.fake.file.InMemoryS3Client;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * FR-051a, FR-050 (000 T112): tệp đính kèm (bảng {@code file} của {@code common.file}) đi kèm bản ghi sở hữu trong chuỗi
 * loại bỏ: chuỗi hết hạn thì xóa nội dung S3 rồi dòng {@code file} cùng transaction với bản ghi, không để lại dòng tệp
 * trỏ tới bản ghi đã xóa; xóa S3 lỗi thì giữ nguyên cả chuỗi (đợt sau thử lại); bản ghi còn hạn giữ tệp.
 */
@Import({RetentionFixtures.class, FakeFileConfig.class})
class FR_051a_AttachedFileRetentionIT extends MySqlIntegrationTestBase {

    private static final String FLOOR = SampleFloorRecordProvider.TYPE;
    private static final CurrentUser NURSE = FakePermissions.user(10, Role.NURSING_STAFF);
    private static final CurrentUser SCHEDULER = FakePermissions.user(0, Role.SCHEDULER);

    @Autowired
    ExpiredDataPurgeJob job;

    @Autowired
    RetentionFixtures.Control control;

    @Autowired
    SwitchableUser user;

    @Autowired
    BusinessTime time;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InMemoryS3Client s3;

    @Value("${eldercare.file.bucket:${ELDERCARE_S3_BUCKET:}}")
    String bucket;

    private Instant expired;
    private Instant recent;

    @BeforeEach
    void setUp() {
        control.clear();
        s3.failDeletes(false);
        expired = time.now().atZone(BusinessTime.ZONE).minusYears(11).toInstant();
        recent = time.now().minusSeconds(86_400);
        runJob();
    }

    @AfterEach
    void tearDown() {
        s3.failDeletes(false);
        control.clear();
        user.set(NURSE);
    }

    private String runJob() {
        user.set(SCHEDULER);
        try {
            return job.run();
        } finally {
            user.set(NURSE);
        }
    }

    private long floorRecord(Instant at) {
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, note, floor_area_id) VALUES (?, ?, 'ghi nhận', 2)",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                NURSE.accountId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** Tệp đã gắn vào bản ghi, nội dung ở bucket giả. */
    private String attachedFile(String ownerType, long ownerId, Instant at) {
        String key = UUID.randomUUID().toString();
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromString("ảnh"));
        LocalDateTime when = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        jdbc.update(
                "INSERT INTO file (uuid, original_name, mime_type, size_bytes, sha256, uploaded_by, uploaded_at,"
                        + " owner_type, owner_id, attached_at) VALUES (?, 'anh.jpg', 'image/jpeg', 3, ?, ?, ?, ?, ?, ?)",
                key,
                "0".repeat(64),
                NURSE.accountId(),
                when,
                ownerType,
                ownerId,
                when);
        return key;
    }

    private boolean recordExists(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sample_floor_record WHERE id = ?", Integer.class, id) > 0;
    }

    private boolean fileRowExists(String key) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM file WHERE uuid = ?", Integer.class, key) > 0;
    }

    private String deletedFiles(String runId) {
        return String.valueOf(jdbc.queryForObject(
                "SELECT JSON_EXTRACT(details, '$.deleted.\"M15.ATTACHED_FILE\".count') FROM audit_log"
                        + " WHERE action = 'EXPIRED_DATA_PURGED' AND entity_id = ?",
                Object.class,
                runId));
    }

    @Test
    void FR_051a_expiredRecord_isPurgedWithAttachedFileRow_andStoredContent() {
        long old = floorRecord(expired);
        String file = attachedFile(FLOOR, old, expired);

        String runId = runJob();

        assertThat(recordExists(old)).isFalse();
        assertThat(fileRowExists(file)).isFalse();
        assertThat(s3.contains(file)).isFalse();
        assertThat(deletedFiles(runId)).isEqualTo("1");
    }
}
