package com.eldercare.backend.billing.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;

/** Schema lỗi 010; lỗi thật vẫn do bộ xử lý RFC 9457 chung trả. */
@Schema(name = "CostProblem")
public record CostProblemBody(
        String type,
        String title,
        Integer status,
        String detail,
        String errorCode,
        List<Map<String, Object>> unmetConditions) {}
