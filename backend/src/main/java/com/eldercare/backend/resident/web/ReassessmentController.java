package com.eldercare.backend.resident.web;

import com.eldercare.backend.resident.internal.assessment.service.ReassessmentQueryService;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.web.response.ReassessmentBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trạng thái cần hoặc quá hạn đánh giá lại (001 T066; contracts/api-001.yaml, tag {@code assessments}). Chỉ đọc: báo
 * đánh giá lại do Bộ lập lịch và sự kiện tạo (Q-307), không có endpoint tạo yêu cầu.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "assessments")
class ReassessmentController {

    private final ReassessmentQueryService reassessments;

    ReassessmentController(ReassessmentQueryService reassessments) {
        this.reassessments = reassessments;
    }

    @GetMapping("/residents/{id}/reassessment")
    ReassessmentBody get(@PathVariable long id) {
        return ReassessmentBody.of(reassessments.get(ResidentProfileService.target(id)));
    }

    @GetMapping("/reassessments/due")
    List<ReassessmentBody> due() {
        return reassessments.due().stream().map(ReassessmentBody::of).toList();
    }
}
