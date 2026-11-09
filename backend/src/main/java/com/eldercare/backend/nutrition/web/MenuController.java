package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuContent;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.service.MenuService;
import com.eldercare.backend.nutrition.internal.menu.service.MenuService.CheckView;
import com.eldercare.backend.nutrition.internal.menu.service.MenuService.MenuDetail;
import com.eldercare.backend.nutrition.internal.menu.service.MenuService.SwapInput;
import com.eldercare.backend.nutrition.web.request.CreateMenuRequest;
import com.eldercare.backend.nutrition.web.request.DishSwapRequest;
import com.eldercare.backend.nutrition.web.request.MenuCommandRequest;
import com.eldercare.backend.nutrition.web.request.MenuContentRequest;
import com.eldercare.backend.nutrition.web.response.MenuCheckResultResponse;
import com.eldercare.backend.nutrition.web.response.MenuResponse;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
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
 * Thực đơn tuần của api-011 (thẻ {@code menu}; FR-023 → FR-032, BR-M08-06 → 08). Chi tiết và thông tin trước khi gửi
 * không khai schema trong hợp đồng ({@code 200 OK}); thân trả JSON như mô tả của thao tác.
 */
@RestController
@RequestMapping("/api/v1/menus")
@Tag(name = "menu")
public class MenuController {

    private static final String PROBLEM = "application/problem+json";

    private final MenuService menus;

    public MenuController(MenuService menus) {
        this.menus = menus;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public List<MenuResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return menus.list(MenuService.target(0), week).stream()
                .map(MenuResponse::of)
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
        body.put("processingCheckConfirmed", menu.processingCheckConfirmed());
        body.put("processingNote", menu.processingNote());
        body.put("processingCheckedBy", menu.processingCheckedBy());
        body.put("processingCheckedAt", menu.processingCheckedAt());
        body.put("confirmedWarnings", menu.confirmedWarnings());
        body.put("submittedBy", menu.submittedBy());
        body.put("submittedAt", menu.submittedAt());
        body.put("publishedBy", menu.publishedBy());
        body.put("publishedAt", menu.publishedAt());
        body.put("appliedAt", menu.appliedAt());
        body.put("returnReason", menu.returnReason());
        body.put("cancelReason", menu.cancelReason());
        body.put(
                "checks",
                detail.checks().stream().map(MenuCheckResultResponse::of).toList());
        body.put(
                "dishSwaps",
                detail.dishSwaps().stream()
                        .map(swap -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("id", swap.id());
                            row.put("date", swap.content().date());
                            row.put("mealId", swap.content().mealId());
                            row.put("dietId", swap.content().dietId());
                            row.put("oldDishId", swap.content().oldDishId());
                            row.put("newDishId", swap.content().newDishId());
                            row.put("oldSubstituteDishIds", swap.content().oldSubstituteDishIds());
                            row.put("substituteDishIds", swap.content().substituteDishIds());
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
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public Map<String, Object> preSubmitInfo(@PathVariable long id) {
        return Map.of("diets", menus.preSubmitInfo(MenuService.target(id), id));
    }

    @PostMapping("/{id}/check")
    @ApiResponse(responseCode = "200", description = "Bếp nhận KitchenMenuWarning; Bác sĩ nhận kèm tên người")
    public MenuCheckResultResponse check(@PathVariable long id) {
        return MenuCheckResultResponse.of(menus.check(MenuService.target(id), id));
    }

    @PostMapping("/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            description = "Không có quyền",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public MenuCheckResultResponse command(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"submit", "withdraw", "cancel", "return", "publish"})
                    String command,
            @RequestBody(required = false) MenuCommandRequest body) {
        MenuCommandRequest b = body == null ? new MenuCommandRequest(null, null, null, null) : body;
        var target = MenuService.target(id);
        CheckView result = switch (command) {
            case "submit" ->
                menus.submit(
                        target,
                        id,
                        Boolean.TRUE.equals(b.processingCheckConfirmed()),
                        b.processingNote(),
                        b.confirmedWarnings());
            case "withdraw" -> {
                menus.withdraw(target, id);
                yield null;
            }
            case "cancel" -> {
                menus.cancel(target, id, b.reason());
                yield null;
            }
            case "return" -> {
                menus.returnToKitchen(target, id, b.reason());
                yield null;
            }
            case "publish" -> menus.publish(target, id, b.confirmedWarnings());
            default -> throw new InvalidValueException("Lệnh không có trong bảng trạng thái thực đơn: " + command);
        };
        return result == null ? MenuCheckResultResponse.empty() : MenuCheckResultResponse.of(result);
    }

    @PostMapping("/{id}/dish-swaps")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public MenuCheckResultResponse swap(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestBody DishSwapRequest body) {
        return MenuCheckResultResponse.of(menus.swap(
                MenuService.target(id),
                id,
                new SwapInput(
                        body.date(),
                        body.mealId(),
                        body.dietId(),
                        body.oldDishId(),
                        body.newDishId(),
                        body.substituteDishIds(),
                        body.confirmedWarnings()),
                body.reason()));
    }

    private static MenuDish dish(MenuContentRequest.Row row) {
        if (row.date() == null || row.mealId() == null || row.dietId() == null || row.dishId() == null) {
            throw new InvalidValueException("Mỗi dòng thực đơn cần ngày, bữa, chế độ ăn, món");
        }
        return new MenuDish(row.date(), row.mealId(), row.dietId(), row.dishId(), row.substituteDishIds());
    }
}
