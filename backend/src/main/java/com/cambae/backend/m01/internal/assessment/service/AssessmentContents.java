package com.cambae.backend.m01.internal.assessment.service;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.internal.assessment.domain.Assessment;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CurrentValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Nội dung hiện hành của lần đánh giá Đã xác nhận: bản gốc, trừ ba trường đính chính được (nhận xét, lưu ý chăm sóc, mức)
 * lấy theo bản đính chính mới nhất của 000 (FR-032, FR-038). Nhu cầu chăm sóc không đính chính.
 */
@Component
class AssessmentContents {

    /** Tên trường đính chính được, trùng khóa của nội dung đúng gửi tới 000. */
    static final String COMMENT = "comment";

    static final String CARE_NOTE = "careNote";
    static final String CARE_LEVEL = "careLevel";

    record Effective(String comment, String careNote, CareLevel careLevel, boolean corrected) {}

    /** Qua {@code ObjectProvider}: 000 tiêm mọi provider đính chính, nên tiêm thẳng sẽ thành vòng phụ thuộc. */
    private final ObjectProvider<CorrectionApi> corrections;

    AssessmentContents(ObjectProvider<CorrectionApi> corrections) {
        this.corrections = corrections;
    }

    Map<Long, Effective> of(List<Assessment> list) {
        List<Long> confirmedIds = list.stream()
                .filter(Assessment::isConfirmed)
                .map(Assessment::getId)
                .toList();
        Map<Long, CurrentValue> current = confirmedIds.isEmpty()
                ? Map.of()
                : corrections.getObject().current(AssessmentService.RECORD_TYPE, confirmedIds);
        Map<Long, Effective> result = new HashMap<>();
        for (Assessment a : list) {
            result.put(a.getId(), effective(a, current.get(a.getId())));
        }
        return result;
    }

    Effective of(Assessment a) {
        return of(List.of(a)).get(a.getId());
    }

    private static Effective effective(Assessment a, CurrentValue current) {
        String comment = a.getComment();
        String careNote = a.getCareNote();
        CareLevel level = a.getCareLevel();
        boolean corrected = false;
        if (current != null && current.corrected()) {
            corrected = true;
            if (current.content() instanceof Map<?, ?> map) {
                if (map.containsKey(COMMENT)) {
                    comment = blankToNull(map.get(COMMENT));
                }
                if (map.containsKey(CARE_NOTE)) {
                    careNote = blankToNull(map.get(CARE_NOTE));
                }
                if (map.get(CARE_LEVEL) != null) {
                    level = CareLevel.valueOf(map.get(CARE_LEVEL).toString());
                }
            }
        }
        return new Effective(comment, careNote, level, corrected);
    }

    private static String blankToNull(Object value) {
        return value == null || value.toString().isBlank()
                ? null
                : value.toString().strip();
    }
}
