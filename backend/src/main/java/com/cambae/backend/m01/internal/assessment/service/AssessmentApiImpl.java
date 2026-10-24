package com.cambae.backend.m01.internal.assessment.service;

import com.cambae.backend.m01.api.AssessmentApi;
import com.cambae.backend.m01.api.dto.AssessmentSummary;
import com.cambae.backend.m01.api.dto.CareNeedView;
import com.cambae.backend.m01.internal.assessment.domain.Assessment;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentStatus;
import com.cambae.backend.m01.internal.assessment.persistence.AssessmentRepository;
import com.cambae.backend.m01.internal.assessment.service.AssessmentContents.Effective;
import com.cambae.backend.m01.internal.health.persistence.CareNeedRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AssessmentApi} cho 004, 005, 007: lần Đã xác nhận gần nhất, nội dung theo bản đính chính (FR-038). Lần mới hơn
 * để trống lưu ý thì trả rỗng, không lùi về lần cũ hơn.
 */
@Service
class AssessmentApiImpl implements AssessmentApi {

    private final AssessmentRepository assessments;
    private final AssessmentContents contents;
    private final CareNeedRepository careNeeds;

    AssessmentApiImpl(AssessmentRepository assessments, AssessmentContents contents, CareNeedRepository careNeeds) {
        this.assessments = assessments;
        this.contents = contents;
        this.careNeeds = careNeeds;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Optional<String>> currentCareNote(Collection<Long> residentIds) {
        Map<Long, Assessment> latest = latestByResident(residentIds);
        Map<Long, Effective> effective = contents.of(List.copyOf(latest.values()));
        Map<Long, Optional<String>> result = new LinkedHashMap<>();
        for (Long residentId : residentIds) {
            Assessment a = latest.get(residentId);
            result.put(
                    residentId,
                    a == null
                            ? Optional.empty()
                            : Optional.ofNullable(effective.get(a.getId()).careNote()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssessmentSummary> latestConfirmed(long residentId) {
        return assessments
                .findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(residentId, AssessmentStatus.CONFIRMED)
                .map(a -> new AssessmentSummary(
                        a.getId(), residentId, contents.of(a).careLevel(), needViews(a), a.getConfirmedAt()));
    }

    private Map<Long, Assessment> latestByResident(Collection<Long> residentIds) {
        Map<Long, Assessment> latest = new HashMap<>();
        if (residentIds.isEmpty()) {
            return latest;
        }
        // Đã sắp theo thời điểm xác nhận giảm dần: dòng đầu của mỗi người là lần gần nhất
        for (Assessment a : assessments.findByResidentIdInAndStatus(residentIds, AssessmentStatus.CONFIRMED)) {
            latest.putIfAbsent(a.getResidentId(), a);
        }
        return latest;
    }

    private Set<CareNeedView> needViews(Assessment a) {
        return careNeeds.findAllById(a.getCareNeedIds()).stream()
                .map(n -> new CareNeedView(n.getId(), n.getName()))
                .collect(Collectors.toSet());
    }
}
