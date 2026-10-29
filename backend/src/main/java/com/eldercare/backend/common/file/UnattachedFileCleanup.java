package com.eldercare.backend.common.file;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * Dọn tệp tải lên mà sau 24 giờ chưa gắn vào bản ghi nào (001 T004; research H-13): xóa đối tượng S3 rồi dòng
 * {@code file}. Đây là dữ liệu tạm, không phải bản ghi nghiệp vụ (000 FR-010a). S3 xóa lỗi thì giữ dòng để lần chạy sau
 * thử lại, nên không có đối tượng mồ côi mà không có dòng trỏ tới.
 */
@Service
public class UnattachedFileCleanup {

    public static final String JOB = "CLEAN_UNATTACHED_FILES";
    private static final Duration KEEP_UNATTACHED_FOR = Duration.ofHours(24);

    private final JdbcTemplate jdbc;
    private final ObjectProvider<S3Client> s3;
    private final String bucket;
    private final BusinessTime time;

    UnattachedFileCleanup(
            JdbcTemplate jdbc,
            ObjectProvider<S3Client> s3,
            @Value("${eldercare.file.bucket:${ELDERCARE_S3_BUCKET:}}") String bucket,
            BusinessTime time) {
        this.jdbc = jdbc;
        this.s3 = s3;
        this.bucket = bucket;
        this.time = time;
    }

    /** Số tệp đã dọn trong lần chạy này. */
    public int run() {
        S3Client client = s3.getIfAvailable();
        if (client == null) {
            return 0;
        }
        Instant cutoff = time.now().minus(KEEP_UNATTACHED_FOR);
        List<Object[]> stale = jdbc.query(
                "SELECT id, uuid FROM file WHERE attached_at IS NULL AND uploaded_at < ?",
                (rs, i) -> new Object[] {rs.getLong(1), rs.getString(2)},
                LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC));
        int removed = 0;
        for (Object[] file : stale) {
            try {
                client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key((String) file[1])
                        .build());
            } catch (SdkException e) {
                continue;
            }
            // chỉ xóa khi vẫn chưa gắn: lệnh nghiệp vụ có thể vừa gắn tệp giữa hai câu lệnh
            removed += jdbc.update("DELETE FROM file WHERE id = ? AND attached_at IS NULL", file[0]);
        }
        return removed;
    }
}
