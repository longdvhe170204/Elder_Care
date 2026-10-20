package com.cambae.backend.m15.account.web.response;

import com.cambae.backend.common.offline.SyncResult;
import java.util.UUID;

public record SyncRecordResultBody(UUID idempotencyKey, SyncResult result, String reason, String reference) {}
