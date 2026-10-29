package com.testsupport;

import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.error.VersionMismatchException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controller chỉ dùng trong test, nạp bằng @Import (gói ngoài vùng quét của ứng dụng). */
@RestController
public class SampleController {

    public final AtomicInteger created = new AtomicInteger();

    @GetMapping("/api/v1/test-support/errors/{kind}")
    public void fail(@PathVariable String kind) {
        throw switch (kind) {
            case "condition" ->
                new ConditionNotMetException(List.of(new UnmetCondition("BR-M03-06", "Giường chưa Trống")));
            case "waivable" ->
                new ConditionNotMetException(List.of(new UnmetCondition(
                        "KT_H_QUYET_TOAN", "Chưa có biên bản quyết toán", "BR-M11-22", "017", true)));
            case "candidates" ->
                new BusinessException(
                        HttpStatus.CONFLICT,
                        "POSSIBLE_DUPLICATE_PROFILE",
                        "Có thể trùng hồ sơ",
                        Map.of("candidates", List.of(Map.of("id", 7))));
            case "permission" -> new PermissionDeniedException("Không có quyền");
            case "legal" ->
                new PermissionDeniedException(
                        "LEGAL_CONDITION_NOT_MET",
                        "Thiếu điều kiện pháp lý",
                        Map.of(
                                "failedLayer",
                                "LEGAL",
                                "license",
                                Map.of("name", "Chứng chỉ hành nghề", "expiryDate", "2026-09-30")));
            case "processed" -> new AlreadyProcessedException("Yêu cầu đã được xử lý");
            case "version" -> new VersionMismatchException(Map.of("currentValue", 20));
            case "reason" -> new ReasonRequiredException();
            case "invalid" -> new InvalidValueException("Giá trị ngoài khoảng");
            default -> new NotFoundException();
        };
    }

    @PostMapping("/api/v1/test-support/samples")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return Map.of("number", created.incrementAndGet(), "name", body.getOrDefault("name", ""));
    }
}
