package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.resident.api.dto.OpinionSource;

/** Nội dung ý kiến giữ nguyên; thay danh sách chỉ đổi cờ kỹ thuật active và thêm dòng mới. */
public record RiskOpinion(OpinionSource source, String content, boolean active) {}
