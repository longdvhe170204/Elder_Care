package com.cambae.backend.m01.internal.assessment.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.ReassessmentState;
import com.cambae.backend.m01.api.dto.ReassessmentStatus;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.ReassessmentDue;
import com.cambae.backend.m01.api.events.ReassessmentOverdue;
import com.cambae.backend.m01.internal.assessment.domain.ReassessmentBasis;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo bác sĩ cần đánh giá lại (001 T063; FR-039 → FR-043, BR-M01-02; research H-11). Không có bản ghi yêu cầu (Q-307):
 * mỗi lần báo là một dòng của bảng kỹ thuật {@code reassessment_notice} kèm sự kiện {@link ReassessmentDue}; "cần đánh
 * giá lại" và "quá hạn" là trạng thái tính từ các lần báo sau lần đánh giá Đã xác nhận gần nhất. Hạn là lần báo sớm nhất
 * chưa đáp ứng cộng CFG-M01-03; người đang Điều trị tại bệnh viện chưa tính hạn tới khi Ghi nhận trở về. Hồ sơ ở trạng
 * thái cuối không được báo và trả "không áp dụng" (FR-043); job định kỳ chỉ xét bốn trạng thái của FR-039.
 */
@Service
public class ReassessmentNoticeService {

    public static final String PERIODIC_JOB = "NOTIFY_PERIODIC_REASSESSMENT";
    public static final String OVERDUE_JOB = "ALERT_OVERDUE_REASSESSMENT";

    static final String SOURCE_ASSESSMENT = "ASSESSMENT";
    static final String SOURCE_INCIDENT = "INCIDENT";
    static final String SOURCE_STATUS_HISTORY = "STATUS_HISTORY";
    static final String SOURCE_CORRECTION = "CORRECTION";

    private static final Set<ResidentStatus> FINAL =
            EnumSet.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private static final String LATEST_CONFIRMED =
            "COALESCE((SELECT MAX(a.confirmed_at) FROM assessment a WHERE a.resident_id = n.resident_id"
                    + " AND a.status = 'CONFIRMED'), '1000-01-01')";

    /** Một lần báo chưa được đáp ứng. */
    public record Notice(
            long id,
            ReassessmentBasis basis,
            String sourceType,
            long sourceId,
            Instant notifiedAt,
            Instant deadlineFrom,
            Instant overdueAlertedAt) {}

    /** Trạng thái kèm các căn cứ đã báo (GET /residents/{id}/reassessment). */
    public record Detail(ReassessmentStatus status, List<Notice> basis) {}

