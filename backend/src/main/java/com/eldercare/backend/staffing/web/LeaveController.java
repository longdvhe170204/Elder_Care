package com.eldercare.backend.staffing.web;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveInput;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveView;
import com.eldercare.backend.staffing.internal.swap.service.LeaveService;
import com.eldercare.backend.staffing.web.request.LeaveRequests;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Yêu cầu nghỉ đột xuất (contracts/api-015.yaml, nhóm {@code leave-requests}); chỉ trực tuyến (FR-043). */
@RestController
@RequestMapping("/api/v1/leave-requests")
class LeaveController {

    private final LeaveService leaves;

    LeaveController(LeaveService leaves) {
        this.leaves = leaves;
    }

    @GetMapping
    List<LeaveView> list() {
        return leaves.list();
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> create(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody LeaveRequests.Create body) {
        List<Long> ids = leaves.create(new LeaveInput(
                body.kind(),
                body.staffId(),
                body.shiftIds(),
                body.fromDate(),
                body.toDate(),
                body.dates(),
                body.scopeType(),
                body.scopeId(),
                body.reason()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("requestIds", ids));
    }

    @GetMapping("/{id}/pre-approval")
    List<Map<String, Object>> preApproval(@PathVariable long id) {
        return leaves.preApproval(id);
    }

    @PostMapping("/{id}/{command}")
    ResponseEntity<?> command(
            @PathVariable long id,
            @PathVariable String command,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody(required = false) LeaveRequests.Decision body) {
        LeaveRequests.Decision b = body == null ? new LeaveRequests.Decision(null, null) : body;
        switch (command) {
            case "approve" -> {
                List<LeaveService.ItemDecision> items = b.items() == null
                        ? null
                        : b.items().stream()
                                .map(i -> new LeaveService.ItemDecision(
                                        i.shiftId(),
                                        i.date(),
                                        i.result() == LeaveRequests.Result.APPROVED,
                                        i.newLeadId()))
                                .toList();
                return ResponseEntity.ok(Map.of("effects", leaves.approve(id, new LeaveService.Decision(items, b.reason()))));
            }
            case "reject" -> leaves.reject(id, reason(b));
            case "cancel" -> leaves.cancel(id, reason(b));
            default -> throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lệnh không tồn tại");
        }
        return ResponseEntity.ok().build();
    }

    private static Reason reason(LeaveRequests.Decision body) {
        if (body.reason() == null || body.reason().isBlank()) {
            throw new ReasonRequiredException();
        }
        return new Reason(body.reason());
    }
}
