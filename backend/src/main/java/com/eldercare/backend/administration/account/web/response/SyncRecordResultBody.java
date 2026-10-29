package com.eldercare.backend.administration.account.web.response;

import com.eldercare.backend.common.offline.SyncResult;
import java.util.UUID;

public record SyncRecordResultBody(UUID idempotencyKey, SyncResult result, String reason, String reference) {}
