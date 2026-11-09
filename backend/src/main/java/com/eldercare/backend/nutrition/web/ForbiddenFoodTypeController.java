package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.nutrition.internal.familyfood.service.ForbiddenFoodTypeService;
import com.eldercare.backend.nutrition.web.request.ForbiddenFoodTypeRequest;
import com.eldercare.backend.nutrition.web.response.ForbiddenFoodTypeResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục loại đồ ăn bị cấm của api-011 (thẻ {@code catalog}; FR-007), dùng đối chiếu đồ ăn gia đình (FR-053). */
@RestController
@RequestMapping("/api/v1/nutrition/forbidden-food-types")
@Tag(name = "catalog")
public class ForbiddenFoodTypeController {

    private final ForbiddenFoodTypeService types;

    ForbiddenFoodTypeController(ForbiddenFoodTypeService types) {
        this.types = types;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public List<ForbiddenFoodTypeResponse> list() {
        return types.list().stream().map(ForbiddenFoodTypeResponse::of).toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    public ResponseEntity<Void> upsert(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ForbiddenFoodTypeRequest body) {
        types.upsert(body.name(), body.description(), !"INACTIVE".equals(body.status()));
        return ResponseEntity.status(201).build();
    }
}