    private final JdbcTemplate jdbc;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    ReassessmentNoticeService(
            JdbcTemplate jdbc, ParameterApi parameters, ApplicationEventPublisher events, BusinessTime time) {
        this.jdbc = jdbc;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Báo sau sự cố ngã hoặc mức Trung bình trở lên (FR-040); gọi lại cùng sự cố không báo thêm. */
    @Transactional
    public boolean notifyAfterIncident(long residentId, long incidentId) {
        return notice(residentId, ReassessmentBasis.AFTER_INCIDENT, SOURCE_INCIDENT, incidentId);
    }

    /** Báo vì đính chính đổi mức của lần Đã xác nhận gần nhất (FR-032; research H-10). */
    @Transactional
    public boolean notifyAfterCorrection(long residentId, long correctionId) {
        return notice(residentId, ReassessmentBasis.ASSESSMENT_CORRECTION, SOURCE_CORRECTION, correctionId);
    }

    /**
     * Tác động của lệnh Ghi nhận trở về từ Điều trị tại bệnh viện (FR-041; T048): mọi lần báo chưa đáp ứng tính hạn từ
     * lúc trở về, rồi thêm lần báo căn cứ trở về từ bệnh viện. Chạy trong transaction của lệnh trạng thái.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void onReturnFromHospital(long residentId, long historyId, Instant at) {
        jdbc.update(
                "UPDATE reassessment_notice n SET n.deadline_from = ?, n.overdue_alerted_at = NULL"
                        + " WHERE n.resident_id = ? AND n.notified_at > " + LATEST_CONFIRMED,
                Timestamp.from(at),
                residentId);
        insert(residentId, ReassessmentBasis.RETURN_FROM_HOSPITAL, SOURCE_STATUS_HISTORY, historyId, at, at);
    }

    /** Báo định kỳ: số lần báo mới trong lần chạy này (FR-039). */
    @Transactional
    public int runPeriodic() {
        Duration validity = parameters.read(ParameterCode.CFG_M01_02).asDuration();
        Instant now = time.now();
        List<long[]> due = jdbc.query(
                "SELECT r.id, a.id FROM resident r JOIN assessment a ON a.id = (SELECT l.id FROM assessment l"
                        + " WHERE l.resident_id = r.id AND l.status = 'CONFIRMED' ORDER BY l.confirmed_at DESC,"
                        + " l.id DESC LIMIT 1) WHERE r.status IN ('IN_RESIDENCE', 'TEMPORARILY_ABSENT',"
                        + " 'OFF_SITE_ACTIVITY', 'HOSPITALIZED') AND a.confirmed_at <= ?",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)},
                Timestamp.from(now.minus(validity)));
        int notified = 0;
        for (long[] row : due) {
            if (notice(row[0], ReassessmentBasis.PERIODIC, SOURCE_ASSESSMENT, row[1])) {
                notified++;
            }
        }
        return notified;
    }

    /** Cảnh báo quá hạn: một lần mỗi người mỗi lần quá hạn (FR-041). Số cảnh báo phát trong lần chạy này. */
    @Transactional
    public int runOverdue() {
        Instant now = time.now();
        int alerted = 0;
        for (long residentId : residentsWithUnmetNotices()) {
            Detail detail = detail(residentId);
            if (detail.status().state() != ReassessmentState.OVERDUE
                    || detail.basis().stream().anyMatch(n -> n.overdueAlertedAt() != null)) {
                continue;
            }
            for (Notice n : detail.basis()) {
                jdbc.update(
                        "UPDATE reassessment_notice SET overdue_alerted_at = ? WHERE id = ?",
                        Timestamp.from(now),
                        n.id());
            }
            events.publishEvent(new ReassessmentOverdue(
                    residentId, detail.status().overdueSince().orElseThrow()));
            alerted++;
        }
        return alerted;
    }

    @Transactional(readOnly = true)
    public Map<Long, ReassessmentStatus> status(Collection<Long> residentIds) {
        Map<Long, ReassessmentStatus> result = new LinkedHashMap<>();
        for (long id : residentIds) {
            result.put(id, detail(id).status());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Detail detail(long residentId) {
        Optional<ResidentStatus> status = residentStatus(residentId);
        if (status.isEmpty() || FINAL.contains(status.get())) {
            return new Detail(
                    new ReassessmentStatus(ReassessmentState.NOT_APPLICABLE, Optional.empty(), Optional.empty()),
                    List.of());
        }
        List<Notice> unmet = unmet(residentId);
        if (unmet.isEmpty()) {
            return new Detail(
                    new ReassessmentStatus(ReassessmentState.NONE, Optional.empty(), Optional.empty()), List.of());
        }
        if (status.get() == ResidentStatus.HOSPITALIZED) {
            return new Detail(new ReassessmentStatus(ReassessmentState.DUE, Optional.empty(), Optional.empty()), unmet);
        }
        Duration deadline = parameters.read(ParameterCode.CFG_M01_03).asDuration();
        Optional<Instant> dueAt = unmet.stream()
                .map(Notice::deadlineFrom)
                .filter(java.util.Objects::nonNull)
                .min(Instant::compareTo)
                .map(from -> from.plus(deadline));
        boolean overdue = dueAt.map(d -> time.now().isAfter(d)).orElse(false);
        ReassessmentStatus result = overdue
                ? new ReassessmentStatus(ReassessmentState.OVERDUE, dueAt, dueAt)
                : new ReassessmentStatus(ReassessmentState.DUE, dueAt, Optional.empty());
        return new Detail(result, unmet);
    }

    /** Mã người cao tuổi đang có lần báo chưa đáp ứng (nguồn của danh sách "cần thực hiện" và của job quá hạn). */
    @Transactional(readOnly = true)
    public List<Long> residentsWithUnmetNotices() {
        return jdbc.query(
                "SELECT DISTINCT n.resident_id FROM reassessment_notice n WHERE n.notified_at > " + LATEST_CONFIRMED
                        + " ORDER BY n.resident_id",
                (rs, i) -> rs.getLong(1));
    }

    private boolean notice(long residentId, ReassessmentBasis basis, String sourceType, long sourceId) {
        ResidentStatus status = residentStatus(residentId).orElse(null);
        if (status == null || FINAL.contains(status)) {
            return false;
        }
        Instant now = time.now();
        return insert(residentId, basis, sourceType, sourceId, now, status == ResidentStatus.HOSPITALIZED ? null : now);
    }

    private boolean insert(
            long residentId,
            ReassessmentBasis basis,
            String sourceType,
            long sourceId,
            Instant notifiedAt,
            Instant deadlineFrom) {
        int inserted = jdbc.update(
                "INSERT IGNORE INTO reassessment_notice (resident_id, basis, source_type, source_id, notified_at,"
                        + " deadline_from) VALUES (?, ?, ?, ?, ?, ?)",
                residentId,
                basis.name(),
                sourceType,
                sourceId,
                Timestamp.from(notifiedAt),
                deadlineFrom == null ? null : Timestamp.from(deadlineFrom));
        if (inserted == 0) {
            return false;
        }
        long noticeId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        events.publishEvent(new ReassessmentDue(residentId, noticeId, basis.name()));
        return true;
    }

    private List<Notice> unmet(long residentId) {
        return new ArrayList<>(jdbc.query(
                "SELECT n.id, n.basis, n.source_type, n.source_id, n.notified_at, n.deadline_from,"
                        + " n.overdue_alerted_at FROM reassessment_notice n WHERE n.resident_id = ? AND n.notified_at > "
                        + LATEST_CONFIRMED + " ORDER BY n.notified_at, n.id",
                (rs, i) -> new Notice(
                        rs.getLong(1),
                        ReassessmentBasis.valueOf(rs.getString(2)),
                        rs.getString(3),
                        rs.getLong(4),
                        rs.getTimestamp(5).toInstant(),
                        instant(rs.getTimestamp(6)),
                        instant(rs.getTimestamp(7))),
                residentId));
    }

    private Optional<ResidentStatus> residentStatus(long residentId) {
        return jdbc
                .query(
                        "SELECT status FROM resident WHERE id = ?",
                        (rs, i) -> ResidentStatus.valueOf(rs.getString(1)),
                        residentId)
                .stream()
                .findFirst();
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
