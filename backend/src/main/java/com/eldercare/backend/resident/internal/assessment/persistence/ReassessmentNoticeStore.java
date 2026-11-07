package com.eldercare.backend.resident.internal.assessment.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.resident.internal.assessment.domain.PeriodicReassessmentDue;
import com.eldercare.backend.resident.internal.assessment.domain.ReassessmentBasis;
import com.eldercare.backend.resident.internal.assessment.domain.ReassessmentNotice;
import com.eldercare.backend.resident.internal.assessment.domain.ReassessmentNoticeContent;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lần báo cần đánh giá lại ở {@code simple_record} loại {@code M01.REASSESSMENT_NOTICE} (001 T084; research H-11; thay
 * bảng kỹ thuật {@code reassessment_notice}): lần báo "chưa đáp ứng" là lần báo sau lần đánh giá Đã xác nhận gần nhất
 * (so theo {@code created_at}, lúc báo); khóa duy nhất {@code M01.RAN:…} chống báo trùng. Ghi qua {@link SimpleRecordApi}
 * trong transaction của lệnh gọi; chọn dòng chưa đáp ứng bằng SQL (so với bảng {@code assessment}).
 */
@Repository
public class ReassessmentNoticeStore {

    /** Nguồn của lần báo → loại đối tượng cha của dòng (data-model 001 mục 0.1: đối tượng là nguồn). */
    private static final Map<String, String> SOURCE_ENTITY_TYPES = Map.of(
            "ASSESSMENT", "M01.ASSESSMENT",
            "INCIDENT", "M05.INCIDENT",
            "STATUS_HISTORY", "M01.RESIDENT_STATUS_HISTORY",
            "CORRECTION", "M15.CORRECTION");

    private static final Map<String, String> SOURCE_TYPES =
            SOURCE_ENTITY_TYPES.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private static final String KIND = ReassessmentNoticeContent.KIND.code();

    private static final String LATEST_CONFIRMED =
            "COALESCE((SELECT MAX(a.confirmed_at) FROM assessment a WHERE a.resident_id = n.resident_id"
                    + " AND a.status = 'CONFIRMED'), '1000-01-01')";

    private final JdbcTemplate jdbc;
    private final SimpleRecordApi records;

    ReassessmentNoticeStore(JdbcTemplate jdbc, SimpleRecordApi records) {
        this.jdbc = jdbc;
        this.records = records;
    }

    /** Mọi lần báo chưa đáp ứng của hồ sơ tính hạn từ {@code from}, xóa dấu đã cảnh báo quá hạn. */
    public void restartDeadlines(long residentId, Instant from) {
        for (long id : unmetIds(residentId)) {
            change(id, content -> content.withDeadline(from));
        }
    }

    /** Hồ sơ ở bốn trạng thái của FR-039 có lần Đã xác nhận gần nhất không muộn hơn {@code before}. */
    public List<PeriodicReassessmentDue> periodicDue(Instant before) {
        return jdbc.query(
                "SELECT r.id, a.id FROM resident r JOIN assessment a ON a.id = (SELECT l.id FROM assessment l"
                        + " WHERE l.resident_id = r.id AND l.status = 'CONFIRMED' ORDER BY l.confirmed_at DESC,"
                        + " l.id DESC LIMIT 1) WHERE r.status IN ('IN_RESIDENCE', 'TEMPORARILY_ABSENT',"
                        + " 'OFF_SITE_ACTIVITY', 'HOSPITALIZED') AND a.confirmed_at <= ?",
                (rs, i) -> new PeriodicReassessmentDue(rs.getLong(1), rs.getLong(2)),
                Timestamp.from(before));
    }

    public void markOverdueAlerted(long noticeId, Instant at) {
        change(noticeId, content -> content.alerted(at));
    }

    /** Mã người cao tuổi đang có lần báo chưa đáp ứng. */
    public List<Long> residentsWithUnmetNotices() {
        return jdbc.query(
                "SELECT DISTINCT n.resident_id FROM simple_record n WHERE n.kind = ? AND n.created_at > "
                        + LATEST_CONFIRMED + " ORDER BY n.resident_id",
                (rs, i) -> rs.getLong(1),
                KIND);
    }

    /** Thêm lần báo; rỗng khi đã báo cùng căn cứ và nguồn. */
    public Optional<Long> insert(
            long residentId,
            ReassessmentBasis basis,
            String sourceType,
            long sourceId,
            Instant notifiedAt,
            Instant deadlineFrom) {
        String key = ReassessmentNoticeContent.uniqueKey(residentId, basis, sourceType, sourceId);
        if (records.findByUniqueKey(ReassessmentNoticeContent.KIND, key).isPresent()) {
            return Optional.empty();
        }
        SimpleRecord<ReassessmentNoticeContent> row = records.create(
                ReassessmentNoticeContent.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        new EntityRef(SOURCE_ENTITY_TYPES.get(sourceType), sourceId),
                        ReassessmentNoticeContent.STATUS,
                        new ReassessmentNoticeContent(basis, notifiedAt, deadlineFrom, null),
                        null,
                        null,
                        key,
                        0,
                        false));
        return Optional.of(row.id());
    }

    /** Các lần báo chưa đáp ứng của hồ sơ, sớm nhất trước. */
    public List<ReassessmentNotice> unmet(long residentId) {
        return unmetIds(residentId).stream()
                .map(id -> records.require(ReassessmentNoticeContent.KIND, id))
                .map(r -> new ReassessmentNotice(
                        r.id(),
                        r.content().basis(),
                        SOURCE_TYPES.get(r.parent().entityType()),
                        r.parent().entityId(),
                        r.content().notifiedAt(),
                        r.content().deadlineFrom(),
                        r.content().overdueAlertedAt()))
                .toList();
    }

    private List<Long> unmetIds(long residentId) {
        return jdbc.queryForList(
                "SELECT n.id FROM simple_record n WHERE n.kind = ? AND n.resident_id = ? AND n.created_at > "
                        + LATEST_CONFIRMED + " ORDER BY n.created_at, n.id",
                Long.class,
                KIND,
                residentId);
    }

    private void change(long id, java.util.function.UnaryOperator<ReassessmentNoticeContent> update) {
        SimpleRecord<ReassessmentNoticeContent> row = records.lockForUpdate(ReassessmentNoticeContent.KIND, id);
        records.change(
                ReassessmentNoticeContent.KIND,
                id,
                row.version(),
                new RecordChange<>(row.status(), update.apply(row.content()), null, row.uniqueKey(), false));
    }
}
