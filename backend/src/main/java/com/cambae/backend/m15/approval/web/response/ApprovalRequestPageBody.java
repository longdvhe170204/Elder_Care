package com.cambae.backend.m15.approval.web.response;

import com.cambae.backend.m15.approval.api.dto.ApprovalRequestBody;
import java.util.List;

public record ApprovalRequestPageBody(List<ApprovalRequestBody> items, String nextCursor) {}
