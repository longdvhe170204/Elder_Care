package com.cambae.backend.m02.api;

import com.cambae.backend.m02.api.dto.ConditionListView;
import com.cambae.backend.m02.api.dto.ProgressStepView;
import java.util.List;

/** Điều kiện tiếp nhận, kết thúc và tiến trình cho 012, 016 (tên logic {@code DieuKienTiepNhanApi}; contracts mục 1.5). */
public interface AdmissionConditionApi {

    /** FR-004 (a) → (f). */
    ConditionListView admissionConditions(long residentId);

    /** FR-003. */
    List<ProgressStepView> progress(long applicationId);

    /** FR-063, kèm ngoại lệ. */
    ConditionListView endingConditions(long residencyEndingId);
}
