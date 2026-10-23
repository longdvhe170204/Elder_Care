package com.cambae.backend.common.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import com.cambae.backend.support.fake.file.InMemoryS3Client;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 001 T004 (research H-13; đồng hồ giả lập): tệp tải lên mà sau 24 giờ vẫn chưa gắn vào bản ghi nào bị job dọn xóa
 * (đối tượng S3 rồi dòng {@code file}); tệp đã gắn, tệp còn mới không bị đụng; S3 xóa lỗi thì giữ dòng để lần sau thử
 * lại. Đây là dữ liệu tạm, không phải bản ghi nghiệp vụ (000 FR-010a).
 */
@Import(FakeFileConfig.class)
class UnattachedFileCleanupIT extends MySqlIntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1};

    @Autowired
    FileApi files;

    @Autowired
    UnattachedFileCleanup cleanup;

    @Autowired
    InMemoryS3Client s3;

    @Autowired
    TestClockControl clock;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transactions;

    @BeforeEach
    @AfterEach
    void reset() {
        clock.reset();
        s3.failDeletes(false);
    }

    private long upload() {
        return files.upload("scan.png", FileTypes.PNG, PNG, 4_200_001L);
    }

    private boolean rowExists(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM file WHERE id = ?", Integer.class, id) == 1;
    }

    private String key(long id) {
        return jdbc.queryForObject("SELECT uuid FROM file WHERE id = ?", String.class, id);
    }

    @Test
    void unattachedFileOlderThan24Hours_isDeletedFromS3AndDatabase() {
        long id = upload();
        String key = key(id);
        clock.advanceBy(Duration.ofHours(24).plusMinutes(1));

        cleanup.run();

        assertThat(rowExists(id)).isFalse();
        assertThat(s3.contains(key)).isFalse();
    }

    @Test
    void youngUnattachedFile_isKept() {
        long id = upload();
        clock.advanceBy(Duration.ofHours(23));

        cleanup.run();

        assertThat(rowExists(id)).isTrue();
        assertThat(s3.contains(key(id))).isTrue();
    }

    @Test
    void attachedFile_isNeverDeleted() {
        long id = upload();
        transactions.executeWithoutResult(s -> files.attach(id, "TEST.OWNER", 1));
        clock.advanceBy(Duration.ofDays(30));

        cleanup.run();

        assertThat(rowExists(id)).isTrue();
        assertThat(s3.contains(key(id))).isTrue();
    }

    @Test
    void s3DeleteFailure_keepsTheRowForTheNextRun() {
        long id = upload();
        String key = key(id);
        clock.advanceBy(Duration.ofHours(25));
        s3.failDeletes(true);

        cleanup.run();
        assertThat(rowExists(id)).isTrue();

        s3.failDeletes(false);
        cleanup.run();
        assertThat(rowExists(id)).isFalse();
        assertThat(s3.contains(key)).isFalse();
    }
}
