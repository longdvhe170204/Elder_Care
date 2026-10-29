package com.eldercare.backend.common.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import com.eldercare.backend.support.fake.file.InMemoryS3Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
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
    void upload_wrongTypeOrTooLarge_isRejectedWithoutObjectOrRow() {
        int objects = s3.count();
        int rows = rowCount();

        assertThatThrownBy(() -> files.upload("a.png", FileTypes.PDF, PNG, UPLOADER))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("FILE_TYPE_NOT_ALLOWED");
                });
        byte[] big = Arrays.copyOf(PNG, (int) FileTypes.MAX_SIZE + 1);
        assertThatThrownBy(() -> files.upload("big.png", FileTypes.PNG, big, UPLOADER))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("FILE_TOO_LARGE");
                });

        assertThat(s3.count()).isEqualTo(objects);
        assertThat(rowCount()).isEqualTo(rows);
    }

    @Test
    void upload_s3Failure_leavesNoRow() {
        int rows = rowCount();
        s3.failPuts(true);

        assertThatThrownBy(() -> files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("FILE_STORAGE_ERROR");
                    assertThat(e.status().value()).isEqualTo(503);
                });

        assertThat(rowCount()).isEqualTo(rows);
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
    void attach_unknownFile_is404() {
        assertThatThrownBy(() -> transactions.executeWithoutResult(s -> files.attach(987_654_321L, OWNER_TYPE, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void attach_outsideTransaction_isRefused() {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);

        assertThatThrownBy(() -> files.attach(id, OWNER_TYPE, 1)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void attachedFile_cannotBeChangedInDatabase() throws SQLException {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);
        transactions.executeWithoutResult(s -> files.attach(id, OWNER_TYPE, 5));

        try (Connection admin = adminConnection();
                var statement = admin.prepareStatement("UPDATE file SET owner_id = 6 WHERE id = ?")) {
            statement.setLong(1, id);
            assertThatThrownBy(statement::executeUpdate).hasMessageContaining("attached");
        }
        // tài khoản ứng dụng cũng không xóa được tệp đã gắn
        assertThatThrownBy(() -> jdbc.update("DELETE FROM file WHERE id = ?", id))
                .hasMessageContaining("retention");
    }

    @Test
    void unattachedFile_canBeDeletedByApplication() {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);

        assertThat(jdbc.update("DELETE FROM file WHERE id = ?", id)).isEqualTo(1);
    }

    @Test
    void download_unattached_onlyUploader() {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);

        FileContent content = service.download(id, UPLOADER);

        assertThat(content.originalName()).isEqualTo("scan.png");
        assertThat(content.mimeType()).isEqualTo(FileTypes.PNG);
        assertThat(content.content()).isEqualTo(PNG);
        assertThatThrownBy(() -> service.download(id, UPLOADER + 1)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void download_attached_followsOwnerModulePermission() {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);
        transactions.executeWithoutResult(s -> files.attach(id, OWNER_TYPE, 9));

        assertThatThrownBy(() -> service.download(id, UPLOADER)).isInstanceOf(PermissionDeniedException.class);
        provider.allowed.set(true);
        assertThat(service.download(id, UPLOADER + 1).content()).isEqualTo(PNG);
    }

    @Test
    void download_attachedWithoutProvider_isDenied() {
        long id = files.upload("scan.png", FileTypes.PNG, PNG, UPLOADER);
        transactions.executeWithoutResult(s -> files.attach(id, "NO.PROVIDER", 9));

        assertThatThrownBy(() -> service.download(id, UPLOADER)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void download_unknownFile_is404() {
        assertThatThrownBy(() -> service.download(987_654_321L, UPLOADER)).isInstanceOf(NotFoundException.class);
    }

    private int rowCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM file", Integer.class);
    }
}
