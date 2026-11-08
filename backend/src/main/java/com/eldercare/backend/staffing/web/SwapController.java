package com.eldercare.backend.staffing.web;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.staffing.internal.swap.domain.SwapChecks;
import com.eldercare.backend.staffing.internal.swap.domain.SwapView;
import com.eldercare.backend.staffing.internal.swap.service.SwapService;
import com.eldercare.backend.staffing.web.request.SwapRequests;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC-54: yêu cầu đổi ca (contracts/api-015.yaml, nhóm {@code shift-swaps}); chỉ trực tuyến (FR-043). */
@RestController
@RequestMapping("/api/v1/shift-swaps")
class SwapController {

    private final SwapService swaps;

    SwapController(SwapService swaps) {
        this.swaps = swaps;
    }

    @GetMapping
    List<SwapView> list() {
        return swaps.list();
    }

    @PostMapping
    ResponseEntity<SwapChecks> create(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody SwapRequests.Create body) {
        SwapService.Created created =
                swaps.create(body.kind(), body.shiftXId(), body.recipientId(), body.shiftYId(), body.reason());
        return ResponseEntity.created(URI.create("/api/v1/shift-swaps/" + created.id()))
                .body(created.checks());
    }

    @GetMapping("/recipient-shifts")
    List<Map<String, Object>> recipientShifts(@RequestParam long shiftXId, @RequestParam long recipientId) {
        return swaps.recipientShifts(shiftXId, recipientId);
    }

    @GetMapping("/{id}/checks")
    SwapChecks checks(@PathVariable long id) {
        return swaps.checks(id);
    }

    @PostMapping("/{id}/{command}")
    ResponseEntity<?> command(
            @PathVariable long id,
            @PathVariable String command,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody(required = false) SwapRequests.Command body) {
        SwapRequests.Command b = body == null ? new SwapRequests.Command(null, null, null, null) : body;
        switch (command) {
            case "accept" -> {
                return ResponseEntity.ok(swaps.accept(id));
            }
            case "decline" -> swaps.decline(id, required(b));
            case "withdraw-acceptance" -> swaps.withdraw(id, b.reason());
            case "cancel" -> swaps.cancel(id, required(b));
            case "reject" -> swaps.reject(id, required(b));
            case "approve" -> {
                List<Map<String, Object>> notTransferred = swaps.approve(
                        id,
                        new SwapService.Decision(
                                b.reason() == null ? null : b.reason().value(),
                                b.newLeadXId(),
                                b.newLeadYId(),
                                Boolean.TRUE.equals(b.confirmWarning())));
                return ResponseEntity.ok(Map.of("notTransferredAssignments", notTransferred));
            }
            default -> throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lệnh không tồn tại");
        }
        return ResponseEntity.ok().build();
    }

    private static com.eldercare.backend.common.Reason required(SwapRequests.Command body) {
        if (body.reason() == null) {
            throw new com.eldercare.backend.common.error.ReasonRequiredException();
        }
        return body.reason();
    }
}
