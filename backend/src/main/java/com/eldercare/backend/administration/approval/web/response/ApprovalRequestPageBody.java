package com.eldercare.backend.administration.approval.web.response;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestBody;
import java.util.List;

public record ApprovalRequestPageBody(List<ApprovalRequestBody> items, String nextCursor) {}
