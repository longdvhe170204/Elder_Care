package com.eldercare.backend.belongings.web;

import com.eldercare.backend.belongings.internal.inventorycheck.service.InventoryCheckService;
import com.eldercare.backend.belongings.web.request.InventoryCheckRequests;
import com.eldercare.backend.common.error.BusinessException;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Phiếu kiểm kê đồ có giá trị (013 T026; api-013 tag {@code belonging-inventory-checks}). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "belonging-inventory-checks")
class InventoryCheckController {

    private final InventoryCheckService service;

    InventoryCheckController(InventoryCheckService service) {
        this.service = service;
    }

    @GetMapping("/belonging-inventory-checks")
    InventoryCheckService.Overview overview(@RequestParam(name = "periodCode", required = false) String periodCode) {
        return service.overview(periodCode);
    }

    @PostMapping("/belonging-inventory-checks")
    @ResponseStatus(HttpStatus.CREATED)
    InventoryCheckService.CheckView create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody InventoryCheckRequests.Create body) {
        if (body == null || body.storageLocationId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Thiếu vị trí lưu giữ");
        }
        return service.create(body.storageLocationId());
    }

    @GetMapping("/belonging-inventory-checks/{id}")
    InventoryCheckService.CheckView get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/belonging-inventory-checks/{id}/{command}")
    InventoryCheckService.CheckView command(
            @PathVariable long id,
            @PathVariable String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody(required = false) InventoryCheckRequests.Result body) {
        InventoryCheckRequests.Result request =
                body == null ? new InventoryCheckRequests.Result(null, null, null, null) : body;
        return switch (command) {
            case "complete" -> service.complete(id, request.toCompletion());
            case "cancel" -> service.cancel(id, request.reason());
            default -> throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lệnh không tồn tại");
        };
    }
}
