package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuContent;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuStatus;
import com.eldercare.backend.nutrition.internal.menu.service.MenuQueries;
import com.eldercare.backend.nutrition.internal.menu.service.MenuService;
import com.eldercare.backend.nutrition.internal.menu.service.MenuService.MenuDetail;
import com.eldercare.backend.nutrition.internal.menu.service.MenuService.SwapInput;
import com.eldercare.backend.nutrition.web.request.CreateMenuRequest;
import com.eldercare.backend.nutrition.web.request.DishSwapRequest;
import com.eldercare.backend.nutrition.web.request.MenuCommandRequest;
import com.eldercare.backend.nutrition.web.request.MenuContentRequest;
import com.eldercare.backend.nutrition.web.response.MealPlanningInfoResponse;
import com.eldercare.backend.nutrition.web.response.MenuResponse;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import com.eldercare.backend.nutrition.web.response.SharedMenuDayResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Thực đơn tuần chung của api-011 (thẻ {@code menu}; FR-023 → FR-031, Q-445). Chi tiết không khai schema trong hợp đồng
 * ({@code 200 OK}); thân trả JSON như mô tả của thao tác.
 */
@RestController
@RequestMapping("/api/v1/menus")
@Tag(name = "menu")
public class MenuController {

    private static final String PROBLEM = "application/problem+json";

    private final MenuService menus;
    private final MenuQueries queries;
    private final AccountApi accounts;

    public MenuController(MenuService menus, MenuQueries queries, AccountApi accounts) {
        this.menus = menus;
        this.queries = queries;
        this.accounts = accounts;
    }

    @GetMapping("/shared")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<SharedMenuDayResponse> shared(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return SharedMenuDayResponse.of(queries.shared(from, to));
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public List<MenuResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
            @RequestParam(required = false) MenuStatus status) {
        return menus.list(MenuService.target(0), week, status).stream()
                .map(m -> MenuResponse.of(m, submitterName(m)))
                .toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody CreateMenuRequest body) {
        long id = menus.create(MenuService.target(0), body.weekStart(), body.copyFromId());
        return ResponseEntity.created(URI.create("/api/v1/menus/" + id)).build();
    }

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public ResponseEntity<Map<String, Object>> detail(@PathVariable long id) {
        MenuDetail detail = menus.detail(MenuService.target(id), id);
        Menu menu = detail.menu();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", menu.id());
        body.put("weekStart", menu.weekStart());
        body.put("status", menu.status());
        body.put("version", menu.version());
        body.put("content", menu.content());
        body.put("copiedFromId", menu.copiedFromId());
        body.put("dishesToReplace", detail.dishesToReplace());
        body.put("submittedBy", submitterName(menu));
        body.put("submittedAt", menu.submittedAt());
        body.put("publishedBy", menu.publishedBy() == null ? null : name(menu.publishedBy()));
        body.put("publishedAt", menu.publishedAt());
        body.put("appliedAt", menu.appliedAt());
        body.put("returnReason", menu.returnReason());
        body.put("cancelReason", menu.cancelReason());
        body.put(
                "dishSwaps",
                detail.dishSwaps().stream()
                        .map(swap -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("id", swap.id());
                            row.put("date", swap.content().date());
                            row.put("mealId", swap.content().mealId());
                            row.put("oldDishId", swap.content().oldDishId());
                            row.put("newDishId", swap.content().newDishId());
                            row.put("reason", swap.content().reason());
                            row.put("swappedBy", swap.actorId());
                            row.put("swappedAt", swap.occurredAt());
                            return row;
                        })
                        .toList());
        return ResponseEntity.ok().eTag(VersionEtag.of(menu.version())).body(body);
    }

    @PutMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    @ApiResponse(
            responseCode = "412",
            description = "Phiên bản đã đổi",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> update(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody MenuContentRequest body) {
        List<MenuDish> dishes = body.dishes() == null
                ? List.of()
                : body.dishes().stream().map(MenuController::dish).toList();
        Menu menu = menus.updateContent(MenuService.target(id), id, ifMatch, new MenuContent(dishes));
        return ResponseEntity.ok().eTag(VersionEtag.of(menu.version())).build();
    }

    @GetMapping("/{id}/pre-submit-info")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<MealPlanningInfoResponse> preSubmitInfo(@PathVariable long id) {
        return menus.preSubmitInfo(MenuService.target(id), id).stream()
                .map(MealPlanningInfoResponse::of)
                .toList();
    }

    @PostMapping("/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "403",
            description = "Không có quyền",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> command(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"submit", "withdraw", "cancel", "return", "publish"})
                    String command,
            @RequestBody(required = false) MenuCommandRequest body) {
        var target = MenuService.target(id);
        var reason = body == null ? null : body.reason();
        switch (command) {
            case "submit" -> menus.submit(target, id);
            case "withdraw" -> menus.withdraw(target, id);
            case "cancel" -> menus.cancel(target, id, reason);
            case "return" -> menus.returnToKitchen(target, id, reason);
            case "publish" -> menus.publish(target, id);
            default -> throw new InvalidValueException("Lệnh không có trong bảng trạng thái thực đơn: " + command);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dish-swaps")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> swap(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestBody DishSwapRequest body) {
        menus.swap(
                MenuService.target(id),
                id,
                new SwapInput(body.date(), body.mealId(), body.oldDishId(), body.newDishId()),
                body.reason());
        return ResponseEntity.ok().build();
    }

    private String submitterName(Menu menu) {
        return menu.submittedBy() == null ? null : name(menu.submittedBy());
    }

    private String name(long accountId) {
        return accounts.fullName(accountId).orElse(null);
    }

    private static MenuDish dish(MenuContentRequest.Row row) {
        if (row.date() == null || row.mealId() == null || row.dishId() == null) {
            throw new InvalidValueException("Mỗi dòng thực đơn cần ngày, bữa, món");
        }
        return new MenuDish(row.date(), row.mealId(), row.dishId());
    }
}
