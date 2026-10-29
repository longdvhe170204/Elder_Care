package com.eldercare.backend.administration.account.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(name = "EarlyUnlockCandidatePage")
public record EarlyUnlockCandidatePage(List<Item> items) {

    @Schema(name = "EarlyUnlockCandidate")
    public record Item(Long accountId, String fullName, Instant lockedUntil, Long version) {}
}
