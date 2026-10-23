package com.cambae.backend.m01.internal.assessment.service;

import com.cambae.backend.m01.api.ReassessmentApi;
import com.cambae.backend.m01.api.dto.ReassessmentStatus;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;

/** {@link ReassessmentApi} cho 016 FR-044 (001 T063): tính từ các lần báo, không có bản ghi yêu cầu (Q-307). */
@Service
class ReassessmentApiImpl implements ReassessmentApi {

    private final ReassessmentNoticeService notices;

    ReassessmentApiImpl(ReassessmentNoticeService notices) {
        this.notices = notices;
    }

    @Override
    public Map<Long, ReassessmentStatus> status(Collection<Long> residentIds) {
        return notices.status(residentIds);
    }
}
