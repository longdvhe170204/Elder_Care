package com.eldercare.backend.administration.parameter.web;

import com.eldercare.backend.administration.parameter.internal.service.ParameterService;
import com.eldercare.backend.administration.parameter.internal.service.ParameterService.HistoryEntry;
import com.eldercare.backend.administration.parameter.internal.service.ParameterService.ParameterView;
import com.eldercare.backend.administration.parameter.web.request.ChangeBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/parameters} theo contracts/api-000.yaml (000 T080); không có API tạo hay xóa mã (FR-011). */
@RestController
@RequestMapping("/api/v1/parameters")
public class ParameterController {

    private final ParameterService parameters;

    public ParameterController(ParameterService parameters) {
        this.parameters = parameters;
    }

    @GetMapping
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public List<ParameterView> list(@RequestParam(required = false) String module) {
        return parameters.list(module);
    }

    @GetMapping("/{code}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<ParameterView> detail(@PathVariable String code) {
        ParameterView view = parameters.detail(code);
        return ResponseEntity.ok().eTag(VersionEtag.of(view.version())).body(view);
    }

    @PostMapping("/{code}/change-value")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã lưu"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<ParameterView> changeValue(
            @PathVariable String code, @RequestHeader("If-Match") String ifMatch, @RequestBody ChangeBody body) {
        ParameterView view = parameters.changeValue(
                code, ifMatch, body.value(), Boolean.TRUE.equals(body.restoreDefault()), body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(view.version())).body(view);
    }

    @GetMapping("/{code}/history")
    public List<HistoryEntry> history(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime asOf) {
        return parameters.history(code, asOf == null ? null : asOf.toInstant());
    }
}
