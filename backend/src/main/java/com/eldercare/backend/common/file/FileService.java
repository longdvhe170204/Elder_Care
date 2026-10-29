package com.eldercare.backend.common.file;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Bản cài {@link FileApi} (001 T003; research H-13): nội dung ở bucket S3 riêng tư qua {@link S3Client}, siêu dữ liệu ở
 * bảng {@code file}. Tải xuống đi qua backend và kiểm quyền mỗi lần, không phát URL ký sẵn (NFR-08).
 */
@Service
public class FileService implements FileApi {

    private final FileStore store;
    private final ObjectProvider<S3Client> s3;
    private final String bucket;
    private final BusinessTime time;
    // tra khi tải xuống, không khi dựng bean: provider của module sở hữu phụ thuộc ngược vào FileApi
    private final ObjectProvider<FilePermissionProvider> providers;

    FileService(
            FileStore store,
            ObjectProvider<S3Client> s3,
            @Value("${eldercare.file.bucket:${ELDERCARE_S3_BUCKET:}}") String bucket,
            BusinessTime time,
            ObjectProvider<FilePermissionProvider> providers) {
        this.store = store;
        this.s3 = s3;
        this.bucket = bucket;
        this.time = time;
        this.providers = providers;
    }

    @Override
    public long upload(String originalName, String mimeType, byte[] content, long uploadedBy) {
        FileTypes.check(mimeType, content, FileTypes.SYSTEM_GENERATED);
        S3Client client = client();
        String key = UUID.randomUUID().toString();
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(mimeType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw storageError();
        }
        try {
            return store.insert(key, originalName, mimeType, content.length, sha256(content), uploadedBy, time.now());
        } catch (RuntimeException e) {
            // không có dòng thì đối tượng không ai trỏ tới; dọn ngay, lỗi dọn không che lỗi gốc
            try {
                client.deleteObject(
                        DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (SdkException ignored) {
                // đối tượng mồ côi vô hại: không dòng nào trỏ tới, không ai đọc được
            }
            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void attach(long fileId, String ownerType, long ownerId) {
        if (store.attach(fileId, ownerType, ownerId, time.now()) == 1) {
            return;
        }
        if (store.find(fileId).isEmpty()) {
            throw new NotFoundException();
        }
        throw new BusinessException(HttpStatus.CONFLICT, "FILE_ALREADY_ATTACHED", "Tệp đã gắn vào một bản ghi");
    }

    /**
     * Nội dung tệp cho người xem. Tệp chưa gắn: chỉ người tải lên; tệp đã gắn: theo {@link FilePermissionProvider} của
     * loại chủ sở hữu, không có thì từ chối. Không lộ tệp tồn tại hay không cho người không có quyền ngoài 403.
     */
    public FileContent download(long fileId, long viewerAccountId) {
        FileStore.Row row = store.find(fileId).orElseThrow(NotFoundException::new);
        boolean allowed;
        if (row.attached()) {
            FilePermissionProvider provider = providers.stream()
                    .filter(p -> p.ownerType().equals(row.ownerType()))
                    .findFirst()
                    .orElse(null);
            allowed = provider != null && provider.canDownload(row.ownerId());
        } else {
            allowed = row.uploadedBy() == viewerAccountId;
        }
        if (!allowed) {
            throw new PermissionDeniedException("Không có quyền tải tệp này");
        }
        try {
            byte[] content = client().getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(row.uuid())
                            .build())
                    .asByteArray();
            return new FileContent(row.originalName(), row.mimeType(), content);
        } catch (SdkException e) {
            throw storageError();
        }
    }

    private S3Client client() {
        S3Client client = s3.getIfAvailable();
        if (client == null) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_UNAVAILABLE", "Chưa cấu hình nơi lưu tệp");
        }
        return client;
    }

    private static BusinessException storageError() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_ERROR", "Không truy cập được nơi lưu tệp");
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
