package com.eldercare.backend.common.file;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Siêu dữ liệu tệp ở bảng {@code file} (001 research H-13); nội dung nằm trong S3. */
@Repository
class FileStore {

    /** Một dòng {@code file}; {@code ownerType} rỗng khi chưa gắn. */
    record Row(
            long id,
            String uuid,
            String originalName,
            String mimeType,
            long uploadedBy,
            String ownerType,
            Long ownerId) {

        boolean attached() {
            return ownerType != null;
        }
    }

    private final JdbcTemplate jdbc;

    FileStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    long insert(
            String uuid,
            String originalName,
            String mimeType,
            long sizeBytes,
            String sha256,
            long uploadedBy,
            Instant uploadedAt) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO file (uuid, original_name, mime_type, size_bytes, sha256, uploaded_by,"
                                    + " uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            java.sql.Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, uuid);
                    statement.setString(2, originalName);
                    statement.setString(3, mimeType);
                    statement.setLong(4, sizeBytes);
                    statement.setString(5, sha256);
                    statement.setLong(6, uploadedBy);
                    statement.setObject(7, LocalDateTime.ofInstant(uploadedAt, ZoneOffset.UTC));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    Optional<Row> find(long id) {
        return jdbc
                .query(
                        "SELECT id, uuid, original_name, mime_type, uploaded_by, owner_type, owner_id FROM file"
                                + " WHERE id = ?",
                        (rs, i) -> new Row(
                                rs.getLong("id"),
                                rs.getString("uuid"),
                                rs.getString("original_name"),
                                rs.getString("mime_type"),
                                rs.getLong("uploaded_by"),
                                rs.getString("owner_type"),
                                rs.getObject("owner_id", Long.class)),
                        id)
                .stream()
                .findFirst();
    }

    /** Gắn nếu chưa gắn; trả số dòng đổi (0 khi tệp không có hoặc đã gắn). */
    int attach(long id, String ownerType, long ownerId, Instant at) {
        return jdbc.update(
                "UPDATE file SET owner_type = ?, owner_id = ?, attached_at = ? WHERE id = ? AND attached_at IS NULL",
                ownerType,
                ownerId,
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                id);
    }
}
