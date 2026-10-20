package com.cambae.backend.m15.account.web.response;

import java.util.List;

public record SyncResponse(Boolean sessionEnded, List<Long> residentsInScope, List<SyncRecordResultBody> results) {}
