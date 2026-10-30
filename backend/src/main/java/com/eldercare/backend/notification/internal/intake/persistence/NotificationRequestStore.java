package com.eldercare.backend.notification.internal.intake.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.notification.internal.intake.domain.IntakeResult;
import com.eldercare.backend.notification.internal.intake.domain.RequestFlag;
import com.eldercare.backend.notification.internal.intake.domain.ValidatedRequest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc ghi yêu cầu thông báo và các phần của nó (data-model mục 0.1, 3); bảng nhóm 3 chỉ thêm: phần nội dung, nhóm người
 * nhận ghi cùng câu thêm vào cột {@code contents}, {@code recipient_groups}; lần nhận trùng ở {@code reminder_marker} loại
 * {@code M13.REQUEST_DUPLICATE} (009 T043).
 */
@Repository
public class NotificationRequestStore {

    static final String DUPLICATE = "M13.REQUEST_DUPLICATE";
    static final String REQUEST = "M13.NOTIFICATION_REQUEST";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final ReminderMarkers markers;

    public NotificationRequestStore(JdbcTemplate jdbc, JsonMapper json, ReminderMarkers markers) {
        this.jdbc = jdbc;
        this.json = json;
        this.markers = markers;
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
        markers.mark(DUPLICATE, REQUEST, requestId, receivedAt.toString(), null);
    }

    /** Lưu yêu cầu đã kiểm tra kèm phần nội dung và nhóm người nhận; trả id. */
    public long insert(ValidatedRequest v, Instant receivedAt) {
        return insert(v, List.of(), receivedAt);
    }

    /**
     * Lưu yêu cầu kèm kết quả xác định người nhận của từng nhóm theo thứ tự (FR-010); bảng chỉ cho thêm nên kết quả ghi
     * cùng lần thêm.
     */
    public long insert(ValidatedRequest v, List<String> groupOutcomes, Instant receivedAt) {
        NotificationRequest r = v.request();
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO notification_request (source_module, event_type, source_group,"
                                    + " source_record_type, source_record_id, event_key, level, resident_id,"
                                    + " floor_area_id, short_content, title, caused_by, notify_causer, occurred_at,"
                                    + " received_at, intake_result, rejection_reason, flags, information_subject,"
                                    + " q354_case, contents, recipient_groups)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                    ps.setString(21, json.writeValueAsString(contents(v.content())));
                    ps.setString(22, json.writeValueAsString(groups(r.recipientGroups(), groupOutcomes)));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Phần nội dung theo thứ tự: {@code {seq, informationType, content}} (FR-013). */
    private static List<Map<String, Object>> contents(List<ContentPart> content) {
        List<Map<String, Object>> parts = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            Map<String, Object> part = new LinkedHashMap<>();
            part.put("seq", i + 1);
            part.put("informationType", content.get(i).type().name());
            part.put("content", content.get(i).content());
            parts.add(part);
        }
        return parts;
    }

    /** Nhóm người nhận theo thứ tự kèm kết quả xác định người nhận (research B-04). */
    private List<Map<String, Object>> groups(List<RecipientGroup> recipientGroups, List<String> outcomes) {
        List<RecipientGroup> groups = recipientGroups == null ? List.of() : recipientGroups;
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            RecipientGroup g = groups.get(i);
            Substitution s = g instanceof RecipientGroup.DutyGroup d ? d.substitution() : null;
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("seq", i + 1);
            group.put("groupCode", g.getClass().getSimpleName());
            group.put("params", json.valueToTree(g));
            group.put("substitution", substitution(s));
            group.put("substituteGroup", s instanceof Substitution.Designated des ? groupJson(des.substitute()) : null);
            group.put("outcome", i < outcomes.size() ? outcomes.get(i) : null);
            out.add(group);
        }
        return out;
    }

    /** Sự kiện nguồn không dựng được yêu cầu (FR-006): lưu một yêu cầu bị từ chối để hiện ở danh sách lỗi gửi. */
    public long insertBuildFailure(String eventType, Instant receivedAt) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO notification_request (event_type, level, received_at, intake_result,"
                                    + " rejection_reason, contents, recipient_groups)"
                                    + " VALUES (?, 'LOW', ?, ?, 'REQUEST_BUILD_FAILED', JSON_ARRAY(), JSON_ARRAY())",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, eventType);
                    ps.setObject(2, utc(receivedAt));
                    ps.setString(3, IntakeResult.REJECTED.name());
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private Object groupJson(RecipientGroup g) {
        return json.valueToTree(Map.of("code", g.getClass().getSimpleName(), "params", g));
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
