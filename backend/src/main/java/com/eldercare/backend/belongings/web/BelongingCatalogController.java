package com.eldercare.backend.belongings.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogViews.BelongingTypeView;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogViews.ConditionLevelView;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogViews.StorageLocationView;
import com.eldercare.backend.belongings.internal.catalog.service.BelongingCatalogService;
import com.eldercare.backend.belongings.web.request.BelongingTypeRequest;
import com.eldercare.backend.belongings.web.request.ConditionLevelRequest;
import com.eldercare.backend.belongings.web.request.StorageLocationRequest;
import com.eldercare.backend.belongings.web.response.IdResponse;
import com.eldercare.backend.common.Role;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh mục đồ gửi (013 T005; api-013 tag {@code belonging-catalogs}). Hợp đồng không khai thân phản hồi của lệnh;
 * lệnh trả {@code id} như các controller 012. Mọi kiểm tra ở {@link BelongingCatalogService}.
 */
@RestController
@RequestMapping("/api/v1/belongings")
@Tag(name = "belonging-catalogs")
class BelongingCatalogController {

    private final BelongingCatalogService service;
    private final ObjectProvider<CurrentUser> users;

    BelongingCatalogController(BelongingCatalogService service, ObjectProvider<CurrentUser> users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping("/types")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<BelongingTypeView> types() {
        return service.types();
    }

    @PostMapping("/types")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ResponseStatus(HttpStatus.CREATED)
    IdResponse createType(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody BelongingTypeRequest body) {
        return new IdResponse(service.createType(body.name(), body.hasCashAmount(), body.requiresConsentForUse()));
    }

    @PutMapping("/types/{id}")
    void updateType(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody BelongingTypeRequest body) {
        service.updateType(
                id, body.name(), body.hasCashAmount(), body.requiresConsentForUse(), body.status(), body.reason());
    }

    @GetMapping("/condition-levels")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<ConditionLevelView> conditionLevels() {
        return service.conditionLevels();
    }

    @PostMapping("/condition-levels")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ResponseStatus(HttpStatus.CREATED)
    IdResponse saveConditionLevel(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody ConditionLevelRequest body) {
        return new IdResponse(
                service.saveConditionLevel(body.id(), body.name(), body.order(), body.unusable(), body.status()));
    }

    @GetMapping("/storage-locations")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<StorageLocationView> storageLocations() {
        return service.storageLocations();
    }

    /** Hành chính, Quản lý viện; mỗi vai trò đi qua khóa quyền của mình (plan "Kế hoạch triển khai R-15" điểm 3). */
    @PostMapping("/storage-locations")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    IdResponse saveStorageLocation(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody StorageLocationRequest body) {
        boolean manager = users.getObject().roles().contains(Role.NURSING_HOME_MANAGER);
        long id = manager
                ? service.saveStorageLocationAsManager(
                        body.id(), body.name(), body.description(), body.floorAreaId(), body.status())
                : service.saveStorageLocationAsOfficeStaff(
                        body.id(), body.name(), body.description(), body.floorAreaId(), body.status());
        return new IdResponse(id);
    }
}
