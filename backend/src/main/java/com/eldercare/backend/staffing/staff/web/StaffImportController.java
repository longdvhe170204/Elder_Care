package com.eldercare.backend.staffing.staff.web;

import com.eldercare.backend.staffing.staff.internal.domain.StaffImportRejected;
import com.eldercare.backend.staffing.staff.internal.service.StaffImportService;
import com.eldercare.backend.staffing.staff.web.response.StaffImportResponses;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** T048: không lưu file gốc; 409 trả bảng xem trước theo hợp đồng. */
@RestController
@RequestMapping("/api/v1/staff/import")
class StaffImportController {
    private final StaffImportService service;

    StaffImportController(StaffImportService service) {
        this.service = service;
    }

    @GetMapping(value = "/template", produces = StaffImportService.MIME)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(
                                mediaType = StaffImportService.MIME,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=staff-template.xlsx")
                .contentType(MediaType.parseMediaType(StaffImportService.MIME))
                .body(service.template());
    }

    @PostMapping(
            value = "/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = StaffImportResponses.Preview.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    StaffImportResponses.Preview preview(@RequestPart("file") MultipartFile file) throws IOException {
        return StaffImportResponses.Preview.of(
                service.preview(file.getOriginalFilename(), file.getContentType(), file.getBytes()));
    }

    @PostMapping(
            value = "/confirm",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = StaffImportResponses.Result.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = StaffImportResponses.Preview.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<?> confirm(@RequestHeader("Idempotency-Key") UUID key, @RequestPart("file") MultipartFile file)
            throws IOException {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(StaffImportResponses.Result.of(
                            service.confirm(file.getOriginalFilename(), file.getContentType(), file.getBytes())));
        } catch (StaffImportRejected rejected) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(StaffImportResponses.Preview.of(rejected.preview()));
        }
    }
}
