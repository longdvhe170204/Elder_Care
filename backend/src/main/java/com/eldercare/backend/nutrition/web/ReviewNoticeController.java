package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.nutrition.internal.review.service.ReviewNoticeService;
import com.eldercare.backend.nutrition.web.response.ReviewNoticeResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thông báo xem lại chế độ ăn của api-011 (thẻ {@code review-and-sample}; FR-058, FR-059, BR-M08-05). */
@RestController
@RequestMapping("/api/v1/nutrition/review-notices")
@Tag(name = "review-and-sample")
public class ReviewNoticeController {

    private final ReviewNoticeService review;

    ReviewNoticeController(ReviewNoticeService review) {
        this.review = review;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public List<ReviewNoticeResponse> list(@RequestParam(required = false) Long residentId) {
        return review.list(ReviewNoticeService.target(residentId), residentId).stream()
                .map(ReviewNoticeResponse::of)
                .toList();
    }
}
