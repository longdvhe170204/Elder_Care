package com.eldercare.backend.resident.internal.wish.domain;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import java.time.Instant;

/** Một ý kiến khác đã ghi cùng phiên bản phiếu, có nguồn, người ghi, thời điểm (FR-023g); không sửa, không xóa. */
public record RecordedOpinion(OpinionSource source, String content, long recordedBy, Instant at) {}
