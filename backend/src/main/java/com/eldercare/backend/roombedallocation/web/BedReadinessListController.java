package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.roombedallocation.internal.bed.service.BedReadinessService;
import com.eldercare.backend.roombedallocation.web.response.BedReadinessListItem;
import com.eldercare.backend.roombedallocation.web.response.StructureProblem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** T060: danh sách giường Chờ sẵn sàng cho Trưởng tầng, Người phụ trách ca (research G-14; duyệt 2026-11-06). */
@RestController
@RequestMapping("/api/v1/bed-readiness")
public class BedReadinessListController {
    private final BedReadinessService service;

    public BedReadinessListController(BedReadinessService service) {
        this.service = service;
    }

    @GetMapping("/beds")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                        @Content(
                                mediaType = "application/json",
                                array = @ArraySchema(schema = @Schema(implementation = BedReadinessListItem.class)))),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public List<BedReadinessListItem> beds(@RequestParam(required = false) Long floorId) {
        return service.awaitingReadyBeds(floorId).stream()
                .map(BedReadinessListItem::of)
                .toList();
    }
}
