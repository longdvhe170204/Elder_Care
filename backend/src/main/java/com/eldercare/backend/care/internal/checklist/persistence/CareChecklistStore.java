package com.eldercare.backend.care.internal.checklist.persistence;

import com.eldercare.backend.care.internal.careplan.domain.RiskDecisionMaker;
import com.eldercare.backend.care.internal.checklist.domain.ChecklistRiskAgreement;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.SelfDeterminationAssessment;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** FR-030: một truy vấn thỏa thuận của tất cả người trong checklist, không N+1 theo người. */
@Repository
public class CareChecklistStore {
    private final NamedParameterJdbcTemplate jdbc;

    public CareChecklistStore(JdbcTemplate jdbc) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public List<ChecklistRiskAgreement> agreements(Collection<Long> residents, LocalDate date) {
        if (residents.isEmpty()) return List.of();
        return jdbc.query(
                "SELECT i.*,p.resident_id FROM care_plan_item i JOIN care_plan p ON p.id=i.care_plan_id WHERE p.resident_id IN(:residents) AND p.status='EFFECTIVE' AND p.effective_from<=:date AND (p.effective_to IS NULL OR p.effective_to>=:date) AND i.kind='RISK_AGREEMENT' AND i.active ORDER BY p.resident_id,i.id",
                Map.of("residents", residents, "date", date),
                (r, n) -> new ChecklistRiskAgreement(
                        r.getLong("id"),
                        r.getLong("resident_id"),
                        r.getString("choice"),
                        r.getString("discussed_risks"),
                        r.getBoolean("resident_participates"),
                        r.getLong("assessing_doctor_id"),
                        new SelfDeterminationAssessment(
                                r.getBoolean("can_self_determine"),
                                r.getString("support_method"),
                                r.getObject("assessment_review_date", LocalDate.class)),
                        r.getObject("assessment_at", LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant(),
                        new RiskDecisionMaker(
                                r.getString("decision_maker_type"),
                                r.getObject("decision_maker_family_id", Long.class),
                                r.getObject("decision_maker_doctor_id", Long.class),
                                r.getString("doctor_decision_reason")),
                        r.getObject("review_date", LocalDate.class)));
    }
}
