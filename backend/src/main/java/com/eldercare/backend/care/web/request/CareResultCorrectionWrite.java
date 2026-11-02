package com.eldercare.backend.care.web.request;

import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.common.Reason;

/** Bản dịch T051 được chủ dự án duyệt ngày02/11/2026. */
public record CareResultCorrectionWrite(CorrectionType type, Object correctedContent, Reason reason) {}
