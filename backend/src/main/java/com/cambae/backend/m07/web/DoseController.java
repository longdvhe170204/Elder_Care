package com.cambae.backend.m07.web;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.api.dto.AdministerDoseRequest;
import com.cambae.backend.m07.api.dto.DoseDetailView;
import com.cambae.backend.m07.api.dto.RefuseDoseRequest;
import com.cambae.backend.m07.api.dto.SkipDoseRequest;
import com.cambae.backend.m07.internal.domain.DoseReview;
import com.cambae.backend.m07.internal.service.DoseAdministrationService;
import com.cambae.backend.m07.internal.service.DoseReviewService;
import com.cambae.backend.m07.web.request.ReviewDoseRequestBody;
import com.cambae.backend.m15.account.api.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for dose confirmation and review (FR-020 -> FR-033, FR-029; research D-08, D-11).
 */
@RestController
public class DoseController {

    private final DoseAdministrationService doseAdministrationService;
    private final DoseReviewService doseReviewService;
    private final ObjectProvider<CurrentUser> currentUserProvider;
    private final BusinessTime businessTime;

    public DoseController(
            DoseAdministrationService doseAdministrationService,
            DoseReviewService doseReviewService,
            ObjectProvider<CurrentUser> currentUserProvider,
            BusinessTime businessTime) {
        this.doseAdministrationService = doseAdministrationService;
        this.doseReviewService = doseReviewService;
        this.currentUserProvider = currentUserProvider;
        this.businessTime = businessTime;
    }

    /**
     * Confirm administered medication dose (FR-020, FR-026).
     */
    @PostMapping({"/api/v1/lieu-thuoc/{doseKey}/da-dung", "/api/v1/medication-doses/{doseKey}/administer"})
    public ResponseEntity<DoseDetailView> administer(
            @PathVariable("doseKey") String doseKey, @RequestBody(required = false) AdministerDoseRequest body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long recorderId = currentUser != null ? currentUser.accountId() : 1L;
        AdministerDoseRequest request = body != null
                ? body
                : new AdministerDoseRequest(
                        businessTime.now(), null, null, null, null, null, null, null, false, null, null);

        DoseDetailView view =
                doseAdministrationService.administerDose(doseKey, request, recorderId, null, businessTime.now());
        return ResponseEntity.ok(view);
    }

    /**
     * Confirm refused medication dose (FR-021, FR-032).
     */
    @PostMapping({"/api/v1/lieu-thuoc/{doseKey}/tu-choi", "/api/v1/medication-doses/{doseKey}/refuse"})
    public ResponseEntity<DoseDetailView> refuse(
            @PathVariable("doseKey") String doseKey, @Valid @RequestBody RefuseDoseRequest body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long recorderId = currentUser != null ? currentUser.accountId() : 1L;

        DoseDetailView view = doseAdministrationService.refuseDose(doseKey, body, recorderId, null, businessTime.now());
        return ResponseEntity.ok(view);
    }

    /**
     * Confirm omitted/skipped medication dose (FR-021).
     */
    @PostMapping({"/api/v1/lieu-thuoc/{doseKey}/khong-thuc-hien", "/api/v1/medication-doses/{doseKey}/skip"})
    public ResponseEntity<DoseDetailView> skip(
            @PathVariable("doseKey") String doseKey, @Valid @RequestBody SkipDoseRequest body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long recorderId = currentUser != null ? currentUser.accountId() : 1L;

        DoseDetailView view = doseAdministrationService.skipDose(doseKey, body, recorderId, null, businessTime.now());
        return ResponseEntity.ok(view);
    }

    /**
     * Retrieve list of doses pending review (FR-029).
     */
    @GetMapping({"/api/v1/xem-lai-lieu", "/api/v1/dose-reviews"})
    public List<DoseReview> listPendingReviews(
            @RequestParam(name = "tangKhuVucId", required = false) Long floorAreaId) {
        return doseReviewService.listPendingReviews(floorAreaId);
    }

    /**
     * Process dose review decision (Accept or Request Correction; FR-029).
     */
    @PostMapping({"/api/v1/xem-lai-lieu/{id}/ket-qua", "/api/v1/dose-reviews/{id}/result"})
    public ResponseEntity<DoseReview> processReview(
            @PathVariable("id") Long reviewId, @Valid @RequestBody ReviewDoseRequestBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long reviewerId = currentUser != null ? currentUser.accountId() : 2L;

        DoseReview updated =
                doseReviewService.reviewDose(reviewId, body.accept(), body.note(), reviewerId, businessTime.now());
        return ResponseEntity.ok(updated);
    }
}
