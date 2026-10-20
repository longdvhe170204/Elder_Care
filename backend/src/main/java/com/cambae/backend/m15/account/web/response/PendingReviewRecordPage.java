package com.cambae.backend.m15.account.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PendingReviewRecordPage")
public record PendingReviewRecordPage(List<PendingReviewRecordBody> items, String nextCursor) {}
