package com.eldercare.backend.common.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import com.eldercare.backend.support.fake.file.InMemoryS3Client;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 001 T003 (research H-13, NFR-08): {@link FileApi} thật. Tải lên kiểm kiểu, kích thước rồi lưu vào S3 và ghi dòng
 * {@code file}; lỗi S3 thì không có dòng; lệnh nghiệp vụ gắn tệp một lần trong transaction của lệnh; tệp đã gắn không
 * đổi được ở cơ sở dữ liệu; tải xuống theo quyền của module sở hữu bản ghi.
 */
@Import({FakeFileConfig.class, FileServiceIT.Providers.class})
class FileServiceIT extends MySqlIntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};
    private static final long UPLOADER = 4_100_001L;
    private static final String OWNER_TYPE = "TEST.OWNER";

    /** Quyền tải của module sở hữu giả: test đặt có hoặc không. */
    static class AllowDownload implements FilePermissionProvider {

        final AtomicBoolean allowed = new AtomicBoolean();

        @Override
        public String ownerType() {
            return OWNER_TYPE;
        }

        @Override
        public boolean canDownload(long ownerId) {
            return allowed.get();
        }
    }

    @TestConfiguration
    static class Providers {

        @Bean
        AllowDownload allowDownload() {
            return new AllowDownload();
        }
    }

    @Autowired
    FileApi files;

    @Autowired
    FileService service;

    @Autowired
    InMemoryS3Client s3;

    @Autowired
    AllowDownload provider;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transactions;

    @BeforeEach
    void reset() {
        s3.failPuts(false);
        provider.allowed.set(false);
    }

    @AfterEach
    void restore() {
        s3.failPuts(false);
    }

    @Test
    void upload_storesObjectInS3AndRowWithoutOwner() {
        int before = s3.count();

        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);

        assertThat(s3.count()).isEqualTo(before + 1);
        var row = jdbc.queryForMap("SELECT * FROM file WHERE id = ?", id);
        assertThat(s3.contains((String) row.get("uuid"))).isTrue();
        assertThat(row.get("original_name")).isEqualTo("scan.png");
        assertThat(row.get("mime_type")).isEqualTo(FileTypes.PNG);
        assertThat(((Number) row.get("size_bytes")).longValue()).isEqualTo(PNG.length);
        assertThat((String) row.get("sha256")).hasSize(64);
        assertThat(((Number) row.get("uploaded_by")).longValue()).isEqualTo(UPLOADER);
        assertThat(row.get("owner_type")).isNull();
        assertThat(row.get("attached_at")).isNull();
    }

    @Test
    void attach_setsOwnerOnce() {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);

        transactions.executeWithoutResult(s -> files.attach(id, OWNER_TYPE, 77));

        var row = jdbc.queryForMap("SELECT owner_type, owner_id, attached_at FROM file WHERE id = ?", id);
        assertThat(row.get("owner_type")).isEqualTo(OWNER_TYPE);
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(77);
        assertThat(row.get("attached_at")).isNotNull();

        assertThatThrownBy(() -> transactions.executeWithoutResult(s -> files.attach(id, OWNER_TYPE, 78)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("FILE_ALREADY_ATTACHED");
                    assertThat(e.status().value()).isEqualTo(409);
                });
    }

    @Test
    void download_attached_followsOwnerModulePermission() {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);
        transactions.executeWithoutResult(s -> files.attach(id, OWNER_TYPE, 9));

        assertThatThrownBy(() -> service.download(id, UPLOADER)).isInstanceOf(PermissionDeniedException.class);
        provider.allowed.set(true);
        assertThat(service.download(id, UPLOADER + 1).content()).isEqualTo(PNG);
    }

    private int rowCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM file", Integer.class);
    }
}
