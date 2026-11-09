package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.service.DietService;
import com.eldercare.backend.nutrition.internal.catalog.service.DietService.DietInput;
import com.eldercare.backend.nutrition.internal.catalog.service.DishService;
import com.eldercare.backend.nutrition.internal.catalog.service.DishService.DishInput;
import com.eldercare.backend.nutrition.web.request.DietRequest;
import com.eldercare.backend.nutrition.web.request.DishCommandBody;
import com.eldercare.backend.nutrition.web.request.DishRequest;
import com.eldercare.backend.nutrition.web.request.FoodIngredientRequest;
import com.eldercare.backend.nutrition.web.request.NutritionReason;
import com.eldercare.backend.nutrition.web.response.DietResponse;
import com.eldercare.backend.nutrition.web.response.DishResponse;
import com.eldercare.backend.nutrition.web.response.MenuCheckResultResponse;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh mục dinh dưỡng của api-011 (thẻ {@code catalog}; FR-001 → FR-004a): chế độ ăn, thành phần thực phẩm, món ăn.
 * Bữa ăn, loại đồ ăn bị cấm chờ chức năng quyền của Quản lý viện (team-plan mục 8); đối chiếu món của một người làm ở phase
 * thực đơn.
 */
@RestController
@RequestMapping("/api/v1/nutrition")
@Tag(name = "catalog")
public class NutritionCatalogController {

    private final DietService diets;
    private final DishService dishes;

    public NutritionCatalogController(DietService diets, DishService dishes) {
        this.diets = diets;
        this.dishes = dishes;
    }

    @GetMapping("/diets")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<DietResponse> diets() {
        return diets.list().stream().map(DietResponse::of).toList();
    }

    @PostMapping("/diets")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Lỗi",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> createDiet(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody DietRequest body) {
        Diet diet = diets.create(input(body));
        return ResponseEntity.created(URI.create("/api/v1/nutrition/diets/" + diet.id()))
                .eTag(VersionEtag.of(diet.version()))
                .build();
    }

    @PutMapping("/diets/{id}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> updateDiet(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody DietRequest body) {
        Diet diet = diets.update(id, ifMatch, input(body));
        return ResponseEntity.ok().eTag(VersionEtag.of(diet.version())).build();
    }

    @PostMapping("/diets/{id}/deactivate")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> deactivateDiet(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestBody NutritionReason body) {
        diets.deactivate(id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/food-ingredients")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> addFoodIngredient(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody FoodIngredientRequest body) {
        diets.addFoodIngredient(body.name());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/dishes")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<DishResponse> dishes(
            @RequestParam(name = "status", required = false) DishStatus status,
            @RequestParam(name = "dietId", required = false) Long dietId) {
        return dishes.list(status, dietId).stream().map(DishResponse::of).toList();
    }

    @PostMapping("/dishes")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "422",
            description = "Lỗi",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> createDish(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody DishRequest body) {
        Dish dish = dishes.create(input(body));
        return ResponseEntity.created(URI.create("/api/v1/nutrition/dishes/" + dish.id()))
                .eTag(VersionEtag.of(dish.version()))
                .build();
    }

    @PutMapping("/dishes/{id}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> updateDish(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody DishRequest body) {
        Dish dish = dishes.update(id, ifMatch, input(body));
        return ResponseEntity.ok().eTag(VersionEtag.of(dish.version())).build();
    }

    @PostMapping("/dishes/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            description = "Không có quyền",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public MenuCheckResultResponse dishCommand(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @Parameter(
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {
                                                "submit",
                                                "withdraw",
                                                "cancel",
                                                "confirm",
                                                "return",
                                                "submit-revision",
                                                "confirm-revision",
                                                "return-revision",
                                                "withdraw-revision",
                                                "deactivate"
                                            }))
                    @PathVariable
                    String command,
            @RequestBody(required = false) DishCommandBody body) {
        dishes.command(
                id,
                command,
                body == null ? null : body.reason(),
                body == null || body.content() == null ? null : input(body.content()));
        return MenuCheckResultResponse.empty();
    }

    private static final String PROBLEM = "application/problem+json";

    private static DietInput input(DietRequest body) {
        return new DietInput(
                body.name(),
                body.description(),
                Boolean.TRUE.equals(body.treatmentRelated()),
                Boolean.TRUE.equals(body.isDefault()),
                body.restrictions(),
                body.safeDishId());
    }

    private static DishInput input(DishRequest body) {
        return new DishInput(
                body.name(),
                body.note(),
                body.ingredients(),
                Boolean.TRUE.equals(body.noCatalogIngredient()),
                body.suitableDietIds(),
                body.texture());
    }
}
