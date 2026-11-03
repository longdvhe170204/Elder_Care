package com.eldercare.backend.care.internal.careplan.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.SelfDeterminationAssessment;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** R-15: SQL chỉ trên care_plan_item; mọi con đi qua RecordEventApi. */
@Repository
public class CareRiskStore {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final RecordEventApi events;

    public CareRiskStore(JdbcTemplate jdbc, RecordEventApi events) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.events = events;
    }

    private EntityRef parent(long id) {
        return new EntityRef("M04.RISK_AGREEMENT", id);
    }

    private Instant instant(ResultSet rs, String field) throws SQLException {
        var value = rs.getObject(field, LocalDateTime.class);
        return value == null ? null : value.atZone(BusinessTime.ZONE).toInstant();
    }

    private LocalDateTime local(Instant at) {
        return LocalDateTime.ofInstant(at, BusinessTime.ZONE);
    }

    public List<CareRiskAgreement> list(long planId) {
        return jdbc.query(
                "SELECT * FROM care_plan_item WHERE care_plan_id=? AND kind='RISK_AGREEMENT' AND active ORDER BY id",
                this::row,
                planId);
    }

    private CareRiskAgreement row(ResultSet rs, int index) throws SQLException {
        long id = rs.getLong("id");
        var participants = events.list(CareRiskKinds.PARTICIPANTS, parent(id)).stream()
                .map(RecordEvent::content)
                .filter(RiskParticipant::active)
                .toList();
        var opinions = events.list(CareRiskKinds.OPINIONS, parent(id)).stream()
                .filter(e -> e.content().active())
                .map(e -> new RiskOpinionEntry(e.content(), e.actorId(), e.occurredAt()))
                .toList();
        return new CareRiskAgreement(
                id,
                rs.getLong("care_plan_id"),
                rs.getObject("source_item_id", Long.class),
                rs.getString("choice"),
                rs.getString("discussed_risks"),
                rs.getBoolean("resident_participates"),
                participants,
                rs.getLong("assessing_doctor_id"),
                new SelfDeterminationAssessment(
                        rs.getBoolean("can_self_determine"),
                        rs.getString("support_method"),
                        rs.getObject("assessment_review_date", LocalDate.class)),
                instant(rs, "assessment_at"),
                new RiskDecisionMaker(
                        rs.getString("decision_maker_type"),
                        rs.getObject("decision_maker_family_id", Long.class),
                        rs.getObject("decision_maker_doctor_id", Long.class),
                        rs.getString("doctor_decision_reason")),
                opinions,
                rs.getObject("review_date", LocalDate.class),
                rs.getObject("recorded_by", Long.class),
                instant(rs, "recorded_at"));
    }

    private void deactivateChildren(long id) {
        events.list(CareRiskKinds.PARTICIPANTS, parent(id)).stream()
                .filter(e -> e.content().active())
                .forEach(e -> events.updateDraft(
                        CareRiskKinds.PARTICIPANTS,
                        e.id(),
                        new RiskParticipant(e.content().type(), e.content().id(), false)));
        events.list(CareRiskKinds.OPINIONS, parent(id)).stream()
                .filter(e -> e.content().active())
                .forEach(e -> events.updateDraft(
                        CareRiskKinds.OPINIONS,
                        e.id(),
                        new RiskOpinion(e.content().source(), e.content().content(), false)));
    }

    public void save(long planId, List<CareRiskAgreement> rows) {
        list(planId).forEach(row -> deactivateChildren(row.id()));
        jdbc.update(
                "UPDATE care_plan_item SET active=FALSE WHERE care_plan_id=? AND kind='RISK_AGREEMENT' AND active",
                planId);
        for (var row : rows) {
            var assessment = row.selfDeterminationAssessment();
            var maker = row.decisionMaker();
            var p = new MapSqlParameterSource()
                    .addValue("plan", planId)
                    .addValue("source", row.sourceAgreementId())
                    .addValue("choice", row.choice())
                    .addValue("risks", row.discussedRisks())
                    .addValue("participates", row.residentParticipates())
                    .addValue("assessor", row.assessingDoctorId())
                    .addValue("self", assessment.canSelfDetermine())
                    .addValue("support", assessment.supportMethod())
                    .addValue("assessed", local(row.assessedAt()))
                    .addValue("assessmentReview", assessment.reviewDate())
                    .addValue("makerType", maker.type())
                    .addValue("family", maker.familyMemberId())
                    .addValue("doctor", maker.doctorId())
                    .addValue("reason", maker.reason())
                    .addValue("review", row.reviewDate())
                    .addValue("actor", row.recordedBy())
                    .addValue("at", local(row.recordedAt()))
                    .addValue("id", row.id());
            long id = row.id();
            if (id == 0) {
                var key = new GeneratedKeyHolder();
                named.update(
                        "INSERT INTO care_plan_item(care_plan_id,kind,data,source_item_id,choice,discussed_risks,resident_participates,assessing_doctor_id,can_self_determine,support_method,assessment_at,assessment_review_date,decision_maker_type,decision_maker_family_id,decision_maker_doctor_id,doctor_decision_reason,review_date,recorded_by,recorded_at) VALUES(:plan,'RISK_AGREEMENT','{}',:source,:choice,:risks,:participates,:assessor,:self,:support,:assessed,:assessmentReview,:makerType,:family,:doctor,:reason,:review,:actor,:at)",
                        p,
                        key,
                        new String[] {"id"});
                id = Objects.requireNonNull(key.getKey()).longValue();
            } else if (named.update(
                            "UPDATE care_plan_item SET active=TRUE,choice=:choice,discussed_risks=:risks,resident_participates=:participates,assessing_doctor_id=:assessor,can_self_determine=:self,support_method=:support,assessment_at=:assessed,assessment_review_date=:assessmentReview,decision_maker_type=:makerType,decision_maker_family_id=:family,decision_maker_doctor_id=:doctor,doctor_decision_reason=:reason,review_date=:review,recorded_by=:actor,recorded_at=:at WHERE id=:id AND care_plan_id=:plan AND kind='RISK_AGREEMENT'",
                            p)
                    != 1) throw new NotFoundException();
            for (var participant : row.participants())
                events.add(
                        CareRiskKinds.PARTICIPANTS,
                        parent(id),
                        new NewRecordEvent<>(
                                participant, null, row.recordedBy(), false, row.recordedAt(), null, null, null, null));
            for (var opinion : row.opinions())
                events.add(
                        CareRiskKinds.OPINIONS,
                        parent(id),
                        new NewRecordEvent<>(
                                opinion.opinion(),
                                null,
                                opinion.recordedBy(),
                                false,
                                opinion.at(),
                                null,
                                null,
                                null,
                                null));
        }
    }

    public void copy(long from, long to) {
        save(
                to,
                list(from).stream()
                        .map(r -> new CareRiskAgreement(
                                0,
                                to,
                                r.lineage(),
                                r.choice(),
                                r.discussedRisks(),
                                r.residentParticipates(),
                                r.participants(),
                                r.assessingDoctorId(),
                                r.selfDeterminationAssessment(),
                                r.assessedAt(),
                                r.decisionMaker(),
                                r.opinions(),
                                r.reviewDate(),
                                r.recordedBy(),
                                r.recordedAt()))
                        .toList());
    }

    public List<LocalDate> reviewDates(LocalDate today) {
        return jdbc.queryForList(
                "SELECT i.review_date date FROM care_plan_item i JOIN care_plan p ON p.id=i.care_plan_id WHERE i.kind='RISK_AGREEMENT' AND i.active AND p.status='EFFECTIVE' AND i.review_date BETWEEN p.effective_from AND ? UNION SELECT i.assessment_review_date date FROM care_plan_item i JOIN care_plan p ON p.id=i.care_plan_id WHERE i.kind='RISK_AGREEMENT' AND i.active AND p.status='EFFECTIVE' AND i.assessment_review_date BETWEEN p.effective_from AND ? ORDER BY date",
                LocalDate.class,
                today,
                today);
    }

    public List<Long> plansForReview(LocalDate date) {
        return jdbc.queryForList(
                "SELECT DISTINCT p.id FROM care_plan p JOIN care_plan_item i ON i.care_plan_id=p.id WHERE p.status='EFFECTIVE' AND p.effective_from<=? AND i.kind='RISK_AGREEMENT' AND i.active AND (i.review_date=? OR i.assessment_review_date=?) ORDER BY p.id",
                Long.class,
                date,
                date,
                date);
    }
}
