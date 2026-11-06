package com.eldercare.backend.administration.retention.internal.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.shared.api.RecordTypeAliases;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * Tệp đính kèm đi kèm bản ghi sở hữu (000 T112; FR-050, FR-051a): dòng {@code file} gắn {@code owner_type},
 * {@code owner_id} của bản ghi trong chuỗi. Tệp không có thời hạn riêng, sống theo bản ghi sở hữu, nên luôn là đã hết hạn
 * khi bản ghi hết hạn. Xóa nội dung S3 trước rồi dòng {@code file}, trong transaction của chuỗi: S3 lỗi thì cả chuỗi
 * rollback, đợt sau thử lại; dòng còn mà đối tượng đã mất chỉ xảy ra với bản ghi đã hết hạn và được xóa ở đợt sau.
 */
@Component
class AttachedFileRetention implements AttachedRetention {

    static final String TYPE = "M15.ATTACHED_FILE";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final RecordTypeAliases aliases;
    private final ObjectProvider<S3Client> s3;
    private final String bucket;

    AttachedFileRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            RecordTypeAliases aliases,
            ObjectProvider<S3Client> s3,
            @Value("${eldercare.file.bucket:${ELDERCARE_S3_BUCKET:}}") String bucket) {
        this.jdbc = jdbc;
        this.database = database;
        this.aliases = aliases;
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.NotResidentLinked();
    }

    @Override
    public List<Attached> attachedTo(DataRef record, RetentionKind recordKind, Instant cutoff) {
        var types = aliases.withLegacy(record.dataType());
        return jdbc.query(
                "SELECT id, attached_at FROM file WHERE owner_type IN (" + Ids.placeholders(types.size())
                        + ") AND owner_id = ?",
                (rs, i) -> new Attached(
                        rs.getLong("id"),
                        rs.getObject("attached_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        true),
                Stream.concat(types.stream(), Stream.of(record.id())).toArray());
    }

    /** Bản ghi nghiệp vụ trỏ tới tệp bằng khóa ngoại ({@code consent.evidence_file_id}…), nên tệp xóa sau. */
    @Override
    public boolean deletedAfterRecords() {
        return true;
    }

    @Override
    public void delete(Collection<Long> ids) {
        List<String> keys = database.jdbc()
                .queryForList("SELECT uuid FROM file WHERE id IN (" + Ids.inList(ids) + ")", String.class);
        S3Client client = s3.getIfAvailable();
        if (client == null && !keys.isEmpty()) {
            throw new IllegalStateException("Không có nơi lưu tệp để xóa nội dung tệp đính kèm");
        }
        for (String key : keys) {
            client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }
        database.jdbc().update("DELETE FROM file WHERE id IN (" + Ids.inList(ids) + ")");
    }
}
