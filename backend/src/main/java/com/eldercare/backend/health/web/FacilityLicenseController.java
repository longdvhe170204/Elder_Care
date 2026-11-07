package com.eldercare.backend.health.web;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.health.api.dto.FacilityContactView;
import com.eldercare.backend.health.internal.license.service.FacilityLicenseService;
import com.eldercare.backend.health.internal.license.service.FacilityLicenseService.LicenseInput;
import com.eldercare.backend.health.internal.license.service.FacilityLicenseService.LicenseView;
import com.eldercare.backend.health.web.request.FacilityContactRequest;
import com.eldercare.backend.health.web.request.FacilityLicenseRequest;
import com.eldercare.backend.health.web.response.M06ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Giấy phép cơ sở và số liên hệ của viện (FR-070, FR-071, FR-075; contracts/api-007 mục J). */
@RestController
@RequestMapping("/api/v1")
public class FacilityLicenseController {
    private static final String PROBLEM = "application/problem+json";

    private final FacilityLicenseService licenses;
    private final FacilityContactApi contact;
    private final BusinessTime time;

    public FacilityLicenseController(FacilityLicenseService licenses, FacilityContactApi contact, BusinessTime time) {
        this.licenses = licenses;
        this.contact = contact;
        this.time = time;
    }

    @GetMapping("/facility-licenses")
    public List<LicenseView> list() {
        return licenses.list(time.today());
    }

    @PostMapping("/facility-licenses")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    public ResponseEntity<Void> save(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody FacilityLicenseRequest request) {
        licenses.save(
                new LicenseInput(
                        request.number(),
                        request.issuer(),
                        request.issuedOn(),
                        request.expiresOn(),
                        Boolean.TRUE.equals(request.coversMedicalPractice()),
                        request.scopes(),
                        request.scanFileId(),
                        request.replaces(),
                        Boolean.TRUE.equals(request.replacesDueToError()),
                        request.reason()),
                time.today());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/facility-contact")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public FacilityContactView contact() {
        return contact.current();
    }

    @PutMapping("/facility-contact")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    public FacilityContactView changeContact(
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody FacilityContactRequest request) {
        return licenses.changeContact(request.phoneNumber(), request.note(), version(ifMatch));
    }

    private static int version(String ifMatch) {
        try {
            return Integer.parseInt(ifMatch.replace("\"", "").strip());
        } catch (NumberFormatException e) {
            throw new InvalidValueException("If-Match phải là số phiên bản");
        }
    }
}
