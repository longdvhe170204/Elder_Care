package com.eldercare.backend.care.web.request;

import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.common.Reason;

public record AttendanceCorrectionWrite(CorrectionType type, Object correctedContent, Reason reason) {}
