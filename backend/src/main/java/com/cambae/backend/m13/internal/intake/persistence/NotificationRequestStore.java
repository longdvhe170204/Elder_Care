package com.cambae.backend.m13.internal.intake.persistence;

import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.Substitution;
import com.cambae.backend.m13.internal.intake.domain.IntakeResult;
import com.cambae.backend.m13.internal.intake.domain.RequestFlag;
import com.cambae.backend.m13.internal.intake.domain.ValidatedRequest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Đọc ghi yêu cầu thông báo và các phần của nó (data-model mục 3); bảng nhóm 3 chỉ thêm. */
@Repository
public class NotificationRequestStore {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public NotificationRequestStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Yêu cầu đã có cùng (bản ghi nguồn, khóa sự kiện) (FR-005). */
    public Optional<Long> findId(String sourceRecordType, String sourceRecordId, String eventKey) {
        return jdbc
                .queryForList(
                        "SELECT id FROM notification_request WHERE source_record_type = ? AND source_record_id = ?"
                                + " AND event_key = ?",
                        Long.class,
                        sourceRecordType,
                        sourceRecordId,
                        eventKey)
                .stream()
                .findFirst();
    }

    /**
     * Như {@link #findId} nhưng đọc bản đã commit mới nhất (đọc có khóa), dùng sau khi chèn trùng khóa vì ảnh chụp của
     * transaction có thể có trước lần chèn kia.
     */
    public Optional<Long> findIdLatest(String sourceRecordType, String sourceRecordId, String eventKey) {
        return jdbc
                .queryForList(
                        "SELECT id FROM notification_request WHERE source_record_type = ? AND source_record_id = ?"
                                + " AND event_key = ? FOR SHARE",
                        Long.class,
                        sourceRecordType,
                        sourceRecordId,
                        eventKey)
                .stream()
                .findFirst();
    }

    public void insertDuplicate(long requestId, Instant receivedAt) {
        jdbc.update(
                "INSERT INTO notification_request_duplicate (request_id, received_at) VALUES (?, ?)",
                requestId,
                utc(receivedAt));
    }

    /** Lưu yêu cầu đã kiểm tra kèm phần nội dung và nhóm người nhận; trả id. */
    public long insert(ValidatedRequest v, Instant receivedAt) {
        NotificationRequest r = v.request();
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO notification_request (source_module, event_type, source_group,"
                                    + " source_record_type, source_record_id, event_key, level, resident_id,"
                                    + " floor_area_id, short_content, title, caused_by, notify_causer, occurred_at,"
                                    + " received_at, intake_result, rejection_reason, flags, information_subject,"
                                    + " q354_case) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, r.sourceModule());
                    ps.setString(2, r.eventType());
                    ps.setString(
                            3, r.sourceGroup() == null ? null : r.sourceGroup().name());
                    ps.setString(
                            4,
                            r.sourceRecord() == null ? null : r.sourceRecord().type());
                    ps.setString(
                            5,
                            r.sourceRecord() == null ? null : r.sourceRecord().id());
                    ps.setString(6, r.eventKey());
                    ps.setString(7, v.level().name());
                    ps.setObject(8, opt(r.residentId()).orElse(null));
                    ps.setObject(9, opt(r.floorAreaId()).orElse(null));
                    ps.setString(10, opt(r.shortContent()).orElse(null));
                    ps.setString(11, opt(r.title()).orElse(null));
                    ps.setObject(12, opt(r.causedBy()).orElse(null));
                    ps.setBoolean(13, r.notifyCauser());
                    ps.setObject(14, r.occurredAt() == null ? null : utc(r.occurredAt()));
                    ps.setObject(15, utc(receivedAt));
                    ps.setString(16, v.result().name());
                    ps.setString(17, v.rejectionReason().orElse(null));
                    ps.setString(18, v.flags().stream().map(RequestFlag::name).collect(Collectors.joining(",")));
                    ps.setString(19, v.informationSubject().map(Enum::name).orElse(null));
                    ps.setString(20, opt(r.q354Case()).map(Enum::name).orElse(null));
                    return ps;
                },
                key);
        long id = key.getKey().longValue();

        List<ContentPart> content = v.content();
        for (int i = 0; i < content.size(); i++) {
            jdbc.update(
                    "INSERT INTO notification_request_content (request_id, seq, information_type, content)"
                            + " VALUES (?, ?, ?, ?)",
                    id,
                    i + 1,
                    content.get(i).type().name(),
                    content.get(i).content());
        }
        List<RecipientGroup> groups = r.recipientGroups() == null ? List.of() : r.recipientGroups();
        for (int i = 0; i < groups.size(); i++) {
            RecipientGroup g = groups.get(i);
            Substitution s = g instanceof RecipientGroup.DutyGroup d ? d.substitution() : null;
            jdbc.update(
                    "INSERT INTO notification_request_recipient_group (request_id, seq, group_code, params,"
                            + " substitution, substitute_group) VALUES (?, ?, ?, ?, ?, ?)",
                    id,
                    i + 1,
                    g.getClass().getSimpleName(),
                    json.writeValueAsString(g),
                    substitution(s),
                    s instanceof Substitution.Designated des ? groupJson(des.substitute()) : null);
        }
        return id;
    }

    /** Sự kiện nguồn không dựng được yêu cầu (FR-006): lưu một yêu cầu bị từ chối để hiện ở danh sách lỗi gửi. */
    public long insertBuildFailure(String eventType, Instant receivedAt) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO notification_request (event_type, level, received_at, intake_result,"
                                    + " rejection_reason) VALUES (?, 'LOW', ?, ?, 'REQUEST_BUILD_FAILED')",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, eventType);
                    ps.setObject(2, utc(receivedAt));
                    ps.setString(3, IntakeResult.REJECTED.name());
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private String groupJson(RecipientGroup g) {
        return json.writeValueAsString(Map.of("code", g.getClass().getSimpleName(), "params", g));
    }

    private static String substitution(Substitution s) {
        return switch (s) {
            case null -> null;
            case Substitution.Default d -> "DEFAULT";
            case Substitution.NoSubstitute n -> "NO_SUBSTITUTE";
            case Substitution.Designated d -> "DESIGNATED";
        };
    }

    private static <T> Optional<T> opt(Optional<T> o) {
        return o == null ? Optional.empty() : o;
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
