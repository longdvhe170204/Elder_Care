package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.care.web.request.AttendanceCorrectionWrite;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/day-care-attendance-records")
public class DayCareAttendanceCorrectionController {
    private final CorrectionApi corrections;

    public DayCareAttendanceCorrectionController(CorrectionApi corrections) {
        this.corrections = corrections;
    }

    @PostMapping("/{id}/correct")
    @ApiResponse(responseCode = "201", description = "Đã lưu hoặc gửi yêu cầu phê duyệt")
    public ResponseEntity<Void> correct(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody AttendanceCorrectionWrite body) {
        if (body.type() == null || body.reason() == null)
            throw new InvalidValueException("Cần kiểu đính chính và lý do");
        if (body.type() == CorrectionType.VOID)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CORRECTION_NOT_SUPPORTED",
                    "Hủy ghi nhận điểm danh đang chờ thống nhất nghiệp vụ");
        corrections.create("M04.ATTENDANCE_RECORD", id, body.type(), body.correctedContent(), body.reason());
        return ResponseEntity.status(201).build();
    }
}
